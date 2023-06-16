package cn.ruc.readio.ui.shelf;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.ruc.readio.R;
import cn.ruc.readio.bookReadActivity.aBookCommentAdapter;
import cn.ruc.readio.bookReadActivity.allCommentActivity;
import cn.ruc.readio.ui.commend.commendCardAdapter;
import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import cn.ruc.readio.worksActivity.PieceComments;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class searchBookActivity extends AppCompatActivity {

    private String search_content,select_content;
    private List<BookItem> searchList=new ArrayList<>();
    private TextView search_button;
    private EditText search_edit;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_book);

        /*调整状态栏为透明色*/
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        /*接受传递的消息*/
        Intent intent = getIntent();

        /*获取内容*/
        search_content = intent.getStringExtra("search_content");
        select_content = intent.getStringExtra("select_content");

        /*设置recyclerview的卡片*/
        LinearLayoutManager m=new LinearLayoutManager(searchBookActivity.this);
        RecyclerView recycler_view=findViewById(R.id.search_recyclerView);
        recycler_view.setLayoutManager(m);
        searchBookAdapter myAdapter = new searchBookAdapter(searchBookActivity.this,searchList);
        myAdapter.getActivity(searchBookActivity.this);
        recycler_view.setAdapter(myAdapter);

        try {
            refreshData();
        } catch (JSONException e) {
            mtoast("没有搜索结果~");
        }
        /*下拉选择框*/
        spinner=findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                select_content=parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //在下拉选项种选择了本来选的东西，也就是说没改变选项，调用这个
            }
        });
        /*搜索按钮*/
        search_edit=findViewById(R.id.edittext);
        search_button=findViewById(R.id.search);
        search_button.setOnClickListener(view -> {
            search_content=search_edit.getText().toString();
            try {
                searchList.clear();
                refreshData();
            } catch (JSONException e) {
                mtoast("没有搜索结果~");
            }
        });

    }

    public void refreshData() throws JSONException {
        ArrayList<Pair<String,String>> json=new ArrayList<>();
        if(Objects.equals(select_content, "书名")) {
            json.add(Pair.create("bookName", search_content));
        }else {
            json.add(Pair.create("authorName", search_content));
        }
        HttpUtil.getRequestAsyn("/app/books/search", json, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mtoast("请求异常，加载不出来");
            }
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    /*获取所有书籍信息*/
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONObject data = jsonObject.getJSONObject("data");

                    /*取出结果列表*/
                    JSONArray search_list=data.getJSONArray("data");
                    int search_size=data.getInt("size");
                    if(search_size==0){
                        mtoast("没有搜索结果哦~");
                    }
                    if(search_size!=0){
                        for(int i = 0; i < search_size; i++){
                            JSONObject search_item = search_list.getJSONObject(i);
                            BookItem bookItem = new BookItem(search_item.getString("bookName"),search_item.getString("authorName"));
                            bookItem.setBookID(search_item.getString("id"));
                            bookItem.setBookAbstract(search_item.optString("abstract","该书籍暂无简介"));
                            bookItem.setCoverID(search_item.getString("coverId"));
                            bookItem.setLikes(search_item.getString("likes"));
                            bookItem.setViews(search_item.getString("views"));
                            searchList.add(bookItem);
                        }
                        runOnUiThread(() -> {
                            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.search_recyclerView);
                            Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                        });
                    }
                    runOnUiThread(() -> {
                        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.search_recyclerView);
                        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void mtoast(String msg){
        runOnUiThread(() -> Toast.makeText(searchBookActivity.this,msg,Toast.LENGTH_LONG).show());
    }
}