package cn.ruc.readio.userPageActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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

import cn.ruc.readio.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.ruc.readio.R;
import cn.ruc.readio.ui.userpage.serialNameAdapter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class newWorksActivity extends Activity {
    static public newWorksActivity new_workActivity;
    final List<Pair<String,String>> list = new ArrayList<>();
    final List<String> NameList = new ArrayList<>();
    private String seriesId = "";
    private String seriesName = "";
    private String workName = "";
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_works);
        new_workActivity = this;
        ImageButton goEditButton = (ImageButton)  findViewById(R.id.confirmNew_button);
        getSeriesTitle();
//        list.add("恶煞");
//        list.add("viko通讯录");
//        list.add("年下不叫哥，心思有点多");

        LinearLayout newWork_titleBar = findViewById(R.id.NewWork_titleBar);
        EditText SerialName = findViewById(R.id.serialName);
        EditText WorkName = findViewById(R.id.pieceName);
        ListView SerialNameList = findViewById(R.id.serialNameList);
        serialNameAdapter adapter = new serialNameAdapter(this, NameList, new serialNameAdapter.onItemViewClickListener(){
            @Override
            public void onItemViewClick(String name) {
                SerialName.setText(name);
                SerialName.setSelection(name.length());
                SerialNameList.setVisibility(View.GONE);
            }
        });
        SerialNameList.setAdapter(adapter);
        SerialNameList.setVisibility(View.GONE);

        SerialName.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                SerialNameList.setVisibility(View.VISIBLE);
                return false;
            }
        });

        SerialName.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
//                SerialNameList.setVisibility(TextUtils.isEmpty(s.toString()) ? View.VISIBLE:View.GONE);
                SerialNameList.setVisibility(View.GONE);
            }
        });

        newWork_titleBar.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                SerialNameList.setVisibility(View.GONE);
                return false;
            }
        });

        goEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText piece_name = (EditText) findViewById(R.id.pieceName);
                EditText serial_name = (EditText) findViewById(R.id.serialName);
                if(TextUtils.isEmpty(piece_name.getText()) || TextUtils.isEmpty(serial_name.getText())){
                    Toast.makeText(newWorksActivity.this, "请完善作品信息", Toast.LENGTH_SHORT).show();
                }
                else {
                Intent intent = new Intent(newWorksActivity.this,editWorkActivity.class);
                String workName = String.valueOf(WorkName.getText());
                String serialName = String.valueOf(SerialName.getText());
                intent.putExtra("seriesName",serialName);
                intent.putExtra("workName",workName);
                intent.putExtra("origin","newWork");
                Log.d("workName1",workName);
                if(NameList.contains(serialName)){
                    for(int i = 0; i < NameList.size(); i++){
                        if(list.get(i).first.equals(serialName)){
                            intent.putExtra("seriesId",list.get(i).second);
                        }
                    }
                }else{
                    intent.putExtra("seriesId","");
                }
                startActivity(intent);
                }
            }
        });
    }
    public void getSeriesTitle(){
        HttpUtil.getRequestWithTokenAsyn(this,"/works/getUserSeriesList",new ArrayList<>(),new Callback(){
            @Override
            public void onFailure(Call call, IOException e) {
                mtoast("请求异常");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try{
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray data = jsonObject.getJSONArray("data");
                    for(int i = 0; i < data.length(); i++){
                        JSONObject datai = data.getJSONObject(i);
                        String namei = datai.getString("seriesName");
                        String Idi = datai.getString("seriesId");
                        list.add(Pair.create(namei,Idi));
                        NameList.add(namei);
                    }
//                    newWorksActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            binding.draftManageBar.getAdapter().notifyDataSetChanged();
//                        }
//                    });
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }
    private void  mtoast(String msg){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(newWorksActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }
}