package com.atguigu.moshang_listeners;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.atguigu.moshang_listeners.data.Music;
import com.atguigu.moshang_listeners.data.MusicList;
import com.atguigu.moshang_listeners.service.MusicService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private LinearLayout turn_layout;
    private ProgressBar progressBar;
    public TextView musicName;
    public TextView musicArtist;
    private ImageButton btn_next;
    private ImageButton btn_playorpause;
    private ImageButton btn_previous;

    //歌曲列表
    private ListView listView;
    //歌曲列表对象
    private ArrayList<Music> musicArrayList;

    //歌曲序号,下标从0开始
    public int number =0;
    //播放状态
    private int status;
    //广播接收器，接受播放状态
    private StatusChnageReceiver receiver;
    //总时长和当前时间位置
    private int time =0;
    private int duration =0;
    //进度条控制常量
    private static final int PROGRESS_INCREASE =0;
    private static final int PROGRESS_PAUSE =1;
    private static final int PROGRESS_RESET =2;
    private Handler progressbarHandler;

    public int flag =0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取运行时权限，访问外部SD卡中文件
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
//        //背景图和状态栏融合在一起
//        if(Build.VERSION.SDK_INT>=21)
//        {
//            View decorView =getWindow().getDecorView();
//            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//            getWindow().setStatusBarColor(Color.TRANSPARENT);
//        }


        //初始化组件view
        initView();

        //初始化音乐列表
        initMusicList();
        //listview 添加数据
        initListView();
        if(musicArrayList.isEmpty())
        {
            Toast.makeText(this,"当前没有歌曲文件",Toast.LENGTH_SHORT).show();
        }

        //为显示组件注册监听器
        registerListener();
        //绑定广播接收器，可以接受广播
        bindStatusChangedReceiver();
        //进度条线程
        initProgressBarHandler();

        //开启服务
        Intent intent =new Intent(MainActivity.this,MusicService.class);
        startService(intent);
        status =MusicService.STATUS_STOPPED;

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //播放相应歌曲
                number =position;
                sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
            }
        });

        //设置MainActivity中初始布局
        if(flag==0)
        {
            flag =1;
            musicName.setText(MusicList.getMusicList().get(0).getMusicName());
            musicArtist.setText(MusicList.getMusicList().get(0).getMusicArtist());
        }
    }

    //初始化组件view
    private void initView() {

        listView =(ListView) findViewById(R.id.listview);
        turn_layout =(LinearLayout) findViewById(R.id.turn_layout);
        progressBar =(ProgressBar) findViewById(R.id.progressbar);
        musicName =(TextView) findViewById(R.id.musicName);
        musicArtist =(TextView) findViewById(R.id.musicArtist);
        btn_next =(ImageButton) findViewById(R.id.btn_next);
        btn_playorpause =(ImageButton) findViewById(R.id.btn_playorpause);
        btn_previous =(ImageButton) findViewById(R.id.btn_previous);
    }

    //向ListView中添加数据
    private void initListView() {
        List<Map<String,String>> list_map =new ArrayList<Map<String,String>>();
        HashMap<String,String> map;
        SimpleAdapter simpleAdapter;
        for(Music music:musicArrayList)
        {
            map =new HashMap<String,String>();
            map.put("musicName",music.getMusicName());
            map.put("musicArtist",music.getMusicArtist());
            list_map.add(map);
        }

        String[] from =new String[]{"musicName","musicArtist"};
        int[] to =new int[]{R.id.listview_tv_title_item1,R.id.listview_tv_artist_item1};

        simpleAdapter =new SimpleAdapter(this,list_map,R.layout.listview_item,from,to);
        listView.setAdapter(simpleAdapter);
    }

    //初始化音乐列表，利用MediaStore获取本地歌曲
    private void initMusicList() {
        musicArrayList = MusicList.getMusicList();
        //避免重复添加歌曲
        if(musicArrayList.isEmpty())
        {
            Cursor mMusicCursor =getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,null,null,
                    MediaStore.Audio.AudioColumns.TITLE);
            //标题索引
            int indexTitle =mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
            //艺术家索引
            int indexArtist =mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
            //总时长
            int indexTotalTime =mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
            //路径
            int indexPath =mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
            //专辑Id
            int indexAlbumId =mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID);

            //通过cursor游标遍历数据库，并将Music对象加载到ArrayList中
            for(mMusicCursor.moveToFirst();!mMusicCursor.isAfterLast();mMusicCursor.moveToNext())
            {
                String strTitle =mMusicCursor.getString(indexTitle);
                String strTotalTime =mMusicCursor.getString(indexTotalTime);
                String strArtist =mMusicCursor.getString(indexArtist);
                String strPath =mMusicCursor.getString(indexPath);
                int albumId =mMusicCursor.getInt(indexAlbumId);

                if(strArtist.equals("<unknown>"))
                {
                    strArtist ="无艺术家";
                }
                Music music =new Music(strTitle,strArtist,strPath,strTotalTime,albumId);
                musicArrayList.add(music);
            }

        }
    }

    //为显示组件注册监听器
    private void registerListener() {
        //上一首
        btn_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
            }
        });
        //播放<--->暂停
        btn_playorpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status)
                {
                    case MusicService.STATUS_PLAYING:
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED:
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED:
                        Log.e("数据","123");
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                    default:
                        break;
                }
            }
        });
        //下一首
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
            }
        });

        //线性布局，跳转到详情Activity
        turn_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(MainActivity.this,PlaySectionActivity.class);
                intent.putExtra("status",status);
                intent.putExtra("time",time);
                intent.putExtra("number",number);
                intent.putExtra("duration",duration);
                startActivity(intent);
            }
        });

    }


    //绑定广播接收器，可以接受广播
    private void bindStatusChangedReceiver() {
        receiver =new StatusChnageReceiver();
        IntentFilter filter =new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver,filter);
    }

    //进度条Handler
    private void initProgressBarHandler() {
        progressbarHandler =new Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what)
                {
                    case PROGRESS_INCREASE:
                        if(progressBar.getProgress()<duration)
                        {
                            progressBar.setProgress(time);
                            progressbarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE,1000);
                            time+=1000;
                        }
                        break;
                    case PROGRESS_PAUSE:
                        progressbarHandler.removeMessages(PROGRESS_INCREASE);
                        break;
                    case PROGRESS_RESET:
                        progressbarHandler.removeMessages(PROGRESS_INCREASE);
                        progressBar.setProgress(0);
                        break;
                }
            }
        };
    }

    //发送命令，控制音乐播放
    public void sendBroadcastOnCommand(int command)
    {
        Intent intent =new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command",command);
        //根据不同的数据，封装不同的数据
        switch (command)
        {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number",number);
                break;
            case MusicService.COMMAND_PREVIOUS:
                break;
            case MusicService.COMMAND_NEXT:
                break;
            case MusicService.COMMAND_PAUSE:
                intent.putExtra("number",number);
                break;
            case MusicService.COMMAND_STOP:
                intent.putExtra("number",number);
                break;
            case MusicService.COMMAND_RESUME:
                intent.putExtra("number",number);
                break;
            case MusicService.COMMAND_SEEK_TO:
                break;
            default:
                break;
        }
        sendBroadcast(intent);
    }

    //内部类，用于播放器状态更新的接受广播
    class StatusChnageReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取播放器状态
            status =intent.getIntExtra("status",-1);
            switch (status)
            {
                case MusicService.STATUS_PLAYING:
                    progressbarHandler.removeMessages(PROGRESS_INCREASE);
                    time  =intent.getIntExtra("time",0);
                    duration =intent.getIntExtra("duration",0);
                    number =intent.getIntExtra("number",number);
                    progressBar.setProgress(time);
                    progressBar.setMax(duration);
                    progressbarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE,1000);
                    if(MusicList.getMusicList().get(number).getMusicName().length()>10)
                    {
                        musicName.setText(MusicList.getMusicList().get(number).getMusicName().substring(0,8)+"...");
                    }else
                    {
                        musicName.setText(MusicList.getMusicList().get(number).getMusicName());
                    }
                    musicArtist.setText(MusicList.getMusicList().get(number).getMusicArtist());
                    btn_playorpause.setBackgroundResource(R.drawable.sing_play);
                    break;
                case MusicService.STATUS_PAUSED:
                    progressbarHandler.sendEmptyMessage(PROGRESS_PAUSE);
                    btn_playorpause.setBackgroundResource(R.drawable.sing_pause);
                    break;
                case MusicService.STATUS_STOPPED:
                    btn_playorpause.setBackgroundResource(R.drawable.sing_pause);
                    break;
                case MusicService.STATUS_COMPLETED:
                    number =intent.getIntExtra("number",0);
                    progressbarHandler.sendEmptyMessage(PROGRESS_RESET);
                    btn_playorpause.setBackgroundResource(R.drawable.sing_pause);
                    break;
                default:
                    break;

            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);

    }

    @Override
    protected void onDestroy() {
        if(status==MusicService.STATUS_STOPPED)
        {
            stopService(new Intent(this,MusicService.class));
        }
        super.onDestroy();
    }

}
