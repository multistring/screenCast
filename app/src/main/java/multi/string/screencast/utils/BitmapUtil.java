package multi.string.screencast.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapUtil {
    public static Bitmap cropBitmap(Bitmap bitmap, float showRate, float videoRate) {
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();

        if (videoRate > showRate) {
            int cropHeight = h;
            int cropWidth = (int)(cropHeight*showRate);
            int startPosX = (w-cropWidth)/2;
            return Bitmap.createBitmap(bitmap, startPosX, 0, cropWidth, cropHeight, null, false);
        } else {
            int cropWidth = w;
            int cropHeight = (int)(cropWidth/showRate);
            int startPosY = (h-cropHeight)/2;
            return Bitmap.createBitmap(bitmap, 0, startPosY, cropWidth, cropHeight, null, false);
        }
    }

    public static Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        try {
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            return bm1;
        } catch (OutOfMemoryError ex) {
        }
        return null;
    }
}

