package cn.ruc.readio.worksActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import cn.ruc.readio.R;
import cn.ruc.readio.databinding.ActivityReadWorksBinding;
import cn.ruc.readio.databinding.FragmentUserpageBinding;
import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class readWorksActivity extends AppCompatActivity {
    ImageView ava = null;
    int like_clicked_times = 0;
    int is_liked = 0;
    int is_collect = 0;
    int collect_clicked_times = 0;
    public List<PieceComments> comment_list = new ArrayList<>();
    private mBottomSheetDialog bottomSheetDialog;
    private BottomSheetBehavior bottomSheetBehavior;
    private ActivityReadWorksBinding binding;
    private String pieceId;

    public RecyclerView recyclerView;
    public pieceCommentAdapter mainAdapter;

    public EditText fragment_comment_bar_editText;

    public ImageView fragment_comment_reply_bar_btn;
    private ImageView fragment_comment_cancel_reply_btn;

    private Boolean isReply = false;

    private User user;

    private String workId;
    private int replyCommentId = -1;
    private ClipData mClipData;
    private ClipboardManager mClipboardManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityReadWorksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        Intent intent = getIntent();
        workId = intent.getStringExtra("extra_data");
        TextView follow_button = (TextView) findViewById(R.id.followAuthorButton);
        EditText writeComment_button = (EditText) findViewById(R.id.writeComment);
        ImageView sendComment_button = (ImageView) findViewById(R.id.sendPieceCommentButton);
        ImageView like_button = (ImageView) findViewById(R.id.likePieceButton);
        ImageView collect_button = (ImageView) findViewById(R.id.collectPieceButton);
        ImageView read_comment_button = (ImageView) findViewById(R.id.commentZoneButton);
        TextView read_content = (TextView) findViewById(R.id.readWorkContentText);
        TextView read_title = (TextView) findViewById(R.id.workTitleText);
        TextView readSerialName = (TextView) findViewById(R.id.readSerialText);
        TextView userName = (TextView) findViewById(R.id.readUserNameText);
        ImageView exitRead_button = (ImageView) findViewById(R.id.exitRead);
        TextView updateTimeTextView = (TextView) findViewById(R.id.updateTimeTextView);

        read_content.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                String workContentDup = (String) read_content.getText();
                mClipData = ClipData.newPlainText("Simple text",workContentDup);
                mClipboardManager.setPrimaryClip(mClipData);
                Toast.makeText(view.getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        ArrayList<Pair<String,String>> queryParam = new ArrayList<>();
        queryParam.add(new Pair<>("piecesId",workId));
        HttpUtil.getRequestWithTokenAsyn(readWorksActivity.this,"/works/getPiecesDetail", queryParam, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mtoast("请求异常，加载不出来");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.code() == 200){
                        String s = response.body().string();
                        JSONObject jsonObject = new JSONObject(s);
                        JSONObject data = jsonObject.getJSONObject("data");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    pieceId =  data.getString("piecesId");
                                    read_content.setText("\n"+data.getString("content"));
                                    read_title.setText("\n"+data.getString("title"));
                                    readSerialName.setText("合集："+data.getJSONObject("series").getString("seriesName")+" ");
                                    user = new User();
                                    JSONObject userObj = data.getJSONObject("user");
                                    user.fromJSONObject(userObj);
                                    userName.setText(user.getUserName());
                                    String avaId = user.getAvaID();
                                    updateTimeTextView.setText(data.getString("updateTime"));
                                    if(data.getInt("isLiked")==1)
                                    {
                                        like_button.setImageResource(R.drawable.liked);
                                        is_liked = 1;
                                    }
                                    else {
                                        like_button.setImageResource(R.drawable.like);
                                    }
                                    if(data.getInt("isCollected")==1)
                                    {
                                        collect_button.setImageResource(R.drawable.collected);
                                    }
                                    else {
                                        collect_button.setImageResource(R.drawable.collect);
                                    }
                                    Tools.getImageBitmapAsyn(avaId,binding.authorAvator,readWorksActivity.this);
                                    if(user.getSubscribed()){
                                        follow_button.setVisibility(View.GONE);
                                    }
                                    //从server获取评论
                                    JSONArray commentArray = data.getJSONArray("comments");
                                    for(int i = 0;i < commentArray.length();++i){
                                        JSONObject commentObj = commentArray.getJSONObject(i);
                                        PieceComments pieceComments = new PieceComments();
                                        pieceComments.fromJsonObj(commentObj);
                                        comment_list.add(pieceComments);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                } catch (IOException | ParseException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }else{
                        mtoast("请求出错");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        exitRead_button.setOnClickListener(view -> finish());
        /*
        判断用户是否已关注太太，如果已关注，follow_button要setGone
         */
        follow_button.setOnClickListener(view -> {
//            Toast.makeText(readWorksActivity.this, "成功关注太太", Toast.LENGTH_SHORT).show();
            /*
            传至服务器，更新数据库
             */
            ArrayList<Pair<String, String>> userSubscribeAddQueryParam = new ArrayList<>();
            userSubscribeAddQueryParam.add(Pair.create("userId", user.getUserId()));
            Log.d("hahaha", "userId = "+user.getUserId());
            HttpUtil.getRequestWithTokenAsyn(readWorksActivity.this,"/user/subscribe/add", userSubscribeAddQueryParam, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Tools.my_toast(readWorksActivity.this, "关注失败，请检查网络连接");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(response.code() == 200){
                        Tools.my_toast(readWorksActivity.this, "成功关注太太！");
                        readWorksActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //传服务器结束
                                follow_button.setVisibility(View.GONE);
                            }
                        });
                    }else{
                        try {
                            String msg = new JSONObject(response.body().string()).getString("msg");
                            Tools.my_toast(readWorksActivity.this, msg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });


        });

        sendComment_button.setOnClickListener(view -> {
            if(TextUtils.isEmpty(writeComment_button.getText())){
                Toast.makeText(readWorksActivity.this, "还没有编写评论哦", Toast.LENGTH_SHORT).show();
            }else{
                //正常发送模式
                String content = writeComment_button.getText().toString();
                if(content.length()==0){
                    mtoast("请输入点什么再发送吧！");
                }else{
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("piecesId", workId);
                        jsonObject.put("content", content);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    HttpUtil.postRequestWithTokenJsonAsyn(readWorksActivity.this, "/works/pieces/comments/add", jsonObject.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Tools.my_toast(readWorksActivity.this, "发送失败，请检查网络连接");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            refreshCommentData();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Tools.my_toast(readWorksActivity.this,"评论发送成功！");
                                    writeComment_button.setText("");
                                }
                            });
                        }
                    });
                }
            }
        });

        like_button.setOnClickListener(view -> {
            like_clicked_times++;
            if(is_liked == 1){
                is_liked = 0;
                like_button.setImageResource(R.drawable.like);
                ArrayList<Pair<String, String>> pieceid = new ArrayList<>();
                pieceid.add(Pair.create("piecesId",pieceId));
                HttpUtil.getRequestWithTokenAsyn(readWorksActivity.this, "/works/pieces/like/del", pieceid, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Tools.my_toast(readWorksActivity.this,"取消点赞失败，请检查网络");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                    }
                });
            }
            else{
                is_liked = 1;
                like_button.setImageResource(R.drawable.liked);
                ArrayList<Pair<String, String>> pieceid = new ArrayList<>();
                pieceid.add(Pair.create("piecesId",pieceId));
                HttpUtil.getRequestWithTokenAsyn(readWorksActivity.this, "/works/pieces/like/add", pieceid, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Tools.my_toast(readWorksActivity.this,"取消点赞失败，请检查网络");
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                    }
                });
            }
        });

        collect_button.setOnClickListener(view -> {
            collect_clicked_times++;
            if(collect_clicked_times % 2 == 0){
                collect_button.setImageResource(R.drawable.collect);}
            else{
                collect_button.setImageResource(R.drawable.collected);
            }
        });

        read_comment_button.setOnClickListener(view -> {
//            for(int i = 0; i < 10; i++ ){
//                User user = new User("呆头鹅","2020201694@ruc.edu.cn","123456");
//            PieceComments commenti = new PieceComments("好棒",12,user);
//            comment_list.add(commenti);
//            }
            bottomsheet();
        });
        collect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Pair<String, String>> pieceid = new ArrayList<>();
                pieceid.add(Pair.create("piecesId",pieceId));
                if(is_collect==1)
                {
                    is_collect=0;
                    collect_button.setImageResource(R.drawable.collect);
                HttpUtil.getRequestWithTokenAsyn(readWorksActivity.this, "/works/pieces/collect/del", pieceid, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Tools.my_toast(readWorksActivity.this,"取消点赞失败，请检查网络");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                    }
                });}
                else{
                    is_collect = 1;
                    collect_button.setImageResource(R.drawable.collected);
                    HttpUtil.getRequestWithTokenAsyn(readWorksActivity.this, "/works/pieces/collect/add", pieceid, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Tools.my_toast(readWorksActivity.this,"取消点赞失败，请检查网络");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                        }
                    });
                }

            }
        });


    }

    private void bottomsheet(){
            if (bottomSheetDialog == null) {
                //创建布局
                View view = LayoutInflater.from(this).inflate(R.layout.work_comment_bottomsheet, null, false);
                bottomSheetDialog = new mBottomSheetDialog(this, R.style.work_comment_bottomsheet);
                recyclerView = view.findViewById(R.id.dialog_recycleView);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                mainAdapter = new pieceCommentAdapter(readWorksActivity.this, comment_list);
                recyclerView.setAdapter(mainAdapter);

                fragment_comment_bar_editText = (EditText) view.findViewById(R.id.fragment_comment_bar_editText);
                fragment_comment_reply_bar_btn = (ImageView) view.findViewById(R.id.fragment_comment_reply_bar_btn);
                fragment_comment_cancel_reply_btn = (ImageView) view.findViewById(R.id.fragment_comment_cancel_reply_btn);
                fragment_comment_reply_bar_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            onCLickedCommentBtn();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                fragment_comment_cancel_reply_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        endReplyMode();
                    }
                });

                //设置点击dialog外部不消失
                bottomSheetDialog.setCanceledOnTouchOutside(true);
                //核心代码 解决了无法去除遮罩问题
                bottomSheetDialog.getWindow().setDimAmount(0f);
                //设置布局
                bottomSheetDialog.setContentView(view);
                //用户行为
                bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
                //dialog的高度
                bottomSheetBehavior.setPeekHeight(getWindowHeight());
            }
            //展示
            bottomSheetDialog.show();

            //重新用户的滑动状态
                bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View view, int newState) {
                    //监听BottomSheet状态的改变
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        bottomSheetDialog.dismiss();
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }

                @Override
                public void onSlide(@NonNull View view, float v) {
                    //监听拖拽中的回调，根据slideOffset可以做一些动画
                }
            });

        }
    private int getWindowHeight() {
        Resources res = this.getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        int heightPixels = displayMetrics.heightPixels;
        //设置弹窗高度为屏幕高度的3/4
        return heightPixels - heightPixels / 4;
    }

    public void refreshCommentData(){
        ArrayList<Pair<String,String>> queryParam = new ArrayList<>();
        queryParam.add(new Pair<>("piecesId",workId));
        HttpUtil.getRequestWithTokenAsyn(readWorksActivity.this, "/works/getPiecesDetail", queryParam, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Tools.my_toast(readWorksActivity.this, "刷新数据失败，请检查网络连接");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200){
                    try {
                        comment_list.clear();
                        JSONObject jsonObject = new JSONObject(response.body().string()).getJSONObject("data");
                        //从server获取评论
                        JSONArray commentArray = jsonObject.getJSONArray("comments");
                        for(int i = 0;i < commentArray.length();++i){
                            JSONObject commentObj = commentArray.getJSONObject(i);
                            PieceComments pieceComments = new PieceComments();
                            pieceComments.fromJsonObj(commentObj);
                            comment_list.add(pieceComments);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recyclerView.getAdapter().notifyDataSetChanged();
                            }
                        });
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }else{

                }
            }
        });
    }

    private void mtoast(String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(readWorksActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }

    public void startReplyMode(PieceComments comment){
        User toUser = comment.getUser();
        replyCommentId = comment.getCommentId();
        isReply = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment_comment_bar_editText.setHint("@"+toUser.getUserName());
                fragment_comment_cancel_reply_btn.setVisibility(View.VISIBLE);
            }
        });
    }

    public void endReplyMode(){
        replyCommentId = -1;
        isReply = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment_comment_bar_editText.setHint("评论一下吧~");
                fragment_comment_cancel_reply_btn.setVisibility(View.GONE);
            }
        });
    }

    public void onCLickedCommentBtn() throws JSONException {
        if(isReply && replyCommentId != -1){
            //回复模式
            int commentId = replyCommentId;
            String content = fragment_comment_bar_editText.getText().toString();
            if(content.length()!=0) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("commentId", commentId);
                jsonObject.put("content", content);
                HttpUtil.postRequestWithTokenJsonAsyn(readWorksActivity.this, "/works/pieces/comments/reply", jsonObject.toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Tools.my_toast(readWorksActivity.this, "发送失败，请检查网络连接");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Tools.my_toast(readWorksActivity.this, "发送成功");
                        refreshCommentData();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fragment_comment_bar_editText.setText("");
                            }
                        });
                        endReplyMode();
                    }
                });
            }else{
                Tools.my_toast(readWorksActivity.this,"您还没有输入内容");
            }

        }else{
            //正常发送模式
            String content = fragment_comment_bar_editText.getText().toString();
            if(content.length() != 0) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("piecesId", workId);
                jsonObject.put("content", content);
                HttpUtil.postRequestWithTokenJsonAsyn(readWorksActivity.this, "/works/pieces/comments/add", jsonObject.toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Tools.my_toast(readWorksActivity.this, "发送失败，请检查网络连接");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Tools.my_toast(readWorksActivity.this, "发送成功");
                        refreshCommentData();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                fragment_comment_bar_editText.setText("");
                            }
                        });
                        endReplyMode();
                    }
                });
            }
            else{
                Tools.my_toast(readWorksActivity.this,"您还没有输入内容");
            }
        }
    }

}

