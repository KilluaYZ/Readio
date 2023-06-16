package cn.ruc.readio.worksActivity;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import cn.ruc.readio.R;
import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.userPageActivity.ReadioActivity;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class pieceCommentAdapter extends RecyclerView.Adapter<pieceCommentAdapter.ViewHolder> {
    private final List<PieceComments> CommentsList;
    private int like_comment_times = 0;
    private readWorksActivity activity;
    private ClipData mClipData;
    private ClipboardManager mClipboardManager;

    public pieceCommentAdapter(readWorksActivity activity, List<PieceComments> CommentsList){
        this.CommentsList = CommentsList;
        this.activity = activity;
    }
    static class ViewHolder extends RecyclerView.ViewHolder{
        private  TextView commentContent;
        private  TextView commentUser;
        private  TextView likesNum;
        private  TextView replyToUser;
        private CircleImageView userAvatar;

        private TextView commentTime;
        private ImageView likePieceCommentButton;
        private  ImageView reply_send_btn;

        private TextView delete_comment_btn;
        public ViewHolder(View view){
            super(view);
            commentContent = view.findViewById(R.id.commentContentText);
            commentUser = view.findViewById(R.id.commentUserText);
            likesNum = view.findViewById(R.id.likePieceCommentNum);
            userAvatar = view.findViewById(R.id.comment_user_avator);
            replyToUser = view.findViewById(R.id.comment_to_userText);
            commentTime = view.findViewById(R.id.commentTimeText);
            likePieceCommentButton = view.findViewById(R.id.likePieceCommentButton);
            reply_send_btn = view.findViewById(R.id.reply_send_btn);
            delete_comment_btn = view.findViewById(R.id.delete_comment_btn);
        }
    }

    @NonNull
    @Override
    public pieceCommentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_piece_comment,parent,false);
        pieceCommentAdapter.ViewHolder viewHolder = new ViewHolder(view);
        ImageView likePieceComment_button = view.findViewById(R.id.likePieceCommentButton);
        mClipboardManager = (ClipboardManager) mBottomSheetDialog.bottomSheetDialog.getContext().getSystemService(CLIPBOARD_SERVICE);
        likePieceComment_button.setOnClickListener(view1 -> {
//                like_comment_times++;
//                if(like_comment_times%2==0){
//                likePieceComment_button.setImageResource(R.drawable.likecomment);}
//                else {
//                    likePieceComment_button.setImageResource(R.drawable.likedcomment);
//                }
            likePieceComment_button.setImageResource(R.drawable.likecomment);

        });
        viewHolder.commentContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                String aComment = (String) viewHolder.commentContent.getText();
                mClipData = ClipData.newPlainText("Simple text",aComment);
                mClipboardManager.setPrimaryClip(mClipData);
                Toast.makeText(view.getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull pieceCommentAdapter.ViewHolder holder, int position) {
        PieceComments comment = CommentsList.get(position);
        holder.commentContent.setText(comment.getContent());
        holder.commentUser.setText(comment.getUserName());
        holder.likesNum.setText(String.valueOf(comment.getLikesNum()));
        holder.commentTime.setText(comment.getDate());
        if(comment.getToUser() != null){
            User toUser = comment.getToUser();
            String toUserText = "  @"+toUser.getUserName();
            holder.replyToUser.setText(toUserText);
        }else{
            holder.replyToUser.setText("");
        }

        try {
            Tools.getImageBitmapAsyn(comment.getUser().getAvaID(), holder.userAvatar, activity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if(comment.getLiked()){
            holder.likePieceCommentButton.setImageResource(R.drawable.likedcomment);
        }else{
            holder.likePieceCommentButton.setImageResource(R.drawable.likecomment);
        }

        holder.likePieceCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(comment.getLiked()){
                    //现在已经喜欢了，准备取消
                    comment.setLikesNum(comment.getLikesNum()-1);
                    comment.setLiked(false);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.recyclerView.getAdapter().notifyDataSetChanged();
                        }
                    });

                    ArrayList<Pair<String, String>> queryParam = new ArrayList<Pair<String, String>>();
                    queryParam.add(Pair.create("commentId", String.valueOf(comment.getCommentId())));
                    HttpUtil.getRequestWithTokenAsyn(activity, "/works/pieces/comments/like/del", queryParam, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Tools.my_toast(activity, "点赞失败，请检查网络连接");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                        }
                    });
                }else{
                    comment.setLikesNum(comment.getLikesNum()+1);
                    comment.setLiked(true);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.recyclerView.getAdapter().notifyDataSetChanged();
                        }
                    });

                    //准备点赞
                    ArrayList<Pair<String, String>> queryParam = new ArrayList<Pair<String, String>>();
                    queryParam.add(Pair.create("commentId", String.valueOf(comment.getCommentId())));
                    HttpUtil.getRequestWithTokenAsyn(activity, "/works/pieces/comments/like/add", queryParam, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Tools.my_toast(activity, "点赞失败，请检查网络连接");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                        }
                    });
                }
            }
        });
        
        holder.reply_send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startReplyMode(comment);
            }
        });

        if(comment.getYours()){
            holder.delete_comment_btn.setVisibility(View.VISIBLE);
        }else{
            holder.delete_comment_btn.setVisibility(View.GONE);
        }

        holder.delete_comment_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Pair<String, String>> queryParam = new ArrayList<>();
                queryParam.add(Pair.create("commentId", String.valueOf(comment.getCommentId())));
                HttpUtil.getRequestWithTokenAsyn(activity, "/works/pieces/comments/del", queryParam, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Tools.my_toast(activity, "删除失败，请检查网络连接");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        activity.refreshCommentData();
                    }
                });
            }
        });


    }
    @Override
    public int getItemCount() {
        return CommentsList.size();
    }

}
