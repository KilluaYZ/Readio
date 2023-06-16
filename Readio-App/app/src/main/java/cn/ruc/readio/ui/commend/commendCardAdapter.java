package cn.ruc.readio.ui.commend;

import android.annotation.SuppressLint;
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
import java.util.List;
import java.util.Objects;
import cn.ruc.readio.R;
import cn.ruc.readio.bookReadActivity.bookDetailActivity;

public class commendCardAdapter extends RecyclerView.Adapter<commendCardAdapter.myHolder> {
    private final List<Recommendation> lists;
    private final Context context;

    public commendCardAdapter(List<Recommendation> lists, Context context) {
        this.lists = lists;
        this.context = context;
    }

    static class myHolder extends RecyclerView.ViewHolder{

        TextView tv1,tv2;
        CardView jumpView;
        ImageView pic;

        public myHolder(View itemView) {
            super(itemView);
            tv1=itemView.findViewById(R.id.quote);
            tv2= itemView.findViewById(R.id.source);
            jumpView= itemView.findViewById(R.id.card_view);
            pic=itemView.findViewById(R.id.picture);
        }
    }

    @NonNull
    @Override
    public myHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new myHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.commend_card_item, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull myHolder holder, @SuppressLint("RecyclerView") int position) {
        Log.d("TAG", "onBindViewHolder: "+lists.get(position).getQuote());
        /*设置图片*/
        (holder).pic.setImageBitmap(lists.get(position).getPic());
        /*设置名言*/
        (holder).tv1.setText(lists.get(position).getQuote());
        /*设置出处*/
        if(!Objects.equals(lists.get(position).getSource(), "null")){
            String dash="——";
            (holder).tv2.setText(dash+lists.get(position).getSource());
        }
        else {
            String text=" ";
            (holder).tv2.setText(text);
        }
        /*设置卡片跳转*/
        (holder).jumpView.setOnClickListener(view -> {
            if(lists.get(position).getBookId()!=0){
                Intent intent=new Intent();
                intent.setClass(context, bookDetailActivity.class);
                intent.putExtra("BookName",lists.get(position).getBookName());
                intent.putExtra("Author",lists.get(position).getAuthor());
                intent.putExtra("BookID",lists.get(position).getBookId());
                context.startActivity(intent);
            }
            else{
                Toast.makeText(context,"这个好句没有对应的书籍哦~",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return lists.size();
    }
}
