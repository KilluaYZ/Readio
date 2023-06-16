package cn.ruc.readio.bookReadActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cn.ruc.readio.R;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class writeCommentActivity extends AppCompatActivity {

    private int BookID;
    private EditText edit_comment;
    private TextView word_count;
    private int num=0,number=0;
    private String comment_content,response_code,response_msg=" ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_comment);

        /*调整状态栏为透明*/
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        /*接受传递的消息*/
        Intent intent = getIntent();

        /*获取内容*/
        BookID = intent.getIntExtra("BookID",0);

        /*设置返回按钮*/
        ImageButton re_button = findViewById(R.id.return_button);
        re_button.setOnClickListener(v -> finish());

        /*写评论部分*/
        edit_comment = findViewById(R.id.edit_comment);
        edit_comment.setOnClickListener(v -> edit_comment.requestFocus());

        /*设置限制字数显示*/
        word_count=findViewById(R.id.word_count);
        edit_comment.addTextChangedListener(new TextWatcher() {
            private CharSequence temp;
            private int selectionStart;
            private int selectionEnd;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                temp = s;
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @SuppressLint("SetTextI18n")
            public void afterTextChanged(Editable s) {
                if (s.length() > 500) {
                    edit_comment.setText(s.toString().substring(0, 500)); //设置EditText只显示前面500位字符
                    edit_comment.setSelection(500);//让光标移至末端
                    Toast.makeText(writeCommentActivity.this, "输入字数已达上限", Toast.LENGTH_SHORT).show();
                } else {
                    number = num + s.length();
                    word_count.setText(number + "/ 500");
                    selectionStart = edit_comment.getSelectionStart();
                    selectionEnd = edit_comment.getSelectionEnd();
                    if (temp.length() < num) {
                        int tempSelection = selectionStart;
                        edit_comment.setText(s);
                        edit_comment.setSelection(tempSelection);//设置光标在最后
                    }
                }

            }
        });

        /*设置发送评论按钮*/
        ImageButton send_button=findViewById(R.id.send_button);
        send_button.setOnClickListener(v -> {
            comment_content=edit_comment.getText().toString();
            if(comment_content.length()==0){
                Tools.my_toast(writeCommentActivity.this,"你还没写东西呐！\n请输入些什么再发布吧~");
            }else{
                try {
                    sendcomment();
                    Tools.my_toast(writeCommentActivity.this,"发表评论成功");
                    edit_comment.setText("");
                } catch (IOException e) {
                    //Tools.my_toast(writeCommentActivity.this,"加载出错啦！");
                    Toast.makeText(writeCommentActivity.this,"发表评论失败",Toast.LENGTH_SHORT).show();
                    Log.d("response_msg", response_msg);
                }
            }
        });

        /*星星评分*/
        RatingBar describe_score=findViewById(R.id.RatingBar);
        TextView describe_tip=findViewById(R.id.describe_tip);
        describe_score.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                String score = String.valueOf(rating);
                String scoreState1 = fun_getScoreState(score);
                describe_tip.setText(scoreState1);
            }
        });


    }
    public void sendcomment() throws IOException {
        JSONObject jsonString=new JSONObject();
        try {
            jsonString.put("bookId",BookID);
            jsonString.put("content",comment_content);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        //String json="{\"bookId\":"+BookID+",\"content\":"+comment_content+"}";
        HttpUtil.postRequestWithTokenJsonAsyn(writeCommentActivity.this ,"/app/book/"+BookID+"/comments/add", jsonString.toString(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mtoast();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    response_code = jsonObject.getString("code");
                    response_msg=jsonObject.optString("msg","null");
                } catch (JSONException e) {
                    Tools.my_toast(writeCommentActivity.this,"加载出错啦！");
                }
            }
        });
    }
    private void mtoast(){
        runOnUiThread(() -> Toast.makeText(writeCommentActivity.this, "请求异常，加载不出来",Toast.LENGTH_LONG).show());
    }

    private String fun_getScoreState(String score) {
        String rating = "";
        switch (score) {
            case "0.5":
                rating = "差";
                break;
            case "1.0":
                rating = "差";
                break;
            case "1.5":
                rating = "较差";
                break;
            case "2.0":
                rating = "较差";
                break;
            case "2.5":
                rating = "一般";
                break;
            case "3.0":
                rating = "一般";
                break;
            case "3.5":
                rating = "好";
                break;
            case "4.0":
                rating = "好";
                break;
            case "4.5":
                rating = "极好";
                break;
            case "5.0":
                rating = "极好";
                break;
        }
        return rating;
    }
}