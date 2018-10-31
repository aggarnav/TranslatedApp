package com.gsoc.ijosa.liquidgalaxycontroller.advancedTools;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Ivan Josa on 30/06/16.
 */
class LGTask implements Parcelable {

    public static final Creator CREATOR =
            new Creator() {
                public LGTask createFromParcel(Parcel in) {
                    return new LGTask(in);
                }

                public LGTask[] newArray(int size) {
                    return new LGTask[size];
                }
            };
    private long id;
    private String title;
    private String description;
    private String script;
    private byte[] image;
    private String shutdownScript;
    private String ip;
    private String user;
    private String password;
    private String browserUrl;

    private boolean isRunning;

    LGTask() {
        id = 0;
        title = "";
        script  = "";
        description = "";
        image = null;
    }


    private LGTask(Parcel in) {
        id = in.readLong();
        title = in.readString();
        description = in.readString();
        script = in.readString();
        image = new byte[in.readInt()];
        in.readByteArray(image);
        shutdownScript = in.readString();
        ip = in.readString();
        user = in.readString();
        password = in.readString();
        browserUrl = in.readString();
        isRunning = in.readByte() != 0;
    }

    String getBrowserUrl() {
        return browserUrl;
    }

    void setBrowserUrl(String browserUrl) {
        this.browserUrl = browserUrl;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    String getScript() {
        return script;
    }

    void setScript(String script) {
        this.script = script;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    String getShutdownScript() {
        return shutdownScript;
    }

    void setShutdownScript(String shutdownScript) {
        this.shutdownScript = shutdownScript;
    }

    String getIp() {
        return ip;
    }

    void setIp(String ip) {
        this.ip = ip;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    boolean isRunning() {
        return isRunning;
    }

    void setRunning(boolean running) {
        isRunning = running;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LGTask task = (LGTask) o;

        return task.getId() == this.getId();

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Long.valueOf(id).hashCode();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(script);
        dest.writeInt(image.length);
        dest.writeByteArray(image);
        dest.writeString(shutdownScript);
        dest.writeString(ip);
        dest.writeString(password);
        dest.writeString(user);
        dest.writeString(browserUrl);
        dest.writeByte((byte) (isRunning ? 1 : 0));
    }
}
