package multi.string.screencast.models;

public class MediaListItem {
    private String path;
    private String name;
    private String frontpgPath; //
    private int    type = VIDEO_TYPE;//0 video 1 pic 2 audio
    private int    width = 0;
    private int    height = 0;
    private int    dur   = 0; //时长单位，为秒
    private boolean    bePlayed = false; //是否已播放

    public static int VIDEO_TYPE = 0;
    public static int PIC_TYPE = 1;
    public static int AUDIO_TYPE =2;

    public MediaListItem() {
    }

    public MediaListItem(String path, String name, String frontpgPath, int type, int widht, int height, int dur, boolean bePlayed) {
        this.path = path;
        this.name = name;
        this.type = type;
        this.width = widht;
        this.height = height;
        this.dur = dur;
        this.bePlayed = bePlayed;
        this.frontpgPath = frontpgPath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getDur() {
        return dur;
    }

    public void setDur(int dur) {
        this.dur = dur;
    }

    public boolean isBePlayed() {
        return bePlayed;
    }

    public void setBePlayed(boolean bePlayed) {
        this.bePlayed = bePlayed;
    }

    public String getFrontpgPath() {
        return frontpgPath;
    }

    public void setFrontpgPath(String frontpgPath) {
        this.frontpgPath = frontpgPath;
    }
}

