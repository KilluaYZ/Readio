package cn.ruc.readio.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.HttpUrl;
import okhttp3.Response;

public class HttpUtil {
    public static String BASE_URL = "killuayz.top"; //http://killuayz.top:5000  http://127.0.0.1:5000
    public static String BASE_SCHEME = "http";
    public static int BASE_PORT = 5000;

    public static int getServer(Activity activity)
    {
//        BASE_URL = readHttpHost(activity);
        if(BASE_URL == "killuayz.top"){
            return 0;
        }
        else{
            return 1;
        }
    }

    public static void setBaseUrl_Tencent(Activity activity)
    {
        BASE_URL = "killuayz.top";
        changeHttpHost(activity, BASE_URL);
    }
    public static void setBaseUrl_550w(Activity activity)
    {
        BASE_URL = "server.killuayz.top";
        changeHttpHost(activity, BASE_URL);
    }

    public static void changeHttpHost(Activity activity, String host){
        SharedPreferences sharedPreferences = activity.getSharedPreferences("SERVER_HOST", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("SERVER_HOST",host);
        editor.commit();
    }

    public static String readHttpHost(Activity activity){
        SharedPreferences sharedPreferences = activity.getSharedPreferences("SERVER_HOST", Context.MODE_PRIVATE);
        return sharedPreferences.getString("SERVER_HOST","");
    }

    /*
    不带Token的异步get请求
    @param String address 对应的api
    @param ArrayList<Pair<String,String>> queryParameter 查询参数
    @param Callback callback 回调函数，即向服务器请求成功或失败后调用的函数
    * */
    public static void getRequestAsyn(String address, ArrayList<Pair<String,String>> queryParameter, Callback callback){
        Log.d("HttpUtil","正在访问 "+BASE_URL + address);
        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        queryParameter.forEach((item) -> {
            url.addQueryParameter(item.first,item.second);
        });

        Request request = new Request.Builder()
                .url(url.build())
                .get()
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(callback);
    }
    /*
    不带Token的同步get请求
    @param String address 对应的api
    @param ArrayList<Pair<String,String>> queryParameter 查询参数
    @return Response okhttp3的Response类型，可以获取到服务器返回的状态码，数据等内容
    * */
    public static Response getRequestSyn(String address, ArrayList<Pair<String,String>> queryParameter) throws IOException {
        Log.d("HttpUtil","正在访问 "+BASE_URL + address);
        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        queryParameter.forEach((item) -> {
            url.addQueryParameter(item.first,item.second);
        });

        Request request = new Request.Builder()
                .url(url.build())
                .get()
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        return okHttpClient.newCall(request).execute();
    }

    /*
    带Token的异步get请求
    @param Activity activity 调用该函数的activity(如果是activity调用就传this，如果是fragment调用就传getActivity())
    @param String address 对应的api
    @param ArrayList<Pair<String,String>> queryParameter 查询参数
    @param Callback callback 回调函数，即向服务器请求成功或失败后调用的函数
    * */
    public static void getRequestWithTokenAsyn(Activity activity, String address, ArrayList<Pair<String,String>> queryParameter, Callback callback){
        Log.d("HttpUtil","正在访问 "+BASE_URL + address);
        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        queryParameter.forEach((item) -> {
            url.addQueryParameter(item.first,item.second);
        });

        Auth.Token token = new Auth.Token(activity);

        Request request = new Request.Builder()
                .url(url.build())
                .get()
                .addHeader("Authorization", token.getToken())
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void getRequestWithTokenAsyn(Activity activity, String address, ArrayList<Pair<String,String>> queryParameter, Callback callback, int needJump){
        Log.d("HttpUtil","正在访问 "+BASE_URL + address);
        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        queryParameter.forEach((item) -> {
            url.addQueryParameter(item.first,item.second);
        });

        Auth.Token token = new Auth.Token(activity, needJump);

        Request request = new Request.Builder()
                .url(url.build())
                .get()
                .addHeader("Authorization", token.getToken())
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
    带Token的同步get请求
    @param Activity activity 调用该函数的activity(如果是activity调用就传this，如果是fragment调用就传getActivity())
    @param String address 对应的api
    @param ArrayList<Pair<String,String>> queryParameter 查询参数
    @return Response okhttp3的Response类型，可以获取到服务器返回的状态码，数据等内容
    * */
    public static Response getRequestWithTokenSyn(Activity activity, String address, ArrayList<Pair<String,String>> queryParameter) throws IOException {
        Log.d("HttpUtil","正在访问 "+BASE_URL + address);
        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        queryParameter.forEach((item) -> {
            url.addQueryParameter(item.first,item.second);
        });

        Auth.Token token = new Auth.Token(activity);

        Request request = new Request.Builder()
                .url(url.build())
                .get()
                .addHeader("Authorization", token.getToken())
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        return okHttpClient.newCall(request).execute();
    }

    /*
    不带Token的异步post请求
    @param String address 对应的api
    @param RequestBody requestBody 报文体内容，类型是okhttp3的RequestBody类型
    @param Callback callback 回调函数，即向服务器请求成功或失败后调用的函数
    * */
    public static void postRequestAsyn(String address, RequestBody requestBody, Callback callback){

        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        Log.d("HttpUtil","postRequest正在访问 "+url.build().toString());
        Request request = new Request.Builder()
                .url(url.build())
                .post(requestBody)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
    不带Token的同步post请求
    @param String address 对应的api
    @param RequestBody requestBody 报文体内容，类型是okhttp3的RequestBody类型
    @return Response okhttp3的Response类型，可以获取到服务器返回的状态码，数据等内容
    * */
    public static Response postRequestSyn(String address, RequestBody requestBody) throws IOException {

        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        Log.d("HttpUtil","postRequest正在访问 "+url.build().toString());
        Request request = new Request.Builder()
                .url(url.build())
                .post(requestBody)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        return okHttpClient.newCall(request).execute();
    }

    /*
    带Token的异步post请求
    @param Activity activity 调用该函数的activity(如果是activity调用就传this，如果是fragment调用就传getActivity())
    @param String address 对应的api
    @param RequestBody requestBody 报文体内容，类型是okhttp3的RequestBody类型
    @param Callback callback 回调函数，即向服务器请求成功或失败后调用的函数
    * */
    public static void postRequestWithTokenAsyn(Activity activity, String  address, RequestBody requestBody, Callback callback){
        Log.d("HttpUtil","正在访问 "+BASE_URL + address);
        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        Auth.Token token = new Auth.Token(activity);

        Request request = new Request.Builder()
                .url(url.build())
                .post(requestBody)
                .addHeader("Authorization", token.getToken())
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
    带Token的同步post请求
    @param Activity activity 调用该函数的activity(如果是activity调用就传this，如果是fragment调用就传getActivity())
    @param String address 对应的api
    @param RequestBody requestBody 报文体内容，类型是okhttp3的RequestBody类型
    @return Response okhttp3的Response类型，可以获取到服务器返回的状态码，数据等内容
    * */
    public static Response postRequestWithTokenSyn(Activity activity, String address, RequestBody requestBody) throws IOException {
        Log.d("HttpUtil","正在访问 "+BASE_URL + address);
        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        Auth.Token token = new Auth.Token(activity);

        Request request = new Request.Builder()
                .url(url.build())
                .post(requestBody)
                .addHeader("Authorization", token.getToken())
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        return okHttpClient.newCall(request).execute();
    }

    /*
    不带Token的异步post请求
    @param String address 对应的api
    @param String jsonString Json字符串，作为报文体内容
    @param Callback callback 回调函数，即向服务器请求成功或失败后调用的函数
    * */
    public static void postRequestJsonAsyn(String address, String jsonString, Callback callback){
//        Log.d("HttpUtil","正在访问 "+BASE_URL + address + "\n body = "+jsonString);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString);

        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        Log.d("HttpUtil","postRequestJson正在访问 "+url.build().toString() + "\n body = "+jsonString);
        Request request = new Request.Builder()
                .url(url.build())
                .post(requestBody)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
    不带Token的同步post请求
    @param String address 对应的api
    @param String jsonString Json字符串，作为报文体内容
    @return Response okhttp3的Response类型，可以获取到服务器返回的状态码，数据等内容
    * */
    public static Response postRequestJsonSyn(String address, String jsonString) throws IOException {
//        Log.d("HttpUtil","正在访问 "+BASE_URL + address + "\n body = "+jsonString);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString);

        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        Log.d("HttpUtil","postRequestJson正在访问 "+url.build().toString() + "\n body = "+jsonString);
        Request request = new Request.Builder()
                .url(url.build())
                .post(requestBody)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        return okHttpClient.newCall(request).execute();
    }

    /*
    带Token的异步post请求
    @param Activity activity 调用该函数的activity(如果是activity调用就传this，如果是fragment调用就传getActivity())
    @param String address 对应的api
    @param String jsonString Json字符串，作为报文体内容
    @param Callback callback 回调函数，即向服务器请求成功或失败后调用的函数
    * */
    public static void postRequestWithTokenJsonAsyn(Activity activity, String address, String jsonString, Callback callback){
        Log.d("HttpUtil","正在访问 "+BASE_URL + address);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString);

        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        Auth.Token token = new Auth.Token(activity);

        Request request = new Request.Builder()
                .url(url.build())
                .post(requestBody)
                .addHeader("Authorization", token.getToken())
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
    带Token的同步post请求
    @param Activity activity 调用该函数的activity(如果是activity调用就传this，如果是fragment调用就传getActivity())
    @param String address 对应的api
    @param String jsonString Json字符串，作为报文体内容
    @return Response okhttp3的Response类型，可以获取到服务器返回的状态码，数据等内容
    * */
    public static Response postRequestWithTokenJsonSyn(Activity activity, String address, String jsonString, Callback callback) throws IOException {
        Log.d("HttpUtil","正在访问 "+BASE_URL + address);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString);

        HttpUrl.Builder url = new HttpUrl.Builder()
                .host(BASE_URL)
                .port(BASE_PORT)
                .scheme(BASE_SCHEME)
                .addPathSegment(address);

        Auth.Token token = new Auth.Token(activity);

        Request request = new Request.Builder()
                .url(url.build())
                .post(requestBody)
                .addHeader("Authorization", token.getToken())
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        return okHttpClient.newCall(request).execute();
    }


    public static void getAvaAsyn(String avaId, ImageView view, FragmentActivity avtivity){
        ArrayList<Pair<String,String>> queryParam = new ArrayList<>();
        queryParam.add(Pair.create("fileId", avaId));
        Log.d("avatorload","进入函数");
        HttpUtil.getRequestAsyn("/file/getFileBinaryById", queryParam, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(view.getContext(), "加载失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if(response.code() == 200){
                    Log.d("avatorload","hello");
                    byte[] picBytes = response.body().bytes();
                    Bitmap pic = BitmapFactory.decodeByteArray(picBytes,0,picBytes.length);
                    avtivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("avatorload","即将设置");
                            view.setImageBitmap(pic);
                            Log.d("avatorload","设置完毕");
                        }
                    });

                }else{
                    Log.d("avatorload","出问题啦");
                }

            }
        });
    }


    public static Bitmap getAvaSyn(String avaId){
        ArrayList<Pair<String,String>> queryParam = new ArrayList<>();
        queryParam.add(Pair.create("fileId", avaId));
        Bitmap pic = null;
        try{
            Response response =  HttpUtil.getRequestSyn("/file/getFileBinaryById", queryParam);
            byte[] picBytes = response.body().bytes();
            pic = BitmapFactory.decodeByteArray(picBytes,0,picBytes.length);

        }catch (IOException e){
            e.printStackTrace();
        }
        return pic;
    }
}
