package multi.string.screencast.dataloaders;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;

import multi.string.screencast.models.Video;
import multi.string.screencast.utils.FileUtil;

public class VideoLoader {
    public static ArrayList<Video> getVideos(Context context){
        ContentResolver mContentResolver = context.getContentResolver();
        ArrayList arrayList = new ArrayList();
        //https://cloud.tencent.com/developer/article/1719789
        String[] mediaColumns = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.TITLE, MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED, MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH, MediaStore.Video.Media.HEIGHT };

        Cursor mCursor = mContentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaColumns,
                MediaStore.Video.Media.MIME_TYPE + "=? or "+MediaStore.Video.Media.MIME_TYPE +"=? or "
                        +MediaStore.Video.Media.MIME_TYPE +"=? or "+MediaStore.Video.Media.MIME_TYPE +"=? or "+MediaStore.Video.Media.MIME_TYPE +"=?",
                new String[]{"video/mp4", "video/x-matroska", "video/mp2ts", "video/x-flv"},
                MediaStore.Video.Media.DATE_ADDED+" asc");

        if (mCursor == null) {
            return arrayList;
        }

        // 注意，DATA 数据在 Android Q 以前代表了文件的路径，但在 Android Q上该路径无法被访问，因此没有意义。
        int ixData = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        int ixMime = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);
        // ID 是在 Android Q 上读取文件的关键字段
        int ixId = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
        int ixSize = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
        int ixTitle = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);

        boolean ret = mCursor.moveToLast();
        if (ret) {
            do {
                @SuppressLint("Range") int videoId = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Video.Media._ID));
                @SuppressLint("Range") String path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Video.Media.DATA));
                @SuppressLint("Range") int width = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Video.Media.WIDTH));
                @SuppressLint("Range") int height = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Video.Media.HEIGHT));
                @SuppressLint("Range") int duration = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Video.Media.DURATION));

                if ((width > 0 && height > 0) || duration > 0) {
                    Video video = new Video(videoId, path, FileUtil.getVideoFileName(path), duration, width, height, "");
                    arrayList.add(video);
                }
            } while (mCursor.moveToPrevious());
        }
        mCursor.close();
        return arrayList;
    }

    @SuppressLint("Range")
    public static void getVidesList(Context context){
        String[] thumbColumns = {MediaStore.Video.Thumbnails.DATA,
                MediaStore.Video.Thumbnails.VIDEO_ID};

        // 视频其他信息的查询条件
        String[] mediaColumns = {MediaStore.Video.Media._ID, MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA, MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_TAKEN,};

        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media
                        .EXTERNAL_CONTENT_URI,
                mediaColumns, null, null, null);

        if (cursor == null) {
            return;
        }

        boolean ret = cursor.moveToFirst();
        if (ret) {
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor
                        .getColumnIndex(MediaStore.Video.Media._ID));
                Cursor thumbCursor = context.getContentResolver().query(
                        MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                        thumbColumns, MediaStore.Video.Thumbnails.VIDEO_ID
                                + "=" + id, null, null);
                if (thumbCursor.moveToFirst()) {
                    Log.i("multistring", "ThumbPath is "+thumbCursor.getString(thumbCursor
                            .getColumnIndex(MediaStore.Video.Thumbnails.DATA))+","+cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media
                            .DATA))+","+cursor.getString(3));
                }
            } while (cursor.moveToNext());
        }
    }
}

