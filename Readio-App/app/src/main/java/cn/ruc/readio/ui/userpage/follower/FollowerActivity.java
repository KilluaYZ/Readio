package cn.ruc.readio.ui.userpage.follower;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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
import cn.ruc.readio.ui.userpage.subscribe.SubscriberActivity;
import cn.ruc.readio.ui.userpage.subscribe.SubscriberAdapter;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FollowerActivity extends AppCompatActivity {

    private List<User> FollowerList = new ArrayList<>();

    private RecyclerView recyclerView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follower);
        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        refreshData();
        RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        recyclerView = (RecyclerView) findViewById(R.id.followerRecyclerView);
        recyclerView.setLayoutManager(layoutManager);
        FollowerAdapter adapter = new FollowerAdapter(this, FollowerList);
        recyclerView.setAdapter(adapter);
        ImageView follower_activity_exit = (ImageView) findViewById(R.id.follower_activity_exit);
        follower_activity_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    public void refreshData(){
        if(this != null){
            HttpUtil.getRequestWithTokenAsyn(this, "/user/subscribe/get/follower", new ArrayList<>(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Tools.my_toast(FollowerActivity.this,"请求异常，加载不出来");
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
                                FollowerList.add(user);
                            }
                            if(recyclerView != null && FollowerActivity.this != null){
                                // 如果recycleView不为null，则先更新一点数据
                                FollowerActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        recyclerView.getAdapter().notifyDataSetChanged();
                                    }
                                });
                            }
                            for(int i = 0;i < FollowerList.size();++i){
                                int finalI = i;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(FollowerActivity.this != null){
                                            User user = FollowerList.get(finalI);
                                            try {
                                                user.setAvator(Tools.getImageBitmapSyn(FollowerActivity.this, user.getAvaID()));
                                                if(recyclerView != null && FollowerActivity.this != null){
                                                    FollowerActivity.this.runOnUiThread(new Runnable() {
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
                            Tools.my_toast(FollowerActivity.this, msg);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            });
        }
    }
}