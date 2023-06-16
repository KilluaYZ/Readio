package cn.ruc.readio.ui.userpage.changeAvatar;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import cn.ruc.readio.R;
import cn.ruc.readio.entity.UserAvatarHistory;
import cn.ruc.readio.ui.userpage.subscribe.SubscriberAdapter;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChangeAvatarAdapter extends RecyclerView.Adapter<ChangeAvatarAdapter.ViewHolder> {
    private List<UserAvatarHistory> UserAvatarList = null;
    private Activity activity = null;
    public ChangeAvatarAdapter(Activity activity, List<UserAvatarHistory> UserAvatarList){
        this.activity = activity;
        this.UserAvatarList = UserAvatarList;
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView avatarImageView = null;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.change_avatar_activity_img_view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_avatar, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserAvatarHistory userAvatarHistory = UserAvatarList.get(position);
        holder.avatarImageView.setImageBitmap(userAvatarHistory.getUserAvatar());
        holder.avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject jsonObj = new JSONObject();
                try {
                    jsonObj.put("fileId", userAvatarHistory.getFileId());
                    HttpUtil.postRequestWithTokenJsonAsyn(activity, "/app/auth/avatar/update", jsonObj.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Tools.my_toast(activity, "更换失败，请检查网络连接");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try{
                                if(response.code() == 200){
                                    Tools.my_toast(activity, "更换成功");
                                    ((changeAvatorActivity) activity).refreshData();
//                                    changeAvatorActivity.MyHandler handler = new changeAvatorActivity.MyHandler((changeAvatorActivity) activity);
//                                    handler.sendEmptyMessage(0);
                                }else{
                                    String msg = new JSONObject(response.body().string()).getString("msg");
                                    Tools.my_toast(activity, msg);
                                }
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return UserAvatarList.size();
    }
}
