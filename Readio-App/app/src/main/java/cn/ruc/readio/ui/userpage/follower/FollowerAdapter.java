package cn.ruc.readio.ui.userpage.follower;

import android.app.Activity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.ruc.readio.R;
import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.util.HttpUtil;
import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FollowerAdapter extends RecyclerView.Adapter<FollowerAdapter.ViewHolder> {
    private List<User> FolloerList = null;
    private Activity activity = null;

    public FollowerAdapter(Activity activity,List<User> FolloerList){
        this.FolloerList = FolloerList;
        this.activity = activity;
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public CircleImageView avatarCircleImageView = null;
        public TextView userNameTextView = null;
        public TextView userIdTextView = null;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarCircleImageView = itemView.findViewById(R.id.follower_activity_user_avatar);
            userNameTextView = itemView.findViewById(R.id.follower_activity_user_name_text_view);
            userIdTextView = itemView.findViewById(R.id.follower_activity_user_id_text_view);
        }
    }

    @NonNull
    @Override
    public FollowerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.follower_user_item, parent, false);
        FollowerAdapter.ViewHolder viewHolder = new FollowerAdapter.ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FollowerAdapter.ViewHolder holder, int position) {
        User subscriber =  FolloerList.get(position);
        holder.userNameTextView.setText(subscriber.getUserName());
        holder.avatarCircleImageView.setImageBitmap(subscriber.getAvator());
        holder.userIdTextView.setText("ID: "+subscriber.getUserId());
    }

    @Override
    public int getItemCount() {
        return FolloerList.size();
    }
}
