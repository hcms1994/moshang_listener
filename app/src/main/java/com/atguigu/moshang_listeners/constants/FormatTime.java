package com.atguigu.moshang_listeners.constants;

/**
 * Created by Administrator on 2017/11/16 0016.
 */

public class FormatTime {

    public static String toTime(int time)
    {
        time/=1000;
        int minute =time/60;
        int second =time%60;
        minute%=60;
        return String.format("%02d:%02d",minute,second);
    }

}
