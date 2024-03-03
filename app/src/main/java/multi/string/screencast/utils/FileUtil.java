package multi.string.screencast.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileUtil {
    public static String tail = "10";

    public static String getVideoFileName(String path){
        if (TextUtils.isEmpty(path)){
            return null;
        }
        File f =new File(path);
        if (!f.exists()) {//判断路径是否存在
            return null;
        }
        String name = f.getName();

        if (TextUtils.isEmpty(name)){
            return "";
        } else  {
            String newName = name;
            if (name.endsWith(".mp4")) {
                newName = name.substring(0, name.length()-4);
            } else if (name.endsWith(".mkv")){
                newName = name.substring(0, name.length()-4);
            } else if (name.endsWith(".ts")){
                newName = name.substring(0, name.length()-3);
            } else if (name.endsWith(".avi")){
                newName = name.substring(0, name.length()-4);
            } else if (name.endsWith(".flv")){
                newName = name.substring(0, name.length()-4);
            }

            return newName;
        }
    }

    public static String getAudioFileName(String path){
        if (TextUtils.isEmpty(path)){
            return null;
        }
        File f =new File(path);
        if (!f.exists()) {//判断路径是否存在
            return null;
        }
        String name = f.getName();

        if (TextUtils.isEmpty(name)){
            return "";
        } else  {
            String newName = name;
            if (name.endsWith(".aac")) {
                newName = name.substring(0, name.length()-4);
            } else if (name.endsWith(".mp3")){
                newName = name.substring(0, name.length()-4);
            } else if (name.endsWith(".m4a")){
                newName = name.substring(0, name.length()-4);
            } else if (name.endsWith(".wav")){
                newName = name.substring(0, name.length()-4);
            } else if (name.endsWith(".amr")){
                newName = name.substring(0, name.length()-4);
            } else if (name.endsWith(".flac")){
                newName = name.substring(0, name.length()-5);
            }

            return newName;
        }
    }

    public static boolean createDir(String path){
        if (path == null || path.length() <=0){
            return false;
        }

        try {
            File dir = new File(path);
            if(!dir.exists()){
                boolean ret = dir.mkdirs();
                return ret;
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return true;
    }

    public static String getFoldPathByFile(String path){
        if (path == null || path.length() <=0){
            return null;
        }

        try {
            File imageFile = new File(path);
            File folderFile = imageFile.getParentFile();
            if (folderFile == null || !imageFile.exists()) {
                return null;
            }

            return folderFile.getAbsolutePath();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    //https://blog.csdn.net/weixin_38322371/article/details/94432906

    /**
     * 将本地图片转成Bitmap
     * @param path 已有图片的路径
     * @return
     */
    public static Bitmap openImage(String path) {
        Bitmap bitmap = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
            bitmap = BitmapFactory.decodeStream(bis);
            bis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }


}
