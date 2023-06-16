package cn.ruc.readio.bookReadActivity;

import static android.util.Pair.create;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import androidx.annotation.NonNull;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import cn.ruc.readio.R;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class readBookActivity extends Activity {
    private String BookID,BookName,Author;
    ViewPager pager = null;
    PagerTabStrip tabStrip = null;
    ArrayList<View> viewContainer = new ArrayList<View>();
    ArrayList<String> contentList = new ArrayList<String>();
    private int firstPosition_Page = 0;
    private int nPosition;
    private book my_book=new book();
    private TextView content;
    private TextView readPage;
    private TextView BookInfo;
    private View view1;
    private View view2;
    private EditText jumpPage;
    int Page = 0;
    private ArrayList<Integer> ifLoad;
    private int lastPosition = 0;
    private int flag = 0;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        contentList = null;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_book);
        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }  //用于调整状态栏为透明色
        readPage = findViewById(R.id.pageBar);
        BookInfo = findViewById(R.id.book_nameBar);
        jumpPage = findViewById(R.id.jumpPageButton);
        ifLoad = new ArrayList<>();
        InitifLoad();
        /*接受传递的消息*/
        Intent intent = getIntent();

        /*获取内容*/
        BookName = intent.getStringExtra("BookName");
        Author = intent.getStringExtra("Author");
        BookID = intent.getStringExtra("BookID");

        getBook();


        pager = (ViewPager) this.findViewById(R.id.viewpager);

        view1 = LayoutInflater.from(this).inflate(R.layout.item_bookview,null);
        view2 = LayoutInflater.from(this).inflate(R.layout.item_bookview,null);
        content = view1.findViewById(R.id.content);
        content.setText("内容正在加载中，请稍后...");
        viewContainer.add(view1);
        viewContainer.add(view2);
        bookReadAdapter myAdapter = new bookReadAdapter();
        pager.setAdapter(myAdapter);
        pager.setCurrentItem(1,false);
        pager.getAdapter().notifyDataSetChanged();
        myAdapter.setNewViewList(viewContainer);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                nPosition = position;
                jumpPage.setText("");
                if (nPosition > lastPosition) {
                    Page++;
                    if (contentList != null)
                    {
                        String wholePage = toString().valueOf(contentList.size());
                        String nowPage = toString().valueOf(Page+1);
                        readPage.setText(nowPage+"/"+wholePage);
                    }
                }
                if (nPosition < lastPosition) {
                    Page--;
                    if (contentList != null)
                    {
                        String wholePage = toString().valueOf(contentList.size());
                        String nowPage;
                        if(flag == 0) {
                            nowPage = toString().valueOf(Page + 1);
                        }
                        else{
                            nowPage = toString().valueOf(Page + 2);
                        }
                        readPage.setText(nowPage+"/"+wholePage);
                    }
                }
                if (nPosition == viewContainer.size() - 1) {
                    if(contentList == null)
                    {
                        Tools.my_toast(readBookActivity.this,"内容仍在加载中，请稍等一下~");
                    }
                    else{
                        if(Page < contentList.size()-1) {
                            if(ifLoad.get(Page+1) == 0) {
                                addPage(myAdapter,contentList.get(Page+1));
                                Loaded(Page+1);
                            }
                        }
                        else{
                            Tools.my_toast(readBookActivity.this,"这是最后一页啦~");
                        }

                    }
//                    pager.setCurrentItem(nPosition, false);
                }

                if (nPosition == 0) {
                    if(contentList == null)
                    {
                        Tools.my_toast(readBookActivity.this,"内容仍在加载中，请稍等一下~");
                    }
                    else {
                        if(Page>=1) {
                            if(ifLoad.get(Page-1) == 0) {
                                firstPosition_Page = Page-1;
                                addPageFront(myAdapter,contentList.get(Page-1));
                                pager.setCurrentItem(nPosition+1, false);
                                pager.getAdapter().notifyDataSetChanged();
                                Loaded(Page-1);

                            }
                        }
                        else{
                            Tools.my_toast(readBookActivity.this,"已经是第一页啦~");
                        }
                    }
                }
                lastPosition = nPosition;
                flag = 0;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state != pager.SCROLL_STATE_IDLE) return;
                pager.setCurrentItem(nPosition, false);
                pager.getAdapter().notifyDataSetChanged();
            }

        });

        jumpPage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(String.valueOf(jumpPage.getText()).equals("")) {
                    return false;
                }
                int jump_page;
                try{
                    jump_page = Integer.parseInt(String.valueOf(jumpPage.getText())) - 1;}
                catch (Exception e){
                    Tools.my_toast(readBookActivity.this,"请输入正确的页码");
                    return false;
                }
                if(jump_page<2 || jump_page > contentList.size()){
                    Tools.my_toast(readBookActivity.this,"请输入正确的页码");
                    return false;
                }
                if(ifLoad.get(jump_page)==1)
                {
                    int toPosition = jump_page - firstPosition_Page;
                    if(Page < jump_page)
                    {
                        Page = jump_page-1;
                    }
                    if(Page > jump_page)
                    {
                        Page = jump_page + 1;
                    }
                    pager.setCurrentItem(toPosition,false);
                    pager.getAdapter().notifyDataSetChanged();
                }
                else {
                    if (jump_page > Page) {
                        for (int k = Page + 1; k <= jump_page + 1; k++) {
                            if (ifLoad.get(k) == 0 && k <= contentList.size() - 1) {
                                addPage(myAdapter, contentList.get(k));
                                Loaded(k);
                            }
                        }

                        Page = jump_page - 1;
                    } else {
                        for (int k = Page - 1; k >= jump_page; k--) {
                            if (ifLoad.get(k) == 0 && k >= 0) {
                                addPageFront(myAdapter, contentList.get(k));
                                Loaded(k);
                                flag = 1;
                            }
                        }
                        if (jump_page - 1 < 0) {
                            jump_page = 0;
                        } else {
                            firstPosition_Page = jump_page;
                        }
                        Page = jump_page + 1;
                    }
                    int toPosition = jump_page - firstPosition_Page;
                    pager.setCurrentItem(toPosition, false);
                    pager.getAdapter().notifyDataSetChanged();
                }
                return false;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ArrayList<Pair<String,String>> queryParam = new ArrayList<>();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("bookId",my_book.getBookId());
            jsonObject.put("progress",toString().valueOf(getProgress()));
        } catch (JSONException e) {
//            Tools.my_toast(readBookActivity.this,"进度上传失败");
        }
        String json = jsonObject.toString();
        HttpUtil.postRequestWithTokenJsonAsyn(readBookActivity.this, "/app/books/update", json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                Tools.my_toast(readBookActivity.this,"进度上传失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
//                Tools.my_toast(readBookActivity.this,"进度保存成功~");
            }
        });
    }

    void getBook(){
        HttpUtil.getRequestWithTokenAsyn(readBookActivity.this,"/app/books/reading/"+Integer.parseInt(BookID), new ArrayList<>() ,new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Tools.my_toast(readBookActivity.this,"请求异常，加载不出来");
            }
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONObject bookinfo = jsonObject.getJSONObject("data");

                    my_book.setBookId(bookinfo.getString("id"));
                    my_book.setBookAbstract(bookinfo.getString("abstract"));
                    my_book.setBookName(bookinfo.getString("bookName"));
                    my_book.setAuthorName(bookinfo.getString("authorName"));
                    my_book.setSize(bookinfo.getInt("size"));
                    my_book.setProgress(bookinfo.getInt("progress"));
                    JSONArray content=bookinfo.getJSONArray("content");
                    ArrayList<Pair<String, String>> bookcontent=new ArrayList<>();

                    for(int i = 0;i<content.length();i++){
                        JSONObject contenti= content.getJSONObject(i);
                        bookcontent.add(create(contenti.getString("ChapterName"),contenti.getString("Text")));
                    }
                    my_book.setContent(bookcontent);
                    contentList = new BookPageDivider(my_book,300).getPagesList();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BookInfo.setText(my_book.getBookName()+" "+my_book.getAuthorName());
                            TextView view1_content = view1.findViewById(R.id.content);
                            TextView view2_content = view2.findViewById(R.id.content);
                            Page = my_book.getProgress()/300;
                            firstPosition_Page = Page;
                            String wholePage = toString().valueOf(contentList.size());
                            String nowPage = toString().valueOf(Page+1);
                            readPage.setText(nowPage+"/"+wholePage);
                            view1_content.setText(contentList.get(Page));
                            Loaded(Page);
                            if(Page < contentList.size()-1) {
                                view2_content.setText(contentList.get(Page + 1));
                                Loaded(Page + 1);
                            }
                            if(Page>=1)
                            {
                                if(ifLoad.get(Page-1) == 0) {
                                    firstPosition_Page = Page -1 ;
                                    LayoutInflater inflater = LayoutInflater.from(readBookActivity.this);
                                    View view = inflater.inflate(R.layout.item_bookview, null);
                                    TextView content = (TextView) view.findViewById(R.id.content);
                                    content.setText(contentList.get(Page-1));
                                    viewContainer.add(0,view);
                                    pager.getAdapter().notifyDataSetChanged();
                                    Loaded(Page-1);
                                    Page--;
                                    pager.setCurrentItem(nPosition+1, false);
                                    pager.getAdapter().notifyDataSetChanged();
                                }
                            }
                        }
                    });
                } catch (JSONException e) {
                    Tools.my_toast(readBookActivity.this, "加载较慢，请重试");
                }
            }
        });
    }
    int getProgress(){
        return Page*300+1;
    }
    void InitifLoad(){
        for(int i = 0; i < 10000; i++)
        {
            ifLoad.add(0);
        }
    }
    void Loaded(int Position)
    {
        ifLoad.set(Position, 1);
    }
    public void addPage(bookReadAdapter myAdapter, String text) {
        LayoutInflater inflater = LayoutInflater.from(readBookActivity.this);
        View view = inflater.inflate(R.layout.item_bookview, null);
        TextView content = (TextView) view.findViewById(R.id.content);
        content.setText(text);
        viewContainer.add(view);
        myAdapter.notifyDataSetChanged();

    }

    public void addPageFront(bookReadAdapter myAdapter,String text){
        LayoutInflater inflater = LayoutInflater.from(readBookActivity.this);
        View view = inflater.inflate(R.layout.item_bookview, null);
        TextView content = (TextView) view.findViewById(R.id.content);
        content.setText(text);
        viewContainer.add(0,view);
        myAdapter.notifyDataSetChanged();
    }
}