package com.termux.filepicker;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;

import com.termux.R;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.LinkedList;

/**
 * A document provider for the Storage Access Framework which exposes the files in the
 * $HOME/ directory to other apps.
 * <p/>
 * Note that this replaces providing an activity matching the ACTION_GET_CONTENT intent:
 * <p/>
 * "A document provider and ACTION_GET_CONTENT should be considered mutually exclusive. If you
 * support both of them simultaneously, your app will appear twice in the system picker UI,
 * offering two different ways of accessing your stored data. This would be confusing for users."
 * - http://developer.android.com/guide/topics/providers/document-provider.html#43
 */
public class TermuxDocumentsProvider extends DocumentsProvider {

    private static final String ALL_MIME_TYPES = "*/*";

    protected File getBaseDirectory() { return TermuxConstants.TERMUX_HOME_DIR; }

    private File[] roots() { return new File[]{getBaseDirectory()}; }


    // The default columns to return information about a root if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_MIME_TYPES,
        Root.COLUMN_FLAGS,
        Root.COLUMN_ICON,
        Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_AVAILABLE_BYTES
    };

    // The default columns to return information about a document if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS,
        Document.COLUMN_SIZE
    };

    @Override
    public Cursor queryRoots(String[] projection) {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_ROOT_PROJECTION);
        final String applicationName = getContext().getString(R.string.application_name);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(getBaseDirectory()));
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(getBaseDirectory()));
        row.add(Root.COLUMN_SUMMARY, null);
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_SEARCH | Root.FLAG_SUPPORTS_IS_CHILD);
        row.add(Root.COLUMN_TITLE, applicationName);
        row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES);
        row.add(Root.COLUMN_AVAILABLE_BYTES, getBaseDirectory().getFreeSpace());
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        includeFile(result, documentId);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        final File parent = getFileForDocId(parentDocumentId);
        File[] children = parent.listFiles();
        if (children == null) throw new FileNotFoundException("Cannot list document directory");
        for (File file : children) {
            try { includeFile(result, new File(parentDocumentId, file.getName()).getAbsolutePath()); }
            catch (FileNotFoundException ignored) { /* Disappeared or points outside HOME. */ }
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(final String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        return ProviderFiles.open(documentId, mode, signal, roots(), new File[0]);
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        final ParcelFileDescriptor pfd = openDocument(documentId, "r", signal);
        return new AssetFileDescriptor(pfd, 0, pfd.getStatSize());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) throws FileNotFoundException {
        if (displayName == null || displayName.isEmpty() || displayName.equals(".") || displayName.equals("..") ||
            displayName.indexOf('/') >= 0 || displayName.indexOf('\\') >= 0 || displayName.indexOf('\0') >= 0)
            throw new FileNotFoundException("Invalid document name");
        File parent = getFileForDocId(parentDocumentId);
        try (ParcelFileDescriptor owner = ProviderFiles.openDirectory(parent, roots())) {
            File anchored = new File("/proc/self/fd/" + owner.getFd());
            for (int number = 1; number <= 10000; number++) {
                String name = number == 1 ? displayName : displayName + " (" + number + ")";
                File candidate = new File(anchored, name);
                try {
                    if (Document.MIME_TYPE_DIR.equals(mimeType)) Files.createDirectory(candidate.toPath());
                    else Files.createFile(candidate.toPath());
                    return new File(parentDocumentId, name).getAbsolutePath();
                } catch (FileAlreadyExistsException ignored) { /* Also covers dangling symlinks. */ }
            }
        } catch (IOException e) {
            FileNotFoundException failure = new FileNotFoundException("Cannot create document");
            failure.initCause(e);
            throw failure;
        }
        throw new FileNotFoundException("Too many document name conflicts");
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        try {
            if (file.equals(getBaseDirectory().getCanonicalFile()))
                throw new FileNotFoundException("Cannot delete provider root");
            // Keep the original basename: deleting an internal symlink must unlink it,
            // never remove its target. The checked parent descriptor anchors this mutation.
            File original = new File(documentId).getAbsoluteFile();
            try (ParcelFileDescriptor parent = ProviderFiles.openDirectory(original.getParentFile(), roots())) {
                Files.delete(new File("/proc/self/fd/" + parent.getFd(), original.getName()).toPath());
            }
        } catch (IOException e) {
            FileNotFoundException failure = new FileNotFoundException("Cannot delete document");
            failure.initCause(e);
            throw failure;
        }
    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        return getMimeType(file, documentId);
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);
        getFileForDocId(rootId);

        final LinkedList<File> pending = new LinkedList<>();
        final Set<String> visited = new HashSet<>();
        final String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        pending.add(new File(rootId));
        // Bound both work and queue size even for a tree with no matching names.
        final int maxScanned = 10000;
        int scanned = 0;
        while (!pending.isEmpty() && result.getCount() < 50 && scanned++ < maxScanned) {
            File candidate = pending.removeFirst();
            final File file;
            try { file = getFileForDocId(candidate.getPath()); }
            catch (FileNotFoundException ignored) { continue; }
            if (file.isDirectory()) {
                if (!visited.add(file.getPath())) continue;
                File[] children = file.listFiles();
                if (children != null) for (File child : children) {
                    if (pending.size() + scanned >= maxScanned) break;
                    pending.add(new File(candidate, child.getName()));
                }
            } else if (candidate.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                try { includeFile(result, candidate.getAbsolutePath()); }
                catch (FileNotFoundException ignored) { /* Changed while searching. */ }
            }
        }

        return result;
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        try {
            File parent = getFileForDocId(parentDocumentId);
            File child = getFileForDocId(documentId);
            return parent.isDirectory() && !parent.equals(child) && ProviderFiles.contains(parent, child);
        } catch (FileNotFoundException e) { return false; }
    }

    /**
     * Get the document id given a file. This document id must be consistent across time as other
     * applications may save the ID and use it to reference documents later.
     * <p/>
     * The reverse of @{link #getFileForDocId}.
     */
    private static String getDocIdForFile(File file) {
        return file.getAbsolutePath();
    }

    /**
     * Get the file given a document id (the reverse of {@link #getDocIdForFile(File)}).
     */
    private File getFileForDocId(String docId) throws FileNotFoundException {
        File file = ProviderFiles.resolve(docId, roots());
        if (!file.exists()) throw new FileNotFoundException("Document not found");
        if (!file.isDirectory() && !file.isFile()) throw new FileNotFoundException("Unsupported document type");
        return file;
    }

    private static String getMimeType(File file, String docId) {
        if (file.isDirectory()) return Document.MIME_TYPE_DIR;
        String mime = ProviderFiles.mimeType(new File(docId));
        return mime == null ? "application/octet-stream" : mime;
    }

    /**
     * Add a representation of a file to a cursor.
     *
     * @param result the cursor to modify
     * @param docId the stable absolute-path document ID
     */
    private void includeFile(MatrixCursor result, String docId) throws FileNotFoundException {
        File file = getFileForDocId(docId);

        int flags = 0;
        if (file.isDirectory()) {
            if (file.canWrite()) flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
        } else if (file.canWrite()) {
            flags |= Document.FLAG_SUPPORTS_WRITE;
        }
        if (!file.equals(getFileForDocId(getBaseDirectory().getPath())) &&
            file.getParentFile() != null && file.getParentFile().canWrite()) flags |= Document.FLAG_SUPPORTS_DELETE;

        final String displayName = new File(docId).getName();
        final String mimeType = getMimeType(file, docId);
        if (mimeType.startsWith("image/")) flags |= Document.FLAG_SUPPORTS_THUMBNAIL;

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SIZE, file.length());
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(Document.COLUMN_FLAGS, flags);
        row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher);
    }

}
