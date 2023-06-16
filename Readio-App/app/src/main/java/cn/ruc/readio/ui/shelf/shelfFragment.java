package cn.ruc.readio.ui.shelf;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import cn.ruc.readio.R;
import cn.ruc.readio.bookReadActivity.readBookActivity;
import cn.ruc.readio.databinding.FragmentShelfBinding;
import cn.ruc.readio.ui.userpage.login.LoginActivity;
import cn.ruc.readio.util.Auth;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class shelfFragment extends Fragment {

    private FragmentShelfBinding binding;
    private GridView grid_view;
    private int if_empty=0;
    static public Fragment shelffrag;

    private List<BookItem> lists;
    private TextView search_button;
    private EditText search_edit;
    private String search_content,select_content;
    private Spinner spinner;
    private ImageButton refresh_button;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentShelfBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        shelffrag = this;
        lists = new ArrayList<>();
        Auth.Token token = new Auth.Token(getActivity());
        if(token.isEmpty())
        {
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
        }
        refreshData();
        if(if_empty==1)
        {
            if(getActivity()!=null)
            {
                Toast.makeText(getActivity(),"您的书架里还没有书呐！\n快往里头添加一些吧！",Toast.LENGTH_SHORT).show();
            }
        }
        /*下拉选择框*/
        spinner=binding.spinner;
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
        search_edit=binding.edittext;
        search_button=binding.search;
        search_button.setOnClickListener(view -> {
            search_content=search_edit.getText().toString();
            Intent intent = new Intent(getContext(), searchBookActivity.class);
            intent.putExtra("search_content", search_content);
            intent.putExtra("select_content",select_content);
            Objects.requireNonNull(getContext()).startActivity(intent);
        });

        refresh_button=binding.refreshButton;
        refresh_button.setOnClickListener(view -> {
            refreshData();
        });

        grid_view = binding.bookGridview;
        bookAdapter myAdapter = new bookAdapter(getContext(),lists);
        grid_view.setAdapter(myAdapter);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        search_edit.setText("");
    }

    private List<BookItem> getData()
    {
        List<BookItem> data = new ArrayList<>();
        data.add(new BookItem("三体","刘慈欣"));
        data.add(new BookItem("挪威的森林","村上春树"));
        data.add(new BookItem("活着","余华"));
        data.add(new BookItem("红楼梦","曹雪芹"));
        data.add(new BookItem("百年孤独","马尔克斯"));
        data.add(new BookItem("哈姆雷特","莎士比亚"));
        data.add(new BookItem("月亮与六便士","毛姆"));
        data.add(new BookItem("复活","马尔克斯"));
        data.add(new BookItem("平凡的世界","路遥"));
        data.add(new BookItem("局外人","加缪"));
        return data;
    }

    public void refreshData(){
        HttpUtil.getRequestWithTokenAsyn(getActivity(),"/app/books/list",new ArrayList<>(), new Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                mtoast();
            }
            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                try {
                    lists.clear();
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONObject data = jsonObject.getJSONObject("data");
                    int mybook_num = data.getInt("size");
                    JSONArray mybook_list = data.getJSONArray("data");
                    if (mybook_num == 0) {
                        if_empty = 1;
                        Objects.requireNonNull(getActivity()).runOnUiThread(() -> Toast.makeText(getActivity(), "您的书架里还没有书呐！\n快往里头添加一些吧！", Toast.LENGTH_SHORT).show());
                    } else {
                        for (int i = 0; i < mybook_list.length(); i++) {
                            JSONObject mybook = mybook_list.getJSONObject(i);
                            BookItem book = new BookItem(mybook.getString("bookName"), mybook.optString("authorID"), mybook.optString("coverId"));
                            book.setBookID(mybook.getString("id"));
                            lists.add(book);
                        }
                        if(getActivity()!=null)
                        {
                        Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                            Log.d("bookadpter", "正在更新");
                            bookAdapter myAdapter = new bookAdapter(getContext(), lists);
                            binding.bookGridview.setAdapter(myAdapter);
                        });}
                    }
                } catch (JSONException e) {
                    Tools.my_toast(Objects.requireNonNull(getActivity()),"书架加载失败");
                }
            }
        });
    }

    private void  mtoast(){
        Objects.requireNonNull(getActivity()).runOnUiThread(() -> Toast.makeText(getActivity(), "请求异常，加载不出来",Toast.LENGTH_LONG).show());
    }


}