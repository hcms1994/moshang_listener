package com.atguigu.moshang_listeners.service;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;


import com.atguigu.moshang_listeners.R;
import com.atguigu.moshang_listeners.data.MusicList;
import com.atguigu.moshang_listeners.utils.CacheUtils;

import java.io.IOException;

/**
 * Created by Administrator on 2017/11/15 0015.
 */

public class MusicService extends Service {

    //播放控制命令，标识操作
    public static final int COMMAND_UNKNOWN =-1;
    public static final int COMMAND_PLAY =0;
    public static final int COMMAND_PAUSE =1;
    public static final int COMMAND_STOP =2;
    public static final int COMMAND_RESUME =3;
    public static final int COMMAND_PREVIOUS =4;
    public static final int COMMAND_NEXT =5;
    public static final int COMMAND_CHECK_IS_PLAYING =6;
    public static final int COMMAND_SEEK_TO =7;
    //播放器状态
    public static final int STATUS_PLAYING =0;
    public static final int STATUS_PAUSED =1;
    public static final int STATUS_STOPPED =2;
    public static final int STATUS_COMPLETED =3;
    //广播标识
    public static String BROADCAST_MUSICSERVICE_CONTROL="MusicService.ACTION_CONTROL";
    public static String BROADCAST_MUSICSERVICE_UPDATE_STATUS ="MusicService.ACTION_UPDATE";
    public static String BROADCAST_LOCALSONGFRAGMENT_CONTROL="LocalSongFragment.ACTION_CONTROL";

    //媒体播放类
    private MediaPlayer player =new MediaPlayer();
    private CommandReceiver receiver;

    //歌曲序号，下标从0开始
    public static int number =0;
    private int status;

    //播放模式常量
    public static final int MODE_ORDER_CYCLE =0;
    public static final int MODE_SINGLE_CYCLE =1;
    public static final int MODE_LIST_CYCLE =2;
    public static final int MODE_RANDOM_CYCLE =3;
    public int play_mode;

    @Override
    public void onCreate() {
        super.onCreate();
        //绑定广播接收器
        bindCommandReceiver();
        status =MusicService.STATUS_STOPPED;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    //绑定广播接收器
    private void bindCommandReceiver()
    {
        receiver =new CommandReceiver();
        IntentFilter filter =new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        registerReceiver(receiver,filter);
    }

    //发送广播，提醒状态改变了
    private void sendBroadcastOnStatusChange(int status)
    {
        Intent intent =new Intent(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        intent.putExtra("status",status);
        if(status!=STATUS_STOPPED)
        {
            intent.putExtra("time",player.getCurrentPosition());
            intent.putExtra("duration",player.getDuration());
            intent.putExtra("number",number);
            intent.putExtra("musicName", MusicList.getMusicList().get(number).getMusicName());
            intent.putExtra("musicArtist",MusicList.getMusicList().get(number).getMusicArtist());
        }
        sendBroadcast(intent);
    }

    //内部类，接受广播命令，并执行操作
    class CommandReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取命令
            int command =intent.getIntExtra("command",MusicService.COMMAND_UNKNOWN);
            //执行命令
            switch(command)
            {
                case COMMAND_SEEK_TO:
                    seekTo(intent.getIntExtra("time",0));
                    break;
                case MusicService.COMMAND_PLAY:
                    number =intent.getIntExtra("number",0);
                    play(number);
                    break;
                case COMMAND_PREVIOUS:
                    moveNumberToPrevious();
                    break;
                case COMMAND_NEXT:
                    moveNumberToNext();
                    break;
                case COMMAND_PAUSE:
                    pause();
                    break;
                case COMMAND_STOP:
                    stop();
                    break;
                case COMMAND_RESUME:
                    resume();
                    break;
                case COMMAND_CHECK_IS_PLAYING:
                    if(player!=null&&player.isPlaying())
                    {
                        sendBroadcastOnStatusChange(MusicService.STATUS_PLAYING);
                    }
                    break;
                default:
                    break;
            }
        }
    }


    //读取音乐文件
    private void load(int number)
    {
        try {
            player.reset();
            player.setDataSource(MusicList.getMusicList().get(number).getMusicPath());
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.setOnCompletionListener(completionListener);
    }

    //播放结束监听器
    MediaPlayer.OnCompletionListener completionListener =new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {

            play_mode = CacheUtils.getInt(MusicService.this,"playmode");
            if(play_mode ==MODE_ORDER_CYCLE)
            {
                if(number==MusicList.getMusicList().size()-1)
                {
                    Toast.makeText(MusicService.this,MusicService.this.getString(R.string.tip_reach_bottom),Toast.LENGTH_SHORT).show();
                }else
                {
                    ++number;
                }
            }else if(play_mode ==MODE_SINGLE_CYCLE)
            {
            }else if(play_mode==MODE_LIST_CYCLE)
            {
                if(number==MusicList.getMusicList().size()-1)
                {
                    number =0;
                }else
                {
                    ++number;
                }
            }else if(play_mode==MODE_RANDOM_CYCLE)
            {
                int totalNum =MusicList.getMusicList().size();
                int num =(int)(Math.random()*totalNum);
                if(num ==number)
                {
                    number+=1;
                }else
                {
                    number =num;
                }
            }
            sendBroadcastOnStatusChange(MusicService.STATUS_COMPLETED);
            play(number);
        }
    };

    //播放音乐
    private void play(int number)
    {
        if(player!=null&&player.isPlaying())
        {
            player.stop();
        }
        load(number);
        player.start();
        status =MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChange(status);
    }
    //暂停播放
    private void pause()
    {
        if(player.isPlaying())
        {
            player.pause();
            status =MusicService.STATUS_PAUSED;
            sendBroadcastOnStatusChange(status);
        }
    }
    //停止播放
    private void stop()
    {
        player.stop();
        status =MusicService.STATUS_STOPPED;
        sendBroadcastOnStatusChange(status);
    }

    //恢复播放（暂停之后）
    private void resume()
    {
        player.start();
        status =MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChange(status);
    }
    //重新播放（播放完毕）
    private void replay()
    {
        player.start();
        status =MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChange(status);
    }

    //跳转至播放位置
    private void seekTo(int time)
    {
        player.seekTo(time);
        status =MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChange(MusicService.STATUS_PLAYING);
    }

    //选择下一曲
    private void moveNumberToNext()
    {
        if(number==(MusicList.getMusicList().size()-1))
        {
            Toast.makeText(MusicService.this,MusicService.this.getString(R.string.tip_reach_bottom),Toast.LENGTH_SHORT).show();
        }else
        {
            ++number;
            play(number);
        }
    }

    //选择上一曲
    private void moveNumberToPrevious()
    {
        //判断是否到达了列表顶端
        if(number==0)
        {
            Toast.makeText(MusicService.this,MusicService.this.getString(R.string.tip_reach_top),Toast.LENGTH_SHORT).show();
        }else
        {
            --number;
            play(number);
        }
    }


    @Override
    public void onDestroy() {
        sendBroadcastOnStatusChange(MusicService.STATUS_STOPPED);
        if(player!=null)
        {
            player.release();
        }
        unregisterReceiver(receiver);
        super.onDestroy();
    }

}
