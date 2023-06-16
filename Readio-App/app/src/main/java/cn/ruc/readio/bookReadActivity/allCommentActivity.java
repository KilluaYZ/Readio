package cn.ruc.readio.bookReadActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.ruc.readio.R;
import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import cn.ruc.readio.worksActivity.PieceComments;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class allCommentActivity extends AppCompatActivity {

    private List<PieceComments> comment_list =new ArrayList<>();
    private String BookName,Author;
    private int BookID,child_commentnum;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_comment);
        /*调整状态栏为透明色*/
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        /*接受传递的消息*/
        Intent intent = getIntent();

        /*获取内容*/
        BookName = intent.getStringExtra("BookName");
        Author = intent.getStringExtra("Author");
        BookID = intent.getIntExtra("BookID",0);

        refreshData();

        //if(comment_list==null) initdata();

        LinearLayoutManager m=new LinearLayoutManager(allCommentActivity.this);
        RecyclerView recycler_view=findViewById(R.id.comments_recyclerView);
        recycler_view.setLayoutManager(m);
        aBookCommentAdapter myAdapter = new aBookCommentAdapter(allCommentActivity.this,comment_list);
        myAdapter.getActivity(allCommentActivity.this);
        recycler_view.setAdapter(myAdapter);


    }

    public void initdata(){
        User user = new User("小美","20230522@ruc.edu.cn", "12345678");
        comment_list.add(new PieceComments("喜欢喜欢喜欢", 12, user));
        comment_list.add(new PieceComments("好看好看好看好看", 12, user));
        comment_list.add(new PieceComments("喜欢喜欢喜欢", 12, user));
        comment_list.add(new PieceComments("喜欢喜欢喜欢", 12, user));
        comment_list.add(new PieceComments("喜欢喜欢喜欢", 12, user));
        comment_list.add(new PieceComments("喜欢喜欢喜欢", 12, user));
        comment_list.add(new PieceComments("喜欢喜欢喜欢", 12, user));
        comment_list.add(new PieceComments("喜欢喜欢喜欢", 12, user));
        comment_list.add(new PieceComments("喜欢喜欢喜欢", 12, user));


    }
    public void refreshData(){
        HttpUtil.getRequestWithTokenAsyn(this,"/app/book/"+ BookID, new ArrayList<>(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mtoast();
            }
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    /*获取所有书籍信息*/
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONObject data = jsonObject.getJSONObject("data");

                    /*取出书籍评论*/
                    JSONObject book_comments=data.getJSONObject("comments");
                    int comment_size=book_comments.getInt("size");
                    JSONArray comments_list=book_comments.getJSONArray("data");
                    if(comment_size==0)
                    {
                        comment_list=null;
                    }
                    if(comment_size!=0){
                        for(int i = 0; i < comment_size; i++){
                            JSONObject comment_item = comments_list.getJSONObject(i);
                            User user = get_userinfo(comment_item.getString("userId"),i);
                            user.setAvaID(String.valueOf(R.drawable.juicy_orange_smile_icon));
                            PieceComments comment = new PieceComments(comment_item.getString("content"),comment_item.getInt("likes"),user);
                            comment.setBookId(String.valueOf(BookID));
                            comment.setCommentId(Integer.parseInt(comment_item.getString("commentId")));
                            comment.setLikesNum(comment_item.getInt("likes"));
                            comment.setDate(comment_item.getString("createTime"));
                            comment.setIf_liked(comment_item.getString("liked"));
                            int childcommentnum=get_childcommentnum(comment_item.getString("commentId"),i);
                            comment.setChildCommentNum(childcommentnum);
                            comment_list.add(comment);
                        }
                        runOnUiThread(() -> {
                            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.comments_recyclerView);
                            Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    User get_userinfo(String userId,int i){
        User user = new User();
        ArrayList<Pair<String,String>> queryParam = new ArrayList<>();
        queryParam.add(new Pair<>("userId",userId));
        HttpUtil.getRequestAsyn("/user/get", queryParam, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mtoast();
            }
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    /*获取该用户的所有信息*/
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONObject data = jsonObject.getJSONObject("data");

                    /*取出书籍评论*/
                    user.setUserName(data.getString("userName"));
                    if(data.optString("avator").equals("null"))
                    {
                        user.setAvaID(String.valueOf(R.drawable.juicy_orange_smile_icon));
                    } else{
                        user.setAvaID(data.getString("avator"));
                    }
                    user.setPhoneNumber(data.getString("phoneNumber"));
                    user.seteMail(data.getString("email"));
                    comment_list.get(i).setUser(user);
                    allCommentActivity.this.runOnUiThread(() -> {
                        RecyclerView recyclerView = findViewById(R.id.comments_recyclerView);
                        if(recyclerView!=null) {
                            Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                        }
                    });
                    new Thread(() -> {
                        Bitmap pic = null;
                        try {
                            pic = Tools.getImageBitmapSyn(allCommentActivity.this, comment_list.get(i).getUser().getAvaID());
                        } catch (IOException | JSONException | ParseException e) {
                            Tools.my_toast(allCommentActivity.this,"图片加载出错啦！");
                        }
                        comment_list.get(i).getUser().setAvator(pic);
                        Log.d("commentAdapter","需要更新");
                        allCommentActivity.this.runOnUiThread(() -> {
                            RecyclerView recyclerView = findViewById(R.id.comments_recyclerView);
                            if(recyclerView!=null) {
                                Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                            }
                        });
                    }).start();
                } catch (JSONException ex) {
                    Tools.my_toast(allCommentActivity.this,"用户加载失败");
                }
            }
        });
        return user;
    }
    private void mtoast(){
        runOnUiThread(() -> Toast.makeText(allCommentActivity.this, "请求异常，加载不出来",Toast.LENGTH_LONG).show());
    }

    int get_childcommentnum(String commentId,int i) {
        ///app/book/<int:book_id>/comment/<int:comment_id>
        HttpUtil.getRequestAsyn("app/book/"+BookID+"/comment/"+commentId, new ArrayList<>() ,new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mtoast();
            }
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    /*获取该评论的所有信息*/
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONObject data = jsonObject.getJSONObject("data");

                    /*取出子评论数量*/
                    child_commentnum =data.getInt("size");
                    comment_list.get(i).setChildCommentNum(child_commentnum);
                    allCommentActivity.this.runOnUiThread(() -> {
                        RecyclerView recyclerView = findViewById(R.id.comments_recyclerView);
                        if (recyclerView != null) {
                            Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                        }
                    });
                } catch (JSONException ex) {
                    Tools.my_toast(allCommentActivity.this, "子评论数量加载失败");
                }
            }
        });
        return child_commentnum;
    }
}