package com.atguigu.moshang_listeners;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.atguigu.moshang_listeners.constants.FormatTime;
import com.atguigu.moshang_listeners.data.MusicList;
import com.atguigu.moshang_listeners.service.MusicService;
import com.atguigu.moshang_listeners.utils.CacheUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaySectionActivity extends Activity {

    private TextView turn_musicName;
    private TextView turn_musicArtist;

    private SeekBar voice_seekbar;
    private TextView text_voice;
    private TextView lrc_tv_lrc;

    private TextView time_current;
    private SeekBar turn_seekbar;
    private TextView time_duration;

    private ImageButton play_style;
    private ImageButton play_previous;
    private ImageButton play_playorpause;
    private ImageButton play_next;
    private ImageButton play_save;

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
    private Handler seekbarHandler;

    private int playmode;

    private AsyncDownload asyncDownload;
    private String lrc_data;
    private String filename;

    //歌词打开状态
    private int LRC_STATUS =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //获取运行时权限，访问外部SD卡中文件
        if(ContextCompat.checkSelfPermission(PlaySectionActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(PlaySectionActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        //背景图和状态栏融合在一起
        if(Build.VERSION.SDK_INT>=21)
        {
            View decorView =getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_play_section);

        //初始化视图view
        findView();

        //为显示组件注册监听器
        registerListener();
        //绑定广播接收器，可以接受广播
        bindStatusChangedReceiver();
        initProgressBarHandler();

        //初始化部件
        initView();

        turn_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //进度条暂停移动
                seekbarHandler.sendEmptyMessage(PROGRESS_PAUSE);

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(status!=MusicService.STATUS_STOPPED)
                {
                    time =seekBar.getProgress();
                    //更新文本
                    time_current.setText(FormatTime.toTime(time));
                }
                if(status==MusicService.STATUS_PLAYING)
                {
                    sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                    //进度条恢复移动
                    seekbarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE,1000);
                }

            }
        });

    }

    //初始化视图
    private void initView() {
        status =getIntent().getIntExtra("status",MusicService.STATUS_STOPPED);
        time =getIntent().getIntExtra("time",0);
        duration =getIntent().getIntExtra("duration",0);
        turn_seekbar.setMax(duration);
        turn_seekbar.setProgress(time);
        number =getIntent().getIntExtra("number",0);
        turn_musicName.setText(MusicList.getMusicList().get(number).getMusicName());
        turn_musicArtist.setText(MusicList.getMusicList().get(number).getMusicArtist());
        time_current.setText(FormatTime.toTime(time));
        time_duration.setText(FormatTime.toTime(duration  ));
        //播放模式
        playmode =CacheUtils.getInt(PlaySectionActivity.this,"playmode");
        switch (playmode) {
            case MusicService.MODE_ORDER_CYCLE:
                play_style.setBackgroundResource(R.drawable.order_cycle);
                break;
            case MusicService.MODE_LIST_CYCLE:
                play_style.setBackgroundResource(R.drawable.list_cycle);
                break;
            case MusicService.MODE_SINGLE_CYCLE:
                play_style.setBackgroundResource(R.drawable.single_cycle);
                break;
            case MusicService.MODE_RANDOM_CYCLE:
                play_style.setBackgroundResource(R.drawable.random_cycle);
                break;
            default:
                break;
        }

    }

    //进度条Handler
    private void initProgressBarHandler() {
        seekbarHandler =new Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what)
                {
                    case PROGRESS_INCREASE:
                        if(turn_seekbar.getProgress()<duration)
                        {
                            turn_seekbar.setProgress(time);
                            seekbarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE,1000);
                            time_current.setText(FormatTime.toTime(time));
                            time+=1000;
                        }
                        break;
                    case PROGRESS_PAUSE:
                        seekbarHandler.removeMessages(PROGRESS_INCREASE);
                        break;
                    case PROGRESS_RESET:
                        seekbarHandler.removeMessages(PROGRESS_INCREASE);
                        turn_seekbar.setProgress(0);
                        time_current.setText("00:00");
                        break;
                }
            }
        };
    }

    //绑定广播接收器，可以接受广播
    private void bindStatusChangedReceiver() {
        receiver =new StatusChnageReceiver();
        IntentFilter filter =new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver,filter);
    }


    //初始化视图view
    private void findView() {
        turn_musicName =(TextView) findViewById(R.id.turn_musicName);
        turn_musicArtist =(TextView) findViewById(R.id.turn_musicArtist);
        voice_seekbar =(SeekBar) findViewById(R.id.voice_seekbar);
        text_voice =(TextView) findViewById(R.id.text_voice);
        lrc_tv_lrc =(TextView) findViewById(R.id.lrc_tv_lrc);
        time_current =(TextView) findViewById(R.id.time_current);
        turn_seekbar =(SeekBar) findViewById(R.id.turn_seekbar);
        time_duration =(TextView) findViewById(R.id.time_duration);
        play_style =(ImageButton) findViewById(R.id.play_style);
        play_previous =(ImageButton) findViewById(R.id.play_previous);
        play_playorpause =(ImageButton) findViewById(R.id.play_playorpause);
        play_next =(ImageButton) findViewById(R.id.play_next);
        play_save =(ImageButton) findViewById(R.id.play_save);
    }

    //为显示组件注册监听器
    private void registerListener() {
        play_style.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (playmode)
                {
                    case MusicService.MODE_ORDER_CYCLE:
                        playmode =MusicService.MODE_LIST_CYCLE;
                        play_style.setBackgroundResource(R.drawable.list_cycle);
                        //保存设置
                        CacheUtils.putInt(PlaySectionActivity.this,"playmode",playmode);
                        Toast.makeText(PlaySectionActivity.this,"列表循环",Toast.LENGTH_SHORT).show();
                        break;
                    case MusicService.MODE_LIST_CYCLE:
                        playmode =MusicService.MODE_SINGLE_CYCLE;
                        play_style.setBackgroundResource(R.drawable.single_cycle);
                        CacheUtils.putInt(PlaySectionActivity.this,"playmode",playmode);
                        Toast.makeText(PlaySectionActivity.this,"单曲循环",Toast.LENGTH_SHORT).show();
                        break;
                    case MusicService.MODE_SINGLE_CYCLE:
                        playmode =MusicService.MODE_RANDOM_CYCLE;
                        play_style.setBackgroundResource(R.drawable.random_cycle);
                        CacheUtils.putInt(PlaySectionActivity.this,"playmode",playmode);
                        Toast.makeText(PlaySectionActivity.this,"随机循环",Toast.LENGTH_SHORT).show();
                        break;
                    case MusicService.MODE_RANDOM_CYCLE:
                        playmode =MusicService.MODE_ORDER_CYCLE;
                        play_style.setBackgroundResource(R.drawable.order_cycle);
                        CacheUtils.putInt(PlaySectionActivity.this,"playmode",playmode);
                        Toast.makeText(PlaySectionActivity.this,"顺序循环",Toast.LENGTH_SHORT).show();
                        break;
                }

            }
        });

        play_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LRC_STATUS =0;
                lrc_tv_lrc.setText("");
                play_save.setBackgroundResource(R.drawable.close_songword);
                sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
            }
        });

        play_playorpause.setOnClickListener(new View.OnClickListener() {
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
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                    default:
                        break;
                }
            }
        });

        play_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LRC_STATUS =0;
                lrc_tv_lrc.setText("");
                play_save.setBackgroundResource(R.drawable.close_songword);
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
            }
        });

        play_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(LRC_STATUS==0)
                {
                    LRC_STATUS =1;
                    play_save.setBackgroundResource(R.drawable.open_songword);
                    try {
                        get_lrc(PlaySectionActivity.this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else if(LRC_STATUS==1)
                {
                    LRC_STATUS =0;
                    lrc_tv_lrc.setText("");
                    play_save.setBackgroundResource(R.drawable.close_songword);
                }
            }
        });
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
            case MusicService.COMMAND_RESUME:
                intent.putExtra("number",number);
                break;
            case MusicService.COMMAND_SEEK_TO:
                intent.putExtra("time",time);
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
                    seekbarHandler.removeMessages(PROGRESS_INCREASE);
                    time  =intent.getIntExtra("time",0);
                    duration =intent.getIntExtra("duration",0);
                    number =intent.getIntExtra("number",number);
                    turn_seekbar.setProgress(time);
                    turn_seekbar.setMax(duration);
                    seekbarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE,1000);
                    time_duration.setText(FormatTime.toTime(duration));
                    turn_musicName.setText(MusicList.getMusicList().get(number).getMusicName());
                    turn_musicArtist.setText(MusicList.getMusicList().get(number).getMusicArtist());
                    play_playorpause.setBackgroundResource(R.drawable.sing_play);
                    break;
                case MusicService.STATUS_PAUSED:
                    seekbarHandler.sendEmptyMessage(PROGRESS_PAUSE);
                    play_playorpause.setBackgroundResource(R.drawable.sing_pause);
                    break;
                case MusicService.STATUS_STOPPED:
                    time =0;
                    duration =0;
                    time_current.setText(FormatTime.toTime(time));
                    time_duration.setText(FormatTime.toTime(duration));
                    seekbarHandler.sendEmptyMessage(PROGRESS_RESET);
                    play_playorpause.setBackgroundResource(R.drawable.sing_pause);
                    break;
                case MusicService.STATUS_COMPLETED:
                    LRC_STATUS =0;
                    lrc_tv_lrc.setText("");
                    play_save.setBackgroundResource(R.drawable.close_songword);

                    number =intent.getIntExtra("number",0);
                    //seekbarHandler.sendEmptyMessage(PROGRESS_RESET);
                    turn_seekbar.setProgress(0);
                    time_current.setText("00:00");
                    play_playorpause.setBackgroundResource(R.drawable.sing_pause);
                    break;
                default:
                    break;

            }
        }
    }

    //音量控制
    private void auto_Control() {
        //获取音量管理器
        final AudioManager audioManager =(AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        //设置当前调整音量大小只是针对媒体音乐
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //设置滑动条最大值
        final int max_progress =audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        voice_seekbar.setMax(max_progress);
        //获取当前音量值
        int progressNum =audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        voice_seekbar.setProgress(progressNum);
        text_voice.setText((progressNum*100)/(max_progress)+"%");
        voice_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                text_voice.setText((progress*100)/(max_progress)+"%");
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,AudioManager.FLAG_PLAY_SOUND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    //异步任务，下载歌曲歌词
    class AsyncDownload extends AsyncTask<String,Integer,String>
    {

        //执行时调用此方法
        @Override
        protected String doInBackground(String... params) {
            String url =null;
            try {
                url =LrcFileDownLoad.getSongLRCUrl(params[0],params[1],params[2]);
                lrc_data = LrcFileDownLoad.getHtmlCode(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return lrc_data;
        }

        //任务执行前调用此方法
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            lrc_tv_lrc.setText("搜索歌词中");
        }

        //任务结束时调用此方法
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s!=null)
            {
                //写入文件
                try {
                    FileOutputStream outputStream =PlaySectionActivity.this.openFileOutput(filename,Context.MODE_PRIVATE);
                    outputStream.write(s.getBytes());
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String string =drawLrcWord(filename);
                lrc_tv_lrc.setText(string);
            }else
            {
                lrc_tv_lrc.setText("没有找到歌词！");
            }
        }
    }

    //判断本地歌词文件是否存在，把歌词存储到本地，以及读取本地歌词等功能
    private void get_lrc(Context context) throws IOException
    {
        //本程序内部存储空间的文件列表
        String[] files =context.fileList();
        if(MusicList.getMusicList().get(number).getMusicName()!=null)
        {
            filename =MusicList.getMusicList().get(number).getMusicName()+".lrc";
            List<String> fileList = Arrays.asList(files);
            //如果存在本地歌词，直接读出并显示，否则下载歌词
            if(fileList.contains(filename))
            {
//                FileInputStream fileInputStream =context.openFileInput(filename);
//                byte[] buffer =new byte[fileInputStream.available()];
//                fileInputStream.read(buffer);
                String string =drawLrcWord(filename);
                lrc_tv_lrc.setText(string);
            }else
            {
                //判断网络状态
                ConnectivityManager cwjManager =(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info =cwjManager.getActiveNetworkInfo();
                //网络可用则进行下载
                if(info!=null&&info.isAvailable())
                {
                    asyncDownload =new AsyncDownload();
                    asyncDownload.execute(LrcFileDownLoad.LRC_SEARCH_URL,MusicList.getMusicList().get(number).getMusicName().replace("(Live)","").replace(" ",""),
                            MusicList.getMusicList().get(number).getMusicArtist());
                }else
                {
                    Toast.makeText(PlaySectionActivity.this,"当前网络不佳，请检测网络配置！",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //解析歌词文件内容
    private String drawLrcWord(String filename)
    {
        String lrc_word ="";
        Pattern pattern =Pattern.compile("\\[\\d{2}:\\d{2}.\\d{2}\\]");
        File file =new File(getApplicationContext().getFilesDir()+"/"+filename);
        try {
            BufferedReader reader =new BufferedReader(new FileReader(file));
            String line ="";

            while((line=reader.readLine())!=null)
            {
                Matcher m =pattern.matcher(line);
                line =m.replaceAll("");
                line =line.replace("[ti:","");
                line =line.replace("[ar:","");
                line =line.replace("[al:","");
                line =line.replace("[by:","");
                line =line.replace("[i","");
                line =line.replace("[ver:","");
                line =line.replace("]","");
                line =line.contains("offset:")?"":line;
                line =line.replace("url","歌词来源");
                line =line.replace("null","");
                lrc_word+=line+"\n";
            }
            return lrc_word;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
        auto_Control();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int progress;
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                progress =voice_seekbar.getProgress();
                if(progress!=0)
                {
                    voice_seekbar.setProgress(progress-1);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                progress =voice_seekbar.getProgress();
                if(progress!=voice_seekbar.getMax())
                {
                    voice_seekbar.setProgress(progress+1);
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                super.onKeyDown(keyCode, event);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(asyncDownload!=null&&!asyncDownload.isCancelled())
        {
            asyncDownload.cancel(true);
        }
    }
}
