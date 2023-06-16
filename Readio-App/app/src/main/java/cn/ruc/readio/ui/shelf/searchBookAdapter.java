package cn.ruc.readio.ui.shelf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;

import cn.ruc.readio.R;
import cn.ruc.readio.bookReadActivity.bookCommentAdapter;
import cn.ruc.readio.bookReadActivity.bookDetailActivity;
import cn.ruc.readio.util.Tools;

public class searchBookAdapter extends RecyclerView.Adapter<searchBookAdapter.myHolder> {
    private final List<BookItem> searchList;
    private final Context context;
    private Activity act;
    public searchBookAdapter(Context context, List<BookItem> list)
    {
        this.context=context;
        this.searchList=list;
    }

    static class myHolder extends RecyclerView.ViewHolder{

        TextView bookName,author,bookAbstract,likes,views;
        CardView jumpView;
        ImageView bookcover;

        public myHolder(View itemView) {
            super(itemView);
            bookName=itemView.findViewById(R.id.bookName);
            author= itemView.findViewById(R.id.author);
            bookAbstract=itemView.findViewById(R.id.bookAbstract);
            jumpView= itemView.findViewById(R.id.search_card);
            bookcover=itemView.findViewById(R.id.bookcover);
            likes= itemView.findViewById(R.id.likeNum);
            views=itemView.findViewById(R.id.viewNum);
        }
    }

    @NonNull
    public searchBookAdapter.myHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new searchBookAdapter.myHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_book, parent, false));
    }

    public void onBindViewHolder(@NonNull searchBookAdapter.myHolder holder, @SuppressLint("RecyclerView") int position) {
        BookItem book=searchList.get(position);
        /*设置书名*/
        (holder).bookName.setText(book.getBookName());
        /*设置作者*/
        (holder).author.setText(book.getAuthor());
        /*设置摘要*/
        (holder).bookAbstract.setText(book.getBookAbstract());
        /*设置浏览量*/
        (holder).likes.setText(book.getLikes());
        /*设置点赞数*/
        (holder).views.setText(book.getViews());
        /*设置卡片跳转*/
        (holder).jumpView.setOnClickListener(view -> {
            Intent intent=new Intent();
            intent.setClass(context, bookDetailActivity.class);
            intent.putExtra("BookName",book.getBookName());
            intent.putExtra("Author",book.getAuthor());
            intent.putExtra("BookID",Integer.parseInt(book.getBookID()));
            context.startActivity(intent);
        });
        if (Objects.equals(book.getCoverID(), "null")) {
            holder.bookcover.setImageResource(R.drawable.default_cover2);
        } else {
            try {
                Tools.getImageBitmapAsyn(book.getCoverID(),holder.bookcover, act);
            } catch (IOException | ParseException e) {
                Tools.my_toast(Objects.requireNonNull(act),"封面获取失败");
            }
        }
    }

    public int getItemCount() {
        if(searchList==null) return 0;
        return searchList.size();
    }
    public void getActivity(Activity act) {
        this.act = act;
    }
}
