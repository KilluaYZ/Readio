package cn.ruc.readio.ui.shelf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;

import cn.ruc.readio.R;
import cn.ruc.readio.bookReadActivity.bookDetailActivity;
import cn.ruc.readio.bookReadActivity.readBookActivity;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class bookAdapter extends BaseAdapter {

    private final List<BookItem> bookList;
    private final Context context;
    public bookAdapter(Context context, List<BookItem> list)
    {
        this.context=context;
        this.bookList=list;
    }

    public int getCount()
    {
        if(bookList.isEmpty())
        {
            return 0;
        }
        return bookList.size();
    }
    public Object getItem(int position)
    {
        if(position>bookList.size()-1)
        {
            return null;
        }
        return bookList.get(position);
    }
    public long getItemId(int position)
    {
        return 0;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_item, null);
            holder = new ViewHolder();
            holder.text = convertView.findViewById(R.id.book_title);
            holder.cover = convertView.findViewById(R.id.book_cover);
            holder.jumpview= convertView.findViewById(R.id.book_item);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        BookItem books = bookList.get(position);
        if (books != null) {
            holder.text.setText(books.getBookName());
                if (Objects.equals(books.getCoverID(), "null")) {
                    holder.cover.setImageResource(R.drawable.default_cover2);
                } else {
                    try {
                        Tools.getImageBitmapAsyn(books.getCoverID(),holder.cover,shelfFragment.shelffrag.getActivity());
                    } catch (IOException | ParseException e) {
                        Tools.my_toast(Objects.requireNonNull(shelfFragment.shelffrag.getActivity()),"封面获取失败");
                    }
                }

            /*设置跳转阅读界面*/
            holder.jumpview.setOnClickListener(view -> {
                Intent read_intent=new Intent();
                read_intent.setClass(context, readBookActivity.class);
                read_intent.putExtra("BookName",bookList.get(position).getBookName());
                read_intent.putExtra("Author",bookList.get(position).getAuthor());
                read_intent.putExtra("BookID",bookList.get(position).getBookID());
                context.startActivity(read_intent);
            });

            holder.jumpview.setOnLongClickListener(view->{
                showPopMenu(view,bookList.get(position).getBookID());
                return false;
            });
        }
        return convertView;
    }

    static class ViewHolder {
        TextView text;
        ImageView cover;
        CardView jumpview;
    }

    public void showPopMenu(View view,String bookID){
        PopupMenu menu = new PopupMenu(context,view);
        menu.getMenuInflater().inflate(R.menu.shelf_select_menu,menu.getMenu());
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.yes_item:
                        delete_book_from_shelf(bookID);
                        break;

                    case R.id.no_item:
                        break;
                }
                return true;
            }
        });
        menu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
            }
        });
        menu.show();
    }
    private void delete_book_from_shelf(String bookId){
        String json = "{\"bookId\"" +":"+ bookId+"}";
        HttpUtil.postRequestWithTokenJsonAsyn((Activity) context,"/app/books/delete", json, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Objects.requireNonNull((Activity)context).runOnUiThread(() -> {
                    Toast.makeText(context,"已删除,刷新页面后它就不见啦~",Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}

