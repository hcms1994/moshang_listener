package com.atguigu.moshang_listeners.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.atguigu.moshang_listeners.service.MusicService;


/**
 * Created by Administrator on 2017/8/17 0017.
 */

public class CacheUtils {

    public static boolean getBoolean(Context context, String key)
    {
        SharedPreferences sp =context.getSharedPreferences("atguigu1",Context.MODE_PRIVATE);
        return sp.getBoolean(key,false);
    }

    public static void putBoolean(Context context, String key,boolean value)
    {
        SharedPreferences sp =context.getSharedPreferences("atguigu1",Context.MODE_PRIVATE);
        sp.edit().putBoolean(key,value).commit();
    }

    //缓存文本数据
    public static void putString(Context context, String key, String value) {
        SharedPreferences sp =context.getSharedPreferences("atguigu1",Context.MODE_PRIVATE);
        sp.edit().putString(key,value).commit();

    }

    //获取文本缓存文本数据
    public static String getString(Context context, String key) {

        SharedPreferences sp =context.getSharedPreferences("atguigu1",Context.MODE_PRIVATE);
        return sp.getString(key,null);
    }

    public static int getInt(Context context, String key)
    {
        SharedPreferences sp =context.getSharedPreferences("atguigu1",Context.MODE_PRIVATE);
        return sp.getInt(key, MusicService.MODE_ORDER_CYCLE);
    }

    public static void putInt(Context context, String key,int value)
    {
        SharedPreferences sp =context.getSharedPreferences("atguigu1",Context.MODE_PRIVATE);
        sp.edit().putInt(key,value).commit();
    }
}
