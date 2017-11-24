package com.atguigu.moshang_listeners;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;

/**
 * Created by Administrator on 2017/11/17 0017.
 */

public class LrcFileDownLoad {
    //歌词api
    public static final String LRC_SEARCH_URL ="http://geci.me/api/lyric/";
    //设置请求超时15秒钟
    private static final int REQUEST_TIMEOUT =15*1000;
    //设置等待数据超时时间15秒钟
    private static final int SO_TIMEOUT =15*1000;

    public static String getSongLRCUrl(String path,String songName,String songArtist) throws Exception
    {
        String url =null;
        String str_json =null;
        //歌词为空时返回null
        if(songName ==null)
        {
            return null;
        }
        //编码转换
        String name = URLEncoder.encode(songName,"UTF-8");
        String artist =URLEncoder.encode(songArtist,"UTF-8");
        str_json =getHtmlCode(path+name+"/"+artist);

        //超时以及其他异常返回null
        if(str_json==null)
        {
            return null;
        }
        JSONObject jsonObject =new JSONObject(str_json);
        int count =jsonObject.getInt("count");
        //没有歌词时返回null
        if(count==0)
        {
            str_json =getHtmlCode(path+name);
            //超时以及其他异常返回null
            if(str_json==null)
            {
                return null;
            }
            JSONObject jsonObject1 =new JSONObject(str_json);
            int count1 =jsonObject1.getInt("count");
            if(count1==0)
            {
                return null;
            }
            //获取得到歌词url列表，这里只是第一个歌词的url
            JSONArray jsonArray1 =jsonObject1.getJSONArray("result");
            JSONObject item1 =jsonArray1.getJSONObject(0);

            url =item1.getString("lrc");
            return url;
        }

        //获取得到歌词url列表，这里只是第一个歌词的url
        JSONArray jsonArray =jsonObject.getJSONArray("result");
        JSONObject item =jsonArray.getJSONObject(0);

        url =item.getString("lrc");
        return url;

    }

    //该方法用于设置超时时间
    public static HttpClient getHttpClient()
    {
        BasicHttpParams httpParams =new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams,REQUEST_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams,SO_TIMEOUT);
        HttpClient client =new DefaultHttpClient(httpParams);
        return client;
    }

    //获取网页源码
    public static String getHtmlCode(String path) {

        String result =null;
        try {
            HttpClient httpclient =getHttpClient();
            HttpGet get =new HttpGet(path);
            HttpResponse response =httpclient.execute(get);

            if(response.getStatusLine().getStatusCode()== HttpStatus.SC_OK)
            {
                HttpEntity entity =response.getEntity();
                BufferedReader br =new BufferedReader(new InputStreamReader(entity.getContent(),"utf-8"));
                String line ;
                result ="";
                while ((line=br.readLine())!=null)
                {
                    result += line+"\n";
                }
            }

        } catch (ConnectTimeoutException e) {
            e.printStackTrace();
            return null;
        }catch(SocketTimeoutException e)
        {
            e.printStackTrace();
            return  null;
        }catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
        return result;
    }


}
