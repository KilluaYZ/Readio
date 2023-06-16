package cn.ruc.readio.ui.userpage.subscribe;

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

public class SubscriberAdapter extends RecyclerView.Adapter<SubscriberAdapter.ViewHolder> {
    private List<User> SubscriberList = null;
    private Activity activity = null;

    public SubscriberAdapter(Activity activity,List<User> SubscriberList){
        this.SubscriberList = SubscriberList;
        this.activity = activity;
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public CircleImageView avatarCircleImageView = null;
        public TextView userNameTextView = null;
        public CardView followBtn = null;
        public TextView followBtnText = null;
        public TextView userIdTextView = null;
        public Boolean unfollowed = false;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarCircleImageView = itemView.findViewById(R.id.subscribe_activity_user_avatar);
            userNameTextView = itemView.findViewById(R.id.subscribe_activity_user_name_text_view);
            followBtn = itemView.findViewById(R.id.subsscribe_activity_follow_btn);
            followBtnText = itemView.findViewById(R.id.subsscribe_activity_follow_btn_text);
            userIdTextView = itemView.findViewById(R.id.subscribe_activity_user_id_text_view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subscriber_user_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User subscriber =  SubscriberList.get(position);
        holder.userNameTextView.setText(subscriber.getUserName());
        holder.avatarCircleImageView.setImageBitmap(subscriber.getAvator());
        holder.userIdTextView.setText("ID: "+subscriber.getUserId());
        holder.followBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.unfollowed){
                    holder.followBtnText.setText("关注");
                    ArrayList<Pair<String, String>> queryParam = new ArrayList<>();
                    queryParam.add(new Pair<>("userId", subscriber.getUserId()));
                    HttpUtil.getRequestWithTokenAsyn(activity, "/user/subscribe/del", queryParam, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                        }
                    });
                }else{
                    holder.followBtnText.setText("取消关注");
                    ArrayList<Pair<String, String>> queryParam = new ArrayList<>();
                    queryParam.add(new Pair<>("userId", subscriber.getUserId()));
                    HttpUtil.getRequestWithTokenAsyn(activity, "/user/subscribe/add", queryParam, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {

                        }
                    });
                }
                holder.unfollowed = !holder.unfollowed;
            }
        });
    }

    @Override
    public int getItemCount() {
        return SubscriberList.size();
    }
}
