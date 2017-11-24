package com.atguigu.moshang_listeners.data;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/11/15 0015.
 */

//采用单一实例，只能通过getMusicList方法获取
    //共享，唯一的AyyayList<Music>对象
public class MusicList {

    private static ArrayList<Music> musicArrayList =new ArrayList<Music>();
    private MusicList()
    {
    }
    public static ArrayList<Music> getMusicList()
    {
        return musicArrayList;
    }
}
