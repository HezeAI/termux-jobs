package com.termux.filepicker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Patterns;

import com.termux.R;
import com.termux.shared.data.DataUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.interact.MessageDialogUtils;
import com.termux.shared.interact.TextInputDialogUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.app.TermuxService;
import com.termux.shared.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class TermuxFileReceiverActivity extends Activity {

    static final String TERMUX_RECEIVEDIR = TermuxConstants.TERMUX_FILES_DIR_PATH + "/home/downloads";
    static final String EDITOR_PROGRAM = TermuxConstants.TERMUX_HOME_DIR_PATH + "/bin/termux-file-editor";
    static final String URL_OPENER_PROGRAM = TermuxConstants.TERMUX_HOME_DIR_PATH + "/bin/termux-url-opener";

    /**
     * If the activity should be finished when the name input dialog is dismissed. This is disabled
     * before showing an error dialog, since the act of showing the error dialog will cause the
     * name input dialog to be implicitly dismissed, and we do not want to finish the activity directly
     * when showing the error dialog.
     */
    boolean mFinishOnDismissNameDialog = true;
    private InputStream mReceivedStream;

    @Override protected void onDestroy() {
        closeReceivedStream();
        super.onDestroy();
    }

    private void closeReceivedStream() {
        if (mReceivedStream != null) {
            try { mReceivedStream.close(); } catch (IOException ignored) {}
            mReceivedStream = null;
        }
    }

    private static final String API_TAG = TermuxConstants.TERMUX_APP_NAME + "FileReceiver";

    private static final String LOG_TAG = "TermuxFileReceiverActivity";

    static boolean isSharedTextAnUrl(String sharedText) {
        if (sharedText == null || sharedText.isEmpty()) return false;

        return Patterns.WEB_URL.matcher(sharedText).matches()
            || Pattern.matches("magnet:\\?xt=urn:btih:.*?", sharedText);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();
        final String scheme = intent.getScheme();

        Logger.logVerbose(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));

        final String sharedTitle = IntentUtils.getStringExtraIfSet(intent, Intent.EXTRA_TITLE, null);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            final Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

            if (sharedUri != null) {
                handleContentUri(sharedUri, sharedTitle);
            } else if (sharedText != null) {
                if (isSharedTextAnUrl(sharedText)) {
                    handleUrlAndFinish(sharedText);
                } else {
                    String subject = IntentUtils.getStringExtraIfSet(intent, Intent.EXTRA_SUBJECT, null);
                    if (subject == null) subject = sharedTitle;
                    if (subject != null) subject += ".txt";
                    promptNameAndSave(new ByteArrayInputStream(sharedText.getBytes(StandardCharsets.UTF_8)), subject);
                }
            } else {
                showErrorDialogAndQuit("Send action without content - nothing to save.");
            }
        } else {
            Uri dataUri = intent.getData();

            if (dataUri == null) {
                showErrorDialogAndQuit("Data uri not passed.");
                return;
            }

            if ("content".equals(scheme)) {
                handleContentUri(dataUri, sharedTitle);
            } else if ("file".equals(scheme)) {
                // When e.g. clicking on a downloaded apk:
                String path = dataUri.getPath();
                if (DataUtils.isNullOrEmpty(path)) {
                    showErrorDialogAndQuit("File path from data uri is null, empty or invalid.");
                    return;
                }

                File file = new File(path);
                try {
                    FileInputStream in = new FileInputStream(file);
                    promptNameAndSave(in, file.getName());
                } catch (FileNotFoundException e) {
                    showErrorDialogAndQuit("Cannot open file: " + e.getMessage() + ".");
                }
            } else {
                showErrorDialogAndQuit("Unable to receive any file or URL.");
            }
        }
    }

    void showErrorDialogAndQuit(String message) {
        mFinishOnDismissNameDialog = false;
        MessageDialogUtils.showMessage(this,
            API_TAG, message,
            null, (dialog, which) -> finish(),
            null, null,
            dialog -> finish());
    }

    void handleContentUri(final Uri uri, String subjectFromIntent) {
        try {
            String attachmentFileName = null;

            String[] projection = new String[]{OpenableColumns.DISPLAY_NAME};
            try (Cursor c = getContentResolver().query(uri, projection, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    final int fileNameColumnId = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (fileNameColumnId >= 0) attachmentFileName = c.getString(fileNameColumnId);
                }
            }

            if (attachmentFileName == null) attachmentFileName = subjectFromIntent;

            InputStream in = getContentResolver().openInputStream(uri);
            promptNameAndSave(in, attachmentFileName);
        } catch (Exception e) {
            showErrorDialogAndQuit("Unable to handle shared content:\n\n" + e.getMessage());
            Logger.logStackTraceWithMessage(LOG_TAG, "handleContentUri(uri=" + uri + ") failed", e);
        }
    }

    void promptNameAndSave(final InputStream in, final String attachmentFileName) {
        closeReceivedStream();
        if (in == null) { showErrorDialogAndQuit("Cannot open shared content."); return; }
        mReceivedStream = in;
        TextInputDialogUtils.textInput(this, R.string.title_file_received, attachmentFileName, R.string.action_file_received_edit, text -> {
            File outFile = saveStreamWithName(in, text);
            if (outFile == null) return;

            final File editorProgramFile = new File(EDITOR_PROGRAM);
            if (!editorProgramFile.isFile()) {
                showErrorDialogAndQuit("The following file does not exist:\n$HOME/bin/termux-file-editor\n\n"
                    + "Create this file as a script or a symlink - it will be called with the received file as only argument.");
                return;
            }

            // Do this for the user if necessary:
            //noinspection ResultOfMethodCallIgnored
            editorProgramFile.setExecutable(true);

            final Uri scriptUri = new Uri.Builder().scheme("file").path(EDITOR_PROGRAM).build();

            Intent executeIntent = new Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE, scriptUri);
            executeIntent.setClass(TermuxFileReceiverActivity.this, TermuxService.class);
            executeIntent.putExtra(TERMUX_SERVICE.EXTRA_ARGUMENTS, new String[]{outFile.getAbsolutePath()});
            startService(executeIntent);
            finish();
        },
            R.string.action_file_received_open_directory, text -> {
                if (saveStreamWithName(in, text) == null) return;

                Intent executeIntent = new Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE);
                executeIntent.putExtra(TERMUX_SERVICE.EXTRA_WORKDIR, TERMUX_RECEIVEDIR);
                executeIntent.setClass(TermuxFileReceiverActivity.this, TermuxService.class);
                startService(executeIntent);
                finish();
            },
            android.R.string.cancel, text -> finish(), dialog -> {
                if (mFinishOnDismissNameDialog) finish();
            });
    }

    File receiveDirectory() { return new File(TERMUX_RECEIVEDIR); }

    public File saveStreamWithName(InputStream in, String attachmentFileName) {
        File outFile = null;
        boolean created = false;
        try (InputStream source = in) {
            // The provider's display name and the editable title are untrusted basenames.
            if (source == null || DataUtils.isNullOrEmpty(attachmentFileName)
                || attachmentFileName.equals(".") || attachmentFileName.equals("..")
                || attachmentFileName.indexOf('/') >= 0 || attachmentFileName.indexOf('\\') >= 0
                || attachmentFileName.indexOf('\0') >= 0) {
                throw new IOException("Choose a file name without a path.");
            }
            File receiveDir = receiveDirectory();
            if (!receiveDir.isDirectory() && !receiveDir.mkdirs())
                throw new IOException("Cannot create receive directory.");
            outFile = new File(receiveDir, attachmentFileName);
            // Exclusive creation is atomic: neither existing files nor links are truncated.
            java.io.FileDescriptor descriptor = Os.open(outFile.getPath(),
                OsConstants.O_WRONLY | OsConstants.O_CREAT | OsConstants.O_EXCL | OsConstants.O_NOFOLLOW, 0600);
            created = true;
            try (FileOutputStream output = new FileOutputStream(descriptor)) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
            } finally {
                // This activity owns the descriptor returned by Os.open. On Android,
                // a stream constructed from a caller-owned descriptor may leave it open.
                if (descriptor.valid()) Os.close(descriptor);
            }
            return outFile;
        } catch (IOException | ErrnoException | RuntimeException e) {
            if (created && outFile != null) outFile.delete();
            showErrorDialogAndQuit("Cannot save shared file. Use a new file name; existing files are never overwritten.");
            return null;
        } finally {
            if (mReceivedStream == in) mReceivedStream = null;
        }
    }

    void handleUrlAndFinish(final String url) {
        final File urlOpenerProgramFile = new File(URL_OPENER_PROGRAM);
        if (!urlOpenerProgramFile.isFile()) {
            showErrorDialogAndQuit("The following file does not exist:\n$HOME/bin/termux-url-opener\n\n"
                + "Create this file as a script or a symlink - it will be called with the shared URL as the first argument.");
            return;
        }

        // Do this for the user if necessary:
        //noinspection ResultOfMethodCallIgnored
        urlOpenerProgramFile.setExecutable(true);

        final Uri urlOpenerProgramUri = new Uri.Builder().scheme("file").path(URL_OPENER_PROGRAM).build();

        Intent executeIntent = new Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE, urlOpenerProgramUri);
        executeIntent.setClass(TermuxFileReceiverActivity.this, TermuxService.class);
        executeIntent.putExtra(TERMUX_SERVICE.EXTRA_ARGUMENTS, new String[]{url});
        startService(executeIntent);
        finish();
    }

}
