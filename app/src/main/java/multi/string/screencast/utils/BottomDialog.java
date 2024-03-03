package multi.string.screencast.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.WindowManager;

import androidx.annotation.NonNull;

public class BottomDialog extends Dialog {
    public BottomDialog(@NonNull Context context, int layoutId, int defaultHeight, int style, int gravity, boolean cancelable) {
        super(context);
        init(layoutId, defaultHeight, style, gravity, cancelable);
    }

    private void init(int layoutId, int defaultHeight, int style, int gravity, boolean cancelable) {
        setContentView(layoutId);
        WindowManager.LayoutParams param = getWindow().getAttributes();
        if (defaultHeight != -1) {
            param.height = defaultHeight;//WindowManager.LayoutParams.MATCH_PARENT;
        }
        param.width = WindowManager.LayoutParams.MATCH_PARENT;
        param.gravity = gravity;
        getWindow().setAttributes(param);
        getWindow().setBackgroundDrawable(null);
        getWindow().setWindowAnimations(style);

        setCanceledOnTouchOutside(cancelable);
        setCancelable(cancelable);
    }

}

