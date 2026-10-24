package com.termux.app;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import com.termux.filepicker.ProviderFiles;

import com.termux.app.utils.PluginUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import androidx.annotation.NonNull;

public class TermuxOpenReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "TermuxOpenReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final Uri data = intent.getData();
        if (data == null) {
            Logger.logError(LOG_TAG, "termux-open: Called without intent data");
            return;
        }

        Logger.logVerbose(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));

        final String filePath = data.getPath();
        final String contentTypeExtra = intent.getStringExtra("content-type");
        final boolean useChooser = intent.getBooleanExtra("chooser", false);
        String intentAction = intent.getAction() == null ? Intent.ACTION_VIEW : intent.getAction();
        switch (intentAction) {
            case Intent.ACTION_SEND:
            case Intent.ACTION_VIEW:
                // Ok.
                break;
            default:
                Logger.logError(LOG_TAG, "Invalid action '" + intentAction + "', using 'view'");
                intentAction = Intent.ACTION_VIEW;
                break;
        }

        final boolean isExternalUrl = data.getScheme() != null && !data.getScheme().equals("file");
        if (isExternalUrl) {
            Intent urlIntent = new Intent(intentAction, data);
            if (intentAction.equals(Intent.ACTION_SEND)) {
                urlIntent.putExtra(Intent.EXTRA_TEXT, data.toString());
                urlIntent.setData(null);
            } else if (contentTypeExtra != null) {
                urlIntent.setDataAndType(data, contentTypeExtra);
            }
            urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(urlIntent);
            } catch (ActivityNotFoundException e) {
                Logger.logError(LOG_TAG, "termux-open: No app handles the url " + data);
            }
            return;
        }

        if (filePath == null) return;
        final File fileToShare = new File(filePath);
        if (!(fileToShare.isFile() && fileToShare.canRead())) {
            Logger.logError(LOG_TAG, "termux-open: Not a readable file: '" + fileToShare.getAbsolutePath() + "'");
            return;
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(intentAction);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String contentTypeToUse;
        if (contentTypeExtra == null) {
            contentTypeToUse = ProviderFiles.mimeType(fileToShare);
            if (contentTypeToUse == null) contentTypeToUse = "application/octet-stream";
        } else {
            contentTypeToUse = contentTypeExtra;
        }

        Uri uriToShare = new Uri.Builder().scheme("content").authority(TermuxConstants.TERMUX_FILE_SHARE_URI_AUTHORITY)
            .path(fileToShare.getAbsolutePath()).build();

        if (Intent.ACTION_SEND.equals(intentAction)) {
            sendIntent.putExtra(Intent.EXTRA_STREAM, uriToShare);
            sendIntent.setType(contentTypeToUse);
        } else {
            sendIntent.setDataAndType(uriToShare, contentTypeToUse);
        }

        if (useChooser) {
            sendIntent = Intent.createChooser(sendIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(sendIntent);
        } catch (ActivityNotFoundException e) {
            Logger.logError(LOG_TAG, "termux-open: No app handles the url " + data);
        }
    }

    public static class ContentProvider extends android.content.ContentProvider {

        private static final String LOG_TAG = "TermuxContentProvider";

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            File file = fileForUri(uri);

            if (projection == null) {
                projection = new String[]{
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns._ID
                };
            }

            Object[] row = new Object[projection.length];
            for (int i = 0; i < projection.length; i++) {
                String column = projection[i];
                Object value;
                switch (column) {
                    case MediaStore.MediaColumns.DISPLAY_NAME:
                        value = new File(uri.getPath()).getName();
                        break;
                    case MediaStore.MediaColumns.SIZE:
                        value = file.length();
                        break;
                    case MediaStore.MediaColumns._ID:
                        value = 1;
                        break;
                    default:
                        value = null;
                }
                row[i] = value;
            }

            MatrixCursor cursor = new MatrixCursor(projection);
            cursor.addRow(row);
            return cursor;
        }

        @Override
        public String getType(@NonNull Uri uri) {
            fileForUri(uri);
            return ProviderFiles.mimeType(new File(uri.getPath()));
        }

        @Override
        public Uri insert(@NonNull Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
            File file = fileForUri(uri);
            String errmsg = externalAppsPolicyError();
            if (errmsg != null) throw new IllegalArgumentException(errmsg);
            return ProviderFiles.open(file.getPath(), mode, null, allowedRoots(), readOnlyFiles());
        }

        File[] allowedRoots() {
            return new File[]{TermuxConstants.TERMUX_FILES_DIR, Environment.getExternalStorageDirectory()};
        }

        File[] readOnlyFiles() {
            return new File[]{TermuxConstants.TERMUX_PROPERTIES_PRIMARY_FILE,
                TermuxConstants.TERMUX_PROPERTIES_SECONDARY_FILE,
                TermuxConstants.TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE,
                TermuxConstants.TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE};
        }

        String externalAppsPolicyError() {
            return PluginUtils.checkIfAllowExternalAppsPolicyIsViolated(getContext(), LOG_TAG);
        }

        private File fileForUri(Uri uri) {
            try {
                if (!"content".equals(uri.getScheme()) ||
                    !TermuxConstants.TERMUX_FILE_SHARE_URI_AUTHORITY.equals(uri.getAuthority()) ||
                    uri.getQuery() != null || uri.getFragment() != null)
                    throw new FileNotFoundException("Invalid provider URI");
                File file = ProviderFiles.resolve(uri.getPath(), allowedRoots());
                if (!file.isFile()) throw new FileNotFoundException("Not a regular file");
                return file;
            } catch (IOException e) {
                throw new IllegalArgumentException("Invalid provider file", e);
            }
        }
    }

}
