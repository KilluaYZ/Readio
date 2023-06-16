package cn.ruc.readio.userPageActivity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
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

public class ReEditActivity extends AppCompatActivity {

    static public ReEditActivity ReEditActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_edit_work);
        ReEditActivity = this;
        Intent intent = getIntent();
        String piecesId = intent.getStringExtra("piecesId");
        String piecesContent = intent.getStringExtra("piecesContent");
        String status = intent.getStringExtra("status");
        TextView publish_button = (TextView) findViewById(R.id.publishButton);
        TextView exitEdit_button = (TextView) findViewById(R.id.editExitButton);
        TextView saveDraft_button = (TextView) findViewById(R.id.saveDraftButton);
        TextView addTag_button = (TextView) findViewById(R.id.addTagButton);
        EditText editContent = (EditText) findViewById(R.id.editPiece);
        editContent.setText(piecesContent);
        if(status.equals("0")) {
            publish_button.setText("存回草稿箱");
        }else{
            publish_button.setText("重新发布");
        }
        editContent.setText(piecesContent);
        Activity thisact = this;
        saveDraft_button.setVisibility(View.GONE);
        addTag_button.setVisibility(View.GONE);

        publish_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!TextUtils.isEmpty(editContent.getText())){
                    try {
                        updatePiece(piecesId,toString().valueOf(editContent.getText()));
                        Toast.makeText(ReEditActivity.this, "您修改的内容已重新发布（⌯'▾'⌯）", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        Tools.my_toast(thisact,"出错了，发表失败");
                    }
                    finish();
                }
                else{
                    Toast.makeText(ReEditActivity.this, "所以你写了什么？(´◔ ‸◔`)", Toast.LENGTH_SHORT).show();
                }
            }
        });
        exitEdit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editContent = (EditText) findViewById(R.id.editPiece);
                if(!TextUtils.isEmpty(editContent.getText())){
                    Intent intent = new Intent(ReEditActivity.this, exitEditActivity.class);
                    startActivity(intent);
                }else{
                    finish();
                }
            }
        });


    }
    public void updatePiece(String piecesId, String content) throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("piecesId", piecesId);
        jsonObject.put("content", content);
        String json = jsonObject.toString();

        HttpUtil.postRequestWithTokenJsonAsyn(this,"/works/updatePieces",  json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(ReEditActivity.this, "发布失败，请重试", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Tools.my_toast(ReEditActivity.this,"作品修改成功！");
            }
        });
    }
}