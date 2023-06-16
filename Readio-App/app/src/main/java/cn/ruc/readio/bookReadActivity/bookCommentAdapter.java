package cn.ruc.readio.bookReadActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.ruc.readio.R;
import cn.ruc.readio.util.Tools;
import cn.ruc.readio.worksActivity.PieceComments;

public class bookCommentAdapter extends RecyclerView.Adapter<bookCommentAdapter.ViewHolder> {
    private final List<PieceComments> CommentsList;
    private List<Integer> like_comment_times= new ArrayList<>();
    private final Context context;
    private Activity act;
    private int if_like_this_comment;

    public bookCommentAdapter(Context context, List<PieceComments> CommentsList){
        this.CommentsList = CommentsList;
        this.context = context;
    }
    static class ViewHolder extends RecyclerView.ViewHolder{
        private final TextView commentContent;
        private final TextView commentUser;
        private final TextView likesNum,child_commentNum;
        private final TextView date;
        private final ImageView userAvator;
        private final ImageView likePieceComment_button;
        private LinearLayout likepart;
        private final CardView jumpView;
        public ViewHolder(View view){
            super(view);
            userAvator=view.findViewById(R.id.comment_user_avator);
            commentUser = view.findViewById(R.id.commentUserText);
            commentContent = view.findViewById(R.id.commentContentText);
            date=view.findViewById(R.id.commentTimeText);
            child_commentNum=view.findViewById(R.id.ChildCommentNum);
            likePieceComment_button = view.findViewById(R.id.likePieceCommentButton);
            likepart=view.findViewById(R.id.likepart);
            likesNum = view.findViewById(R.id.likePieceCommentNum);
            jumpView=view.findViewById(R.id.comment_card);
        }
    }

    @NonNull
    @Override
    public bookCommentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_comment_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull bookCommentAdapter.ViewHolder holder, int position) {
        PieceComments comment = CommentsList.get(position);
        holder.commentContent.setText(comment.getContent());
        holder.commentUser.setText(comment.getUserName());
        holder.likesNum.setText(String.valueOf(comment.getLikesNum()));
        holder.child_commentNum.setText(String.valueOf(comment.getChildCommentNum()));
        holder.date.setText(comment.getDate());
        holder.userAvator.setImageBitmap(comment.getUserAvator());

        holder.jumpView.setOnClickListener(view -> {
            Toast.makeText(context,"评论详情，待开发...",Toast.LENGTH_SHORT).show();
            /*Intent intent=new Intent();
            intent.setClass(context, commentDetailActivity.class);
            context.startActivity(intent);*/
        });

        /*设置点赞按钮*/
        /*初始化*/
        if (Objects.equals(comment.getIf_liked(), "true")) {
            if_like_this_comment=1;
            holder.likePieceComment_button.setImageResource(R.drawable.like_thumb_up);
        } else {
            if_like_this_comment=0;
            holder.likePieceComment_button.setImageResource(R.drawable.thumb_up);
        }
        /*点赞事件*/
        for(int i=0;i<CommentsList.size();i++){
            like_comment_times.add(0);
        }
        holder.likepart.setOnClickListener(view1 -> {
            if (Objects.equals(comment.getIf_liked(), "true")) {
                if_like_this_comment=1;
                holder.likePieceComment_button.setImageResource(R.drawable.like_thumb_up);
            } else {
                if_like_this_comment=0;
                holder.likePieceComment_button.setImageResource(R.drawable.thumb_up);
            }
            like_comment_times.set(position, like_comment_times.get(position) + 1);
            if ((if_like_this_comment + like_comment_times.get(position)) % 2 == 0) {
                try {
                    CommentsList.get(position).bookcomment_like(act, 0);
                } catch (JSONException e) {
                    Tools.my_toast(act,"取消点赞失败");
                }
                holder.likePieceComment_button.setImageResource(R.drawable.thumb_up);
                holder.likesNum.setText(String.valueOf(comment.getLikesNum()));

            } else {
                try {
                    CommentsList.get(position).bookcomment_like(act, 1);
                } catch (JSONException e) {
                    Tools.my_toast(act,"点赞失败");
                }
                holder.likePieceComment_button.setImageResource(R.drawable.like_thumb_up);
                holder.likesNum.setText(String.valueOf(comment.getLikesNum()));
            }
        });
    }
    @Override
    public int getItemCount() {
        if(CommentsList==null) return 0;
        return CommentsList.size();
    }
    public void getActivity(Activity act) {
        this.act=act;
    }
}
