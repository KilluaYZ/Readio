package cn.ruc.readio.ui.userpage.subscribe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import cn.ruc.readio.R;
import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SubscriberActivity extends AppCompatActivity {
    private List<User> SubscribeList = new ArrayList<>();
    private RecyclerView recyclerView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriber);
        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        refreshData();
        RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        recyclerView = (RecyclerView) findViewById(R.id.subscriberRecyclerView);
        recyclerView.setLayoutManager(layoutManager);
        SubscriberAdapter adapter = new SubscriberAdapter(this, SubscribeList);
        recyclerView.setAdapter(adapter);

        ImageView subscribe_activity_exit = (ImageView) findViewById(R.id.subscribe_activity_exit);
        subscribe_activity_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void refreshData(){
        if(this != null){
            HttpUtil.getRequestWithTokenAsyn(this, "/user/subscribe/get/author", new ArrayList<>(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Tools.my_toast(SubscriberActivity.this,"请求异常，加载不出来");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(response.code() == 200){
                        try {
                            JSONArray subscribeJsonArray = new JSONObject(response.body().string()).getJSONArray("data");
                            for(int i = 0;i < subscribeJsonArray.length();++i){
                                JSONObject userObj = subscribeJsonArray.getJSONObject(i);
                                User user = new User();
                                user.fromJSONObject(userObj);
                                SubscribeList.add(user);
                            }

                            if(recyclerView != null && SubscriberActivity.this != null){
                                // 如果recycleView不为null，则先更新一点数据
                                SubscriberActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        recyclerView.getAdapter().notifyDataSetChanged();
                                    }
                                });
                            }
                            for(int i = 0;i < SubscribeList.size();++i){
                                int finalI = i;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(SubscriberActivity.this != null){
                                            User user = SubscribeList.get(finalI);
                                            try {
                                                user.setAvator(Tools.getImageBitmapSyn(SubscriberActivity.this, user.getAvaID()));
                                                if(recyclerView != null && SubscriberActivity.this != null){
                                                    SubscriberActivity.this.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            recyclerView.getAdapter().notifyDataSetChanged();
                                                        }
                                                    });
                                                }
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            } catch (JSONException e) {
                                                throw new RuntimeException(e);
                                            } catch (ParseException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                    }
                                }).start();
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    }else{
                        try {
                            String msg = new JSONObject(response.body().string()).getString("msg");
                            Tools.my_toast(SubscriberActivity.this, msg);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            });
        }
    }
}