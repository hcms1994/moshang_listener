package com.atguigu.moshang_listeners.data;

/**
 * Created by Administrator on 2017/11/15 0015.
 */

public class Music {
    private String musicName;
    private String musicArtist;
    private String musicPath;
    private String musicDuration;
    private int album_Id;

    public Music(String musicName, String musicArtist, String musicPath, String musicDuration, int album_Id) {
        this.musicName = musicName;
        this.musicArtist = musicArtist;
        this.musicPath = musicPath;
        this.musicDuration = musicDuration;
        this.album_Id =album_Id;

    }

    public String getMusicName() {
        return musicName;
    }

    public String getMusicArtist() {
        return musicArtist;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public String getMusicDuration() {
        return musicDuration;
    }

    public int getAlbum_Id() {
        return album_Id;
    }
}
