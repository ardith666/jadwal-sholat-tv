package com.ardith.sholattv;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class AppFileProvider extends ContentProvider {
    private static final String[] COLUMNS = {
            OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
    };

    private static final HashMap<String, File> sRoots = new HashMap<>();
    private String mAuthority;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        if (info.exported) {
            throw new SecurityException("Provider must not be exported");
        }
        mAuthority = info.authority;

        if (sRoots.isEmpty()) {
            parsePathRules(context);
        }
    }

    private void parsePathRules(Context context) {
        try (XmlResourceParser parser = context.getResources().getXml(R.xml.file_paths)) {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    String path = parser.getAttributeValue(null, "path");
                    if (path == null) path = "";

                    File target = null;
                    if ("cache-path".equals(tag)) {
                        target = buildPath(context.getCacheDir(), path);
                    } else if ("files-path".equals(tag)) {
                        target = buildPath(context.getFilesDir(), path);
                    } else if ("external-cache-path".equals(tag)) {
                        target = buildPath(context.getExternalCacheDir(), path);
                    } else if ("external-files-path".equals(tag)) {
                        target = buildPath(context.getExternalFilesDir(null), path);
                    } else if ("external-path".equals(tag)) {
                        target = buildPath(Environment.getExternalStorageDirectory(), path);
                    }

                    if (target != null) {
                        try {
                            sRoots.put(tag, target.getCanonicalFile());
                        } catch (IOException e) {
                            sRoots.put(tag, target);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static File buildPath(File base, String... segments) {
        File cur = base;
        if (cur == null) return null;
        for (String segment : segments) {
            if (segment != null && !segment.isEmpty()) {
                cur = new File(cur, segment);
            }
        }
        return cur;
    }

    public static Uri getUriForFile(Context context, String authority, File file) {
        try {
            String path = file.getCanonicalPath();
            return new Uri.Builder()
                    .scheme("content")
                    .authority(authority)
                    .encodedPath(Uri.encode(file.getName()))
                    .build();
        } catch (IOException e) {
            return Uri.parse("content://" + authority + "/" + file.getName());
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = getFileForUri(uri);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private File getFileForUri(Uri uri) throws FileNotFoundException {
        String fileName = uri.getLastPathSegment();
        if (fileName == null || fileName.isEmpty()) {
            throw new FileNotFoundException("Invalid URI: " + uri);
        }

        Context c = getContext();
        if (c != null) {
            // Check in cacheDir
            File inCache = new File(c.getCacheDir(), fileName);
            if (inCache.exists()) return inCache;

            File inExtCache = new File(c.getExternalCacheDir(), fileName);
            if (inExtCache.exists()) return inExtCache;

            File inFiles = new File(c.getFilesDir(), fileName);
            if (inFiles.exists()) return inFiles;
        }

        throw new FileNotFoundException("File not found for URI: " + uri);
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null && name.endsWith(".apk")) {
            return "application/vnd.android.package-archive";
        }
        return "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = getFileForUri(uri);
            if (projection == null) projection = COLUMNS;

            String[] cols = new String[projection.length];
            Object[] values = new Object[projection.length];
            int i = 0;
            for (String col : projection) {
                if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                    cols[i] = OpenableColumns.DISPLAY_NAME;
                    values[i++] = file.getName();
                } else if (OpenableColumns.SIZE.equals(col)) {
                    cols[i] = OpenableColumns.SIZE;
                    values[i++] = file.length();
                }
            }

            MatrixCursor cursor = new MatrixCursor(cols, 1);
            cursor.addRow(values);
            return cursor;
        } catch (Exception e) {
            return null;
        }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
