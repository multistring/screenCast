package multi.string.screencast.dataloaders;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

import multi.string.screencast.models.Audio;
import multi.string.screencast.utils.FileUtil;

public class AudioLoader {
    public static ArrayList<Audio> getAudios(Context context){
        ContentResolver mContentResolver = context.getContentResolver();
        ArrayList arrayList = new ArrayList();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = mContentResolver.query(musicUri, null, null, null, null);

        if (musicCursor == null) {
            return arrayList;
        }
        if(musicCursor!=null && musicCursor.moveToFirst()){
            int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
            int albumId = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int data= musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int albumkey=musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                long thisalbumId = musicCursor.getLong(albumId);
                String path= musicCursor.getString(data);
                String AlbumKey = musicCursor.getString(albumkey);

                Audio audio = new Audio(thisId, path, FileUtil.getAudioFileName(path), -1, false);
                arrayList.add(audio);

            } while (musicCursor.moveToNext());
        }
        musicCursor.close();

        return arrayList;
    }


}

