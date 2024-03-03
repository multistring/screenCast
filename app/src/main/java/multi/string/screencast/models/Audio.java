package multi.string.screencast.models;

public class Audio {
    public long id;
    public String path;
    public String title;
    public int    duration;
    public boolean is_show;

    public Audio() {
        id = -1;
        path = "";
        title = "";
        duration = -1;
        is_show = false;
    }

    public Audio(long id, String path, String title, int duration, boolean is_show) {
        this.id = id;
        this.path = path;
        this.title = title;
        this.duration = duration;
        this.is_show = is_show;
    }

    public boolean isIs_show() {
        return is_show;
    }

    public void setIs_show(boolean is_show) {
        this.is_show = is_show;
    }
}
