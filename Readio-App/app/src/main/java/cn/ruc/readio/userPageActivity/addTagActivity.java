package cn.ruc.readio.userPageActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.ruc.readio.R;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class addTagActivity extends Activity {

    final List<Pair<String,String>> Taglist = new ArrayList<>();
    final List<String> NameList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tag);

        final List<String> list = new ArrayList<>();
        getTagName();

        EditText tagName = findViewById(R.id.tagName);
        ListView tagList = findViewById(R.id.tagList);
        LinearLayout addTagTitleBar = (LinearLayout) findViewById(R.id.addTag_titleBar);
        ImageButton addTag = (ImageButton) findViewById(R.id.confirmTagButton);
        EditText myTag = (EditText) findViewById(R.id.tagName);
        tagAdapter adapter = new tagAdapter(this, NameList, new tagAdapter.onItemViewClickListener(){
            @Override
            public void onItemViewClick(String name) {
                tagName.setText(name);
                tagName.setSelection(name.length());
                tagList.setVisibility(View.GONE);
            }
        });
        tagList.setAdapter(adapter);
        tagList.setVisibility(View.GONE);

        tagName.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                tagList.setVisibility(View.VISIBLE);
                return false;
            }
        });

        tagName.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
//                SerialNameList.setVisibility(TextUtils.isEmpty(s.toString()) ? View.VISIBLE:View.GONE);
                tagList.setVisibility(View.GONE);
            }
        });

        addTagTitleBar.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                tagList.setVisibility(View.GONE);
                return false;
            }
        });

        addTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText tag_name = (EditText) findViewById(R.id.tagName);
                if(TextUtils.isEmpty(tag_name.getText())){
                    Toast.makeText(addTagActivity.this, "请输入tag", Toast.LENGTH_SHORT).show();
                }
                else {
                    /*
                    上传至服务器，跟新数据库
                     */
                    Intent intent = new Intent();
                    String tagName = String.valueOf(myTag.getText());
                    intent.putExtra("tagName",tagName);
                    if(NameList.contains(tagName)){
                        for(int i = 0; i < NameList.size(); i++)
                        {
                            if(Taglist.get(i).first.equals(tagName))
                            {
                                intent.putExtra("tagId",Taglist.get(i).second);
                            }
                        }
                    }
                    else{
                        intent.putExtra("tagId","");
                    }
                    intent.putExtra("origin","addTag");
                    setResult(3, intent);
                    finish();
                    Toast.makeText(addTagActivity.this, "成功更新tag", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void getTagName(){
        ArrayList<Pair<String , String>> queryParam =  new ArrayList<>();
        queryParam.add(Pair.create("pageSize","7"));
        queryParam.add(Pair.create("pageNum","1"));
        queryParam.add(Pair.create("sortMode","Hot"));
        HttpUtil.getRequestWithTokenAsyn(this,"/works/tag/get", queryParam,new Callback(){
            @Override
            public void onFailure(Call call, IOException e) {
                Tools.my_toast(addTagActivity.this,"请求失败");
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try{
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray data = jsonObject.getJSONArray("data");
                    for(int i = 0; i < data.length(); i++){
                        JSONObject datai = data.getJSONObject(i);
                        String namei = datai.getString("content");
                        String Idi = datai.getString("tagId");
                        Taglist.add(Pair.create(namei,Idi));
                        NameList.add(namei);
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }
}