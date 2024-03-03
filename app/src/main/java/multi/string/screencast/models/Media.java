package multi.string.screencast.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Media implements Parcelable {
    public String path;
    public String name;
    public int    type = VIDEO_TYPE;//0 video 1 pic 2 audio

    public static int VIDEO_TYPE = 0;
    public static int PIC_TYPE = 1;
    public static int AUDIO_TYPE =2;

    /**
     * 选择结果，返回为 ArrayList&lt;String&gt; 图片路径集合
     */
    public static final String EXTRA_RESULT = "select_result";

    protected Media(Parcel in) {
        path = in.readString();
        name = in.readString();
        type = in.readInt();
    }

    public Media(String path, String name, int type) {
        this.path = path;
        this.name = name;
        this.type = type;
    }

    public static final Creator<Media> CREATOR = new Creator<Media>() {
        @Override
        public Media createFromParcel(Parcel in) {
            return new Media(in);
        }

        @Override
        public Media[] newArray(int size) {
            return new Media[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(name);
        dest.writeInt(type);
    }
}
