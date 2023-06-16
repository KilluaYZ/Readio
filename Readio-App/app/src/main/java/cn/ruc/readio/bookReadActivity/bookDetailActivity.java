package cn.ruc.readio.bookReadActivity;
import static java.lang.Math.min;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import cn.ruc.readio.R;
import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.ui.userpage.login.LoginActivity;
import cn.ruc.readio.util.Auth;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import cn.ruc.readio.worksActivity.PieceComments;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class bookDetailActivity extends AppCompatActivity{
    private final Activity bookDetailAct = this;
    private String BookName,Author;
    private int BookID;
    private List<PieceComments> comment_list=new ArrayList<>();
    private TextView book_name, author, book_abstract,view_count,length,likes,shares,like_this_book_text,add_shelf_text,write_comment,more_comment;
    private ImageButton like_this_book,read_book,add_shelf;
    private ImageView book_cover;
    private int like_book_times=0,add_shelf_times=0,if_like=0,if_add_bookmark=0,if_empty=0;
    private TextView no_comment;
    private int book_likes=0,book_shares=0,child_commentnum;


    @SuppressLint({"MissingInflatedId", "ResourceAsColor"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookdetail);

        /*调整状态栏为透明*/
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        /*接受传递的消息*/
        Intent intent = getIntent();

        /*获取内容*/
        BookName = intent.getStringExtra("BookName");
        Author = intent.getStringExtra("Author");
        BookID = intent.getIntExtra("BookID",0);

        /*设置内容*/
        initdetail();

        /*从服务器中获取数据*/
        refreshData();

        /*通过提取图片颜色，设置背景色*/
        //setBackgroundColor();

        /*设置返回按钮*/
        ImageButton re_button = findViewById(R.id.return_commend);
        re_button.setOnClickListener(v -> finish());

        /*设置分享按钮*/
        ImageButton share_button=findViewById(R.id.share_button);
        share_button.setOnClickListener(view-> Tools.my_toast(bookDetailActivity.this,"分享功能，待开发..."));


        /*设置加入书架按钮*/
        add_shelf.setOnClickListener(view1 -> {
            Auth.Token token = new Auth.Token(bookDetailActivity.this);
            if(token.isEmpty())
            {
                Intent intent1 = new Intent(bookDetailActivity.this, LoginActivity.class);
                startActivity(intent1);
            }else {
                add_shelf_times++;
                if ((if_add_bookmark + add_shelf_times) % 2 == 0) {
                    delete_book_from_shelf(String.valueOf(BookID));
                } else {
                    add_book_to_shelf(String.valueOf(BookID));
                }
            }
        });

        /*设置阅读按钮*/
        read_book.setOnClickListener(view -> {
            Intent read_intent=new Intent(bookDetailActivity.this,readBookActivity.class);
            read_intent.putExtra("BookName",BookName);
            read_intent.putExtra("Author",Author);
            read_intent.putExtra("BookID",String.valueOf(BookID));
            startActivity(read_intent);
        });

        /*设置点赞按钮*/
        like_this_book.setOnClickListener(view1 -> {
            Auth.Token token = new Auth.Token(bookDetailActivity.this);
            if(token.isEmpty())
            {
                Intent intent2 = new Intent(bookDetailActivity.this, LoginActivity.class);
                startActivity(intent2);
            }
            else {
                like_book_times++;
                if ((if_like + like_book_times) % 2 == 0) {
                    dislike_this_book(BookID);
                } else {
                    like_this_book(BookID);
                }
            }
        });

        /*设置评论部分*/
        /*添加下划线*/
        write_comment.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        more_comment.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        /*设置卡片中的评论内容*/
        SetComments();
        /*设置写评论按钮*/
        write_comment.setOnClickListener(view1 -> {
            Auth.Token token = new Auth.Token(bookDetailActivity.this);
            if(token.isEmpty())
            {
                Intent intent1 = new Intent(bookDetailActivity.this, LoginActivity.class);
                startActivity(intent1);
            }else {
                Intent intent2 = new Intent(bookDetailActivity.this, writeCommentActivity.class);
                intent2.putExtra("BookID", BookID);
                startActivity(intent2);
            }

        });
        /*添加跳转到所有评论界面链接*/
        more_comment.setOnClickListener(view -> {
            if(if_empty==1)
            {
                Toast.makeText(bookDetailActivity.this,"没有评论啦~",Toast.LENGTH_SHORT).show();
            }
            else {
                Intent comment_intent = new Intent(bookDetailActivity.this, allCommentActivity.class);
                comment_intent.putExtra("BookName", BookName);
                comment_intent.putExtra("Author", Author);
                comment_intent.putExtra("BookID", BookID);
                startActivity(comment_intent);
            }
        });
    }

    /*设置内容*/
    private void initdetail(){
        book_name = findViewById(R.id.book_name);
        author = findViewById(R.id.author);
        book_abstract = findViewById(R.id.book_abstract);
        view_count=findViewById(R.id.views);
        length=findViewById(R.id.words);
        like_this_book=findViewById(R.id.like_this_book);
        read_book=findViewById(R.id.read);
        add_shelf=findViewById(R.id.add_bookmark);
        likes=findViewById(R.id.likes);
        shares=findViewById(R.id.shares);
        like_this_book_text=findViewById(R.id.like_this_book_text);
        add_shelf_text=findViewById(R.id.add_bookmark_text);
        book_cover=findViewById(R.id.picture);
        no_comment=findViewById(R.id.no_comment);
        write_comment=findViewById(R.id.writecomment);
        more_comment=findViewById(R.id.more_comment);

    }
    @SuppressLint("ResourceAsColor")
    private void setBackgroundColor() {
        RelativeLayout detail_page = findViewById(R.id.detail_page);
        Bitmap myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_cover2);

        Palette p = Palette.from(myBitmap).generate();
        Palette.Swatch Swatch = p.getLightVibrantSwatch();

        int backgroundColor = R.color.white;

        if(Swatch != null){
            backgroundColor = Swatch.getRgb();
        }
        detail_page.setBackgroundColor(backgroundColor);
        detail_page.getBackground().setAlpha(30);
    }

    /*设置默认评论内容，测试用*/
    private void initComments() {

        comment_list = new ArrayList<>();

        User user = new User("呆头鹅","2020201694@ruc.edu.cn","123456");

        comment_list.add(new PieceComments("好棒\n好棒",12,user));
        comment_list.add(new PieceComments("好棒",12,user));
        comment_list.add(new PieceComments("好棒",12,user));
        comment_list.add(new PieceComments("好棒",12,user));
    }

    private void SetComments(){
        View view = findViewById(R.id.comments_part);
        Context context = view.getContext();

        //initComments();
        LinearLayoutManager m=new LinearLayoutManager(bookDetailActivity.this);
        m.setOrientation(LinearLayoutManager.HORIZONTAL);
        RecyclerView recycler_view = findViewById(R.id.book_comment);
        recycler_view.setLayoutManager(m);
        bookCommentAdapter myAdapter = new bookCommentAdapter(context,comment_list);
        myAdapter.getActivity(bookDetailActivity.this);
        recycler_view.setAdapter(myAdapter);

    }
    public void refreshData(){
        HttpUtil.getRequestWithTokenAsyn(this ,"/app/book/"+ BookID, new ArrayList<>(), new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mtoast();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    /*获取所有书籍信息*/
                    assert response.body() != null;
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONObject data = jsonObject.getJSONObject("data");

                    /*取出是否加入书架*/
                    String added = data.getString("added");
//                    Log.d("if_add_bookmark", added);
                    if (added.equals("true")) {
                        if_add_bookmark = 1;
                        bookDetailAct.runOnUiThread(() -> {
                            add_shelf.setImageResource(R.drawable.addedinshelf);
                            add_shelf_text.setText("已加入书架");
                        });
                    } else {
                        bookDetailAct.runOnUiThread(() -> {
                            add_shelf.setImageResource(R.drawable.addinshelf);
                            add_shelf_text.setText("加入书架");
                        });
                    }
                    /*取出是否点赞*/
                    String liked = data.getString("liked");
                    if (liked.equals("true")) {
                        if_like = 1;
                        bookDetailAct.runOnUiThread(() -> {
                            like_this_book.setImageResource(R.drawable.ok);
                            like_this_book_text.setText("已点赞");
                        });
                    } else {
                        bookDetailAct.runOnUiThread(() -> {
                            like_this_book.setImageResource(R.drawable.commend);
                            like_this_book_text.setText("点赞");
                        });
                    }
                    /*取出书籍自身信息部分*/
                    JSONObject book_info = data.getJSONObject("book_info");
                    bookDetailActivity.this.runOnUiThread(()-> {
                        try {
                            book_name.setText(book_info.getString("bookName"));
                        } catch (JSONException e) {
                            Tools.my_toast(bookDetailActivity.this,"加载失败1");
                        }
                        try {
                            author.setText(book_info.getString("authorName"));
                        } catch (JSONException e) {
                            Tools.my_toast(bookDetailActivity.this,"加载失败2");
                        }
                        try {
                            length.setText(book_info.getString("length"));
                        } catch (JSONException e) {
                            Tools.my_toast(bookDetailActivity.this,"加载失败3");
                        }
                        try {
                            view_count.setText(book_info.getString("views"));
                        } catch (JSONException e) {
                            Tools.my_toast(bookDetailActivity.this,"加载失败4");
                        }
                        likes.setText(book_info.optString("likes","0"));
                        book_likes = Integer.parseInt(book_info.optString("likes","0"));
                        shares.setText(book_info.optString("shares", "0"));
                        book_shares= Integer.parseInt(book_info.optString("shares", "0"));
                        try {
                            if (!book_info.optString("abstract").equals("null")) {
                                book_abstract.setText(book_info.getString("abstract"));
                            } else {
                                book_abstract.setText("这里是书籍简介~\n但这本书暂时没有~");
                            }
                        } catch (JSONException e) {
                            Tools.my_toast(bookDetailActivity.this,"加载失败7");
                        }
                        try {
                            if (!book_info.getString("coverId").equals("null")) {
                                final Bitmap[] cover = {null};
                                    try{
                                        Tools.getImageBitmapAsyn(book_info.getString("coverId"),book_cover,bookDetailAct);
                                    } catch (JSONException | IOException | ParseException e) {
                                        Tools.my_toast(bookDetailAct, "加载失败8");
                                    }
                            }
                        } catch (JSONException e) {
                            Tools.my_toast(bookDetailActivity.this,"加载失败9");
                        }
                    });
                    /*取出书籍评论*/
                    JSONObject book_comments=data.getJSONObject("comments");
                    int comment_size=book_comments.getInt("size");
                    JSONArray comments_list=book_comments.getJSONArray("data");
                    if(comment_size==0)
                    {
                        bookDetailAct.runOnUiThread(() -> {
                            no_comment.setText("这本书暂时没有评论哦~\n来当第一位吧！");
                            no_comment.setGravity(Gravity.CENTER);
                        });
                        if_empty=1;
                    }
                    if(comment_size!=0) {
                        int show_size=min(3,comment_size); //书籍详情页最多显示3条
                        for (int i = 0; i < show_size; i++) {
                            JSONObject comment_item = comments_list.getJSONObject(i);
                            User user = get_userinfo(comment_item.getString("userId"),i);
                            PieceComments comment = new PieceComments(comment_item.getString("content"), comment_item.getInt("likes"), user);
                            comment.setBookId(String.valueOf(BookID));
                            comment.setCommentId(Integer.parseInt(comment_item.getString("commentId")));
                            comment.setLikesNum(comment_item.getInt("likes"));
                            comment.setDate(comment_item.getString("createTime"));
                            comment.setIf_liked(comment_item.getString("liked"));
                            int childcommentnum=get_childcommentnum(comment_item.getString("commentId"),i);
                            comment.setChildCommentNum(childcommentnum);
                            comment_list.add(comment);
                        }
                    }
                } catch (JSONException e) {
                    Tools.my_toast(bookDetailActivity.this,"加载失败10");
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
                    bookDetailActivity.this.runOnUiThread(() -> {
                        RecyclerView recyclerView = findViewById(R.id.book_comment);
                        if(recyclerView!=null) {
                            Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                        }
                    });
                    new Thread(() -> {
                        Bitmap pic = null;
                        try {
                            pic = Tools.getImageBitmapSyn(bookDetailActivity.this, comment_list.get(i).getUser().getAvaID());
                        } catch (IOException | JSONException | ParseException e) {
                            Tools.my_toast(bookDetailActivity.this,"图片加载出错啦！");
                        }
                        comment_list.get(i).getUser().setAvator(pic);
                        Log.d("commentAdapter","需要更新");
                        bookDetailActivity.this.runOnUiThread(() -> {
                            RecyclerView recyclerView = findViewById(R.id.book_comment);
                            if(recyclerView!=null) {
                                Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                            }
                        });
                    }).start();
                    } catch (JSONException ex) {
                    Tools.my_toast(bookDetailAct,"用户加载失败");
                }
            }
        });
        return user;
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
                    Log.d("commentAdapter","需要更新");
                    bookDetailActivity.this.runOnUiThread(() -> {
                        RecyclerView recyclerView = findViewById(R.id.book_comment);
                        if (recyclerView != null) {
                            Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                        }
                    });
                } catch (JSONException ex) {
                    Tools.my_toast(bookDetailAct, "子评论数量加载失败");
                }
            }
        });
        return child_commentnum;
    }

    private void mtoast(){
        runOnUiThread(() -> Toast.makeText(bookDetailActivity.this, "请求异常，加载不出来",Toast.LENGTH_LONG).show());
    }
    private void add_book_to_shelf(String bookId){
        String json = "{\"bookId\"" +":"+ bookId+"}";
        HttpUtil.postRequestWithTokenJsonAsyn(bookDetailActivity.this,"/app/books/add", json, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                bookDetailAct.runOnUiThread(() -> {
                    Toast.makeText(bookDetailActivity.this, "添加失败，请重试", Toast.LENGTH_SHORT).show();
                    add_shelf.setImageResource(R.drawable.addinshelf);
                    add_shelf_text.setText("加入书架");
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                bookDetailAct.runOnUiThread(() -> {
                    Toast.makeText(bookDetailActivity.this,"加入啦",Toast.LENGTH_SHORT).show();
                    add_shelf.setImageResource(R.drawable.addedinshelf);
                    add_shelf_text.setText("已加入书架");
                });
            }
        });
    }
    private void delete_book_from_shelf(String bookId){

        String json = "{\"bookId\"" +":"+ bookId+"}";

        HttpUtil.postRequestWithTokenJsonAsyn(this,"/app/books/delete", json, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                bookDetailAct.runOnUiThread(() -> {
                    Toast.makeText(bookDetailActivity.this, "取消加入失败，请重试", Toast.LENGTH_SHORT).show();
                    add_shelf.setImageResource(R.drawable.addedinshelf);
                    add_shelf_text.setText("已加入书架");
                });

            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                bookDetailAct.runOnUiThread(() -> {
                    Toast.makeText(bookDetailActivity.this,"删除啦",Toast.LENGTH_SHORT).show();
                    add_shelf.setImageResource(R.drawable.addinshelf);
                    add_shelf_text.setText("加入书架");
                });
            }
        });
    }
    private void like_this_book(int bookId){

        String json = "{\"bookId\" "+":"+ bookId+",\"like\":"+1+"}";
        HttpUtil.postRequestWithTokenJsonAsyn(bookDetailActivity.this,"/app/book/"+bookId+"/like", json, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                bookDetailAct.runOnUiThread(() -> {
                    Toast.makeText(bookDetailActivity.this, "点赞失败，请重试", Toast.LENGTH_SHORT).show();
                    like_this_book.setImageResource(R.drawable.commend);
                    like_this_book_text.setText("点赞");
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                bookDetailAct.runOnUiThread(() -> {
                    Toast.makeText(bookDetailActivity.this,"感谢您的点赞^_^",Toast.LENGTH_SHORT).show();
                    like_this_book.setImageResource(R.drawable.ok);
                    like_this_book_text.setText("已点赞");
                    if(if_like==1){
                        likes.setText(String.valueOf(book_likes));
                    }else{
                    likes.setText(String.valueOf(book_likes+1));
                    }
                });
            }
        });
    }
    private void dislike_this_book(int bookId){
        String json = "{\"bookId\" "+":"+ bookId+",\"like\":"+0+"}";
        HttpUtil.postRequestWithTokenJsonAsyn(bookDetailActivity.this,"/app/book/"+bookId+"/like", json, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                bookDetailAct.runOnUiThread(() -> {
                    Toast.makeText(bookDetailActivity.this, "取消点赞失败，请重试", Toast.LENGTH_SHORT).show();
                    like_this_book.setImageResource(R.drawable.ok);
                    like_this_book_text.setText("点赞");
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                bookDetailAct.runOnUiThread(() -> {
                    Toast.makeText(bookDetailActivity.this,"不要哇ToT",Toast.LENGTH_SHORT).show();
                    like_this_book.setImageResource(R.drawable.commend);
                    like_this_book_text.setText("点赞");
                    if(if_like==1) {
                        likes.setText(String.valueOf(book_likes-1));
                    }
                    else{
                        likes.setText(String.valueOf(book_likes));
                    }
                });
            }
        });
    }
}
