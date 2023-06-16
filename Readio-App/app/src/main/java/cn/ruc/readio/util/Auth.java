package cn.ruc.readio.util;

import static cn.ruc.readio.util.HttpUtil.getRequestWithTokenAsyn;
import static cn.ruc.readio.util.HttpUtil.postRequestJsonAsyn;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import cn.ruc.readio.ui.userpage.login.LoginActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class Auth {

    public static class Token{

        private final String storeName = "token";

        private String token = null;

        private Activity activity = null;

        int needJump = 1;

        public boolean isEmpty(){
            read();
            if(token.length() > 0){
                return false;
            }
            return true;
        }

        public Token(Activity activity){
            this.activity = activity;
        }

        public Token(Activity activity, int needJump){
            this.activity = activity;
            this.needJump = needJump;
        }
        public String getToken() {
            read();
//            if(token == "" && needJump == 1){
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try{
//                            activity.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Toast.makeText(activity, "未登录，正在跳转登录页面！", Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                            Thread.sleep(500);
//                            Intent intent = new Intent(activity, LoginActivity.class);
//                            activity.startActivity(intent);
//                        }
//                        catch (InterruptedException e){
//                            e.printStackTrace();
//                        }
//                    }
//                }).run();
//            }
            return token;
        }

        public void setToken(String token) {
            this.token = token;
            write();
        }

        public void write(){
            SharedPreferences sharedPreferences = activity.getSharedPreferences(storeName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("token", token);
            editor.commit();
        }

        public void read(){
            SharedPreferences sharedPreferences = activity.getSharedPreferences(storeName, Context.MODE_PRIVATE);
            token = sharedPreferences.getString("token","");
        }

        public void clear(){
            SharedPreferences sharedPreferences = activity.getSharedPreferences(storeName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.commit();
        }
    }

    public void login(LoginActivity activity, String phoneNumber, String passWord) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("phoneNumber", phoneNumber);
        requestBody.put("passWord", passWord);
        postRequestJsonAsyn("/app/auth/login", requestBody.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "请检查网络连接", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("hahahaha", "response code "+String.valueOf(response.code()));
                int responseCode = response.code();
                if(responseCode == 200){
                    //成功拿到数据
                    try {
                        Log.d("hahaha", "进入login, 成功拿到token");
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        String tokenString = jsonObject.getJSONObject("data").getString("token");
                        Auth.Token token = new Auth.Token(activity);
                        token.setToken(tokenString);
                        Log.d("hhhhh", "成功拿到token："+token.getToken());
                        activity.finish(); //只有login activity才会调用该函数，所以我们可以暴力地直接结束该activity
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }else if(responseCode == 400){
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        String msg = jsonObject.getString("msg");
                        Tools.my_toast(activity, msg);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        });
    }

    public void register(LoginActivity activity, String phoneNumber, String passWord) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("phoneNumber", phoneNumber);
        requestBody.put("passWord", passWord);
        postRequestJsonAsyn("/app/auth/register", requestBody.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Tools.my_toast(activity, "请检查网络连接");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200){
                    //成功拿到数据
                    activity.changeLayout();
                }else{
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        String msg = jsonObject.getString("msg");
                        Tools.my_toast(activity, msg);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        });
    }

    public void logout(Activity activity){
        getRequestWithTokenAsyn(activity,"/app/auth/logout", new ArrayList<>(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "请检查网络连接", Toast.LENGTH_SHORT);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200){
                    if(activity != null){
                        Tools.my_toast(activity,"退出登陆成功！");
                        Auth.Token token = new Auth.Token(activity);
                        token.clear();
                        activity.finish();
                    }

                }else{
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(activity, new JSONObject(response.body().string()).getString("msg"), Toast.LENGTH_SHORT);
                            } catch (JSONException e) {
                                Tools.my_toast(activity,"退出登陆失败");
                                e.printStackTrace();
                            } catch (IOException e) {
                                Tools.my_toast(activity,"退出登陆失败");
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }


}
