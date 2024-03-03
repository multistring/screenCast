package multi.string.screencast.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import multi.string.screencast.models.Album;
import multi.string.screencast.models.Photo;

public class PhotoUtils {
    public static Album getAlbum(Context context) {
        Album album = new Album();

        final String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

        Uri contentUri;
        String selection = null;
        String[] selectionAllArgs = null;

        contentUri = MediaStore.Files.getContentUri("external");
        selection =
                "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?" + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)";
        selectionAllArgs =
                new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                        String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)};

        ContentResolver contentResolver = context.getContentResolver();


        List<String> projectionList = new ArrayList<String>();
        projectionList.add(MediaStore.Files.FileColumns._ID);
        projectionList.add(MediaStore.MediaColumns.DATA);
        projectionList.add(MediaStore.MediaColumns.DISPLAY_NAME);
        projectionList.add(MediaStore.MediaColumns.DATE_MODIFIED);
        projectionList.add(MediaStore.MediaColumns.MIME_TYPE);
        projectionList.add(MediaStore.MediaColumns.SIZE);
        projectionList.add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME);


        String[] projections = projectionList.toArray(new String[0]);

        Cursor cursor = contentResolver.query(contentUri, projections, selection,
                selectionAllArgs, sortOrder);
        if (cursor == null) {
        } else if (cursor.moveToFirst()) {
            String albumItem_all_name = "所有图片";

            int albumNameCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME);

            do {
                long id = cursor.getLong(0);
                String path = cursor.getString(1);
                String name = cursor.getString(2);
                long dateTime = cursor.getLong(3);
                String type = cursor.getString(4);
                long size = cursor.getLong(5);
                long duration = 0;


                if (TextUtils.isEmpty(path) || TextUtils.isEmpty(type) || (!path.endsWith(".jpg") && !path.endsWith(".JPG"))) {
                    continue;
                }

                int width = 0;
                int height = 0;
                int orientation = 0;

                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri("external"), id);

//某些机型，特定情况下三方应用或用户操作删除媒体文件时，没有通知媒体库，导致媒体库表中还有其数据，但真实文件已经不存在
                File file = new File(path);
                if (!file.isFile() || file.length() <10000) {
                    continue;
                }

                Photo imageItem = new Photo(name, uri, path, dateTime, width, height, orientation
                        , size,
                        duration, type);

                // 添加当前图片的专辑到专辑模型实体中
                String albumName;
                String folderPath;
                if (albumNameCol > 0) {
                    albumName = cursor.getString(albumNameCol);
                    folderPath = albumName;
                } else {
                    File parentFile = new File(path).getParentFile();
                    if (null == parentFile) {
                        continue;
                    }
                    folderPath = parentFile.getAbsolutePath();
                    albumName = getLastPathSegment(folderPath);
                }

                album.addAlbumItem(albumName, folderPath, path, uri);
                album.getAlbumItem(albumName).addImageItem(imageItem);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return album;
    }

    public static String getLastPathSegment(String content) {
        if (content == null || content.length() == 0) {
            return "";
        }
        String[] segments = content.split("/");
        if (segments.length > 0) {
            return segments[segments.length - 1];
        }
        return "";
    }
}
