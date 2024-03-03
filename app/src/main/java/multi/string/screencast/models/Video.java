package multi.string.screencast.models;

public class Video {
    public int id;
    public String path;
    public String title;
    public int    duration;
    public int    width;
    public int    height;
    public String firstFramePath;
    public boolean is_show;
    public Video() {
        id = -1;
        path = "";
        title = "";
        duration = -1;
        width = -1;
        height = -1;
        firstFramePath = "";
        is_show = false;
    }

    public Video(int id, String path, String name, int duration, int width, int height, String picPath) {
        this.id = id;
        this.path = path;
        this.title = name;
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.firstFramePath = picPath;
        is_show = false;
    }

    public boolean isIs_show() {
        return is_show;
    }

    public void setIs_show(boolean is_show) {
        this.is_show = is_show;
    }
}

