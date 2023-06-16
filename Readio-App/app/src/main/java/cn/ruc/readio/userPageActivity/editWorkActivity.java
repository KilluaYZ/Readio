package cn.ruc.readio.userPageActivity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import cn.ruc.readio.R;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class editWorkActivity extends AppCompatActivity {
    static public editWorkActivity editworkActivity;
    ArrayList<String> tagList = new ArrayList<>();
    ArrayList<String> tagIdList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_edit_work);
        editworkActivity = this;
        newWorksActivity.new_workActivity.finish();
        Intent intent = getIntent();

        String seriesName = intent.getStringExtra("seriesName");
        String seriesId = intent.getStringExtra("seriesId");
        String workName = intent.getStringExtra("workName");
        Log.d("hello",seriesName);
        TextView publish_button = (TextView) findViewById(R.id.publishButton);
        TextView exitEdit_button = (TextView) findViewById(R.id.editExitButton);
        TextView saveDraft_button = (TextView) findViewById(R.id.saveDraftButton);
        TextView addTag_button = (TextView) findViewById(R.id.addTagButton);
        Activity thisact = this;
        publish_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editContent = (EditText) findViewById(R.id.editPiece);
                if(!TextUtils.isEmpty(editContent.getText())){
                    try {
                        publishPiece(workName,toString().valueOf(editContent.getText()), seriesId, seriesName,"1");
                    } catch (JSONException e) {
                        Tools.my_toast(thisact,"出错了，发表失败");
                    }

                    Toast.makeText(editWorkActivity.this, "您编辑的内容已发布（⌯'▾'⌯）", Toast.LENGTH_SHORT).show();
                    finish();
                }
                else{
                    Toast.makeText(editWorkActivity.this, "所以你写了什么？(´◔ ‸◔`)", Toast.LENGTH_SHORT).show();
                }
            }
        });
        exitEdit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editContent = (EditText) findViewById(R.id.editPiece);
                if(!TextUtils.isEmpty(editContent.getText())){
                    Intent intent = new Intent(editWorkActivity.this, exitEditActivity.class);
                    startActivity(intent);
                }else{
                    finish();
                }
            }
        });
        saveDraft_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                上传至服务器，更新数据库
                 */
                EditText editContent = (EditText) findViewById(R.id.editPiece);
                if(!TextUtils.isEmpty(editContent.getText())){
                    try {
                        publishPiece(workName,editContent.getText().toString(), seriesId, seriesName,"0");
                    } catch (JSONException e) {
                        Tools.my_toast(thisact,"出错了，保存失败");
                    }

                    Toast.makeText(editWorkActivity.this, "您编辑的内容已保存至草稿箱（⌯'▾'⌯）", Toast.LENGTH_SHORT).show();
                    finish();}
                else{
                    Toast.makeText(editWorkActivity.this, "所以你写了什么？(´◔ ‸◔`)", Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        });
        addTag_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(editWorkActivity.this,addTagActivity.class);
                startActivityForResult(intent,1);
            }
        });

    }
    public void publishPiece(String workName, String content, String seriesId, String seriesName, String status) throws JSONException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("piecesTitle", workName);
        jsonObject.put("content", content);
        jsonObject.put("seriesId", seriesId);
        jsonObject.put("seriesName", seriesName);
        jsonObject.put("status", status);
        jsonObject.put("tagNameList", new JSONArray(tagList));
        jsonObject.put("tagIdList", new JSONArray(tagIdList));
        String json = jsonObject.toString();

        HttpUtil.postRequestWithTokenJsonAsyn(this,"/works/addPieces",  json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(editWorkActivity.this, "发布失败，请重试", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == 3) {
            String tagName = data.getStringExtra("tagName");
            String tagId = data.getStringExtra("tagId");
            Log.d("hello", tagName);
            if (tagList.contains(tagName)) {
            } else {
                tagList.add(tagName);
                tagIdList.add(tagId);
            }
        }
    }
}
