package multi.string.screencast.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class AlbumItem implements Parcelable {
    public String name;
    public String folderPath;
    public String coverImagePath;
    public Uri coverImageUri;
    public ArrayList<Photo> photos;
    public boolean isSelect = false;

    AlbumItem(String name, String folderPath, String coverImagePath, Uri coverImageUri) {
        this.name = name;
        this.folderPath = folderPath;
        this.coverImagePath = coverImagePath;
        this.coverImageUri = coverImageUri;
        this.photos = new ArrayList<>();
    }

    protected AlbumItem(Parcel in) {
        name = in.readString();
        folderPath = in.readString();
        coverImagePath = in.readString();
        coverImageUri = in.readParcelable(Uri.class.getClassLoader());
        photos = in.createTypedArrayList(Photo.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(folderPath);
        dest.writeString(coverImagePath);
        dest.writeParcelable(coverImageUri, flags);
        dest.writeTypedList(photos);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AlbumItem> CREATOR = new Creator<AlbumItem>() {
        @Override
        public AlbumItem createFromParcel(Parcel in) {
            return new AlbumItem(in);
        }

        @Override
        public AlbumItem[] newArray(int size) {
            return new AlbumItem[size];
        }
    };

    public void addImageItem(Photo imageItem) {
        if (this.photos.size() <=2000) {
            this.photos.add(imageItem);
        }
    }

    public void addImageItem(int index, Photo imageItem) {
        if (this.photos.size() <=2000) {
            this.photos.add(index, imageItem);
        }
    }
}

