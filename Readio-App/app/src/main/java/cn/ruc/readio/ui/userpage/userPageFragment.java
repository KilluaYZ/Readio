package cn.ruc.readio.ui.userpage;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import cn.ruc.readio.R;
import cn.ruc.readio.databinding.FragmentUserpageBinding;
import cn.ruc.readio.ui.userpage.follower.FollowerActivity;
import cn.ruc.readio.ui.userpage.login.LoginActivity;
import cn.ruc.readio.ui.userpage.subscribe.SubscriberActivity;
import cn.ruc.readio.ui.userpage.changeAvatar.changeAvatorActivity;
import cn.ruc.readio.userPageActivity.myCollectionActivity;
import cn.ruc.readio.userPageActivity.myLikesActivity;
import cn.ruc.readio.userPageActivity.mySettingsActivity;
import cn.ruc.readio.userPageActivity.newWorksActivity;
import cn.ruc.readio.userPageActivity.worksManageActivity;
import cn.ruc.readio.util.Auth;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class userPageFragment extends Fragment {

    private FragmentUserpageBinding binding;

    public ImageButton new_work_button;


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    Log.d(this.toString(),"修改userID");
                    binding.userID.setText((String)msg.obj);
                    break;
                case 2:
                    Log.d(this.toString(),"修改userName");
                    binding.userName.setText((String)msg.obj);
                    break;
                default:
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserpageBinding.inflate(inflater, container, false);
        Log.d("dtehaha", "onCreateView");

        View root = binding.getRoot();

        ImageButton settingsButton = binding.mySettingsButton;

        ImageButton manage_button = binding.workManageButton;
        TextView userName = binding.userName;
        if(getActivity() != null) {
            Auth.Token token1 = new Auth.Token(getActivity());
            if(token1.isEmpty())
            {
//                userName.setClickable(true);
                userName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(),LoginActivity.class);
                        startActivity(intent);
                    }
                });
            }
            else{
//                userName.setClickable(false);
                userName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
            }
        }

        settingsButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), mySettingsActivity.class);
                startActivity(intent);
            }
        });
        binding.newWorkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(this.toString(),"点击了 新建作品");
                Intent intent = new Intent(getContext(), newWorksActivity.class);
                startActivity(intent);
            }
        });

        manage_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), worksManageActivity.class);
                startActivity(intent);
            }
        });


        binding.myAvator.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), changeAvatorActivity.class);
                startActivity(intent);
            }
        });

        binding.myLikes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), myLikesActivity.class);
                startActivity(intent);
            }
        });

        binding.myCollectionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), myCollectionActivity.class);
                startActivity(intent);
            }
        });

        binding.stars.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SubscriberActivity.class);
                startActivity(intent);
            }
        });
        binding.followers.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FollowerActivity.class);
                startActivity(intent);
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        Auth.Token token = new Auth.Token(getActivity());
        if(token.isEmpty() && getActivity()!=null)
        {
            ((TextView)getActivity().findViewById(R.id.userName)).setText("点击登录 / 注册");
            ((ImageView)getActivity().findViewById(R.id.my_avator)).setImageResource(R.drawable.unlogged);
            ((TextView)getActivity().findViewById(R.id.userID)).setText("");
            ((TextView)getActivity().findViewById(R.id.userName)).setClickable(true);
        }
        super.onResume();
        Log.d("dtehaha", "onRe");
        getProfile();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("dtehaha", "onDes");
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("dtehaha", "onst");
    }

    private void getProfile(){
        if(getActivity() != null) {
            Auth.Token token = new Auth.Token(getActivity());
            if(token.isEmpty())
            {
                binding.userName.setText("点击登录/注册");
                binding.myAvator.setImageResource(R.drawable.unlogged);
            }
            HttpUtil.getRequestWithTokenAsyn(getActivity(), "/app/auth/profile", new ArrayList<>(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Looper.prepare();
                    Toast.makeText(getContext(), "获取信息失败！请检查网络连接", Toast.LENGTH_LONG).show();
                    Looper.loop();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(response.code() == 200){
                        try {
                            JSONObject responseJsonObject = new JSONObject(response.body().string()).getJSONObject("data").getJSONObject("userInfo");
                            String userId = String.valueOf(responseJsonObject.getInt("userId"));
                            String userName = responseJsonObject.getString("userName");
                            int followerNum = responseJsonObject.getInt("followerNum");
                            int subscribeNum = responseJsonObject.getInt("subscribeNum");
//                    handler.obtainMessage(1,userId);
//                    handler.obtainMessage(2,userName);
//                    Bitmap my_avamap = HttpUtil.getAvaSyn(responseJsonObject.getString("avator"));
//                    HttpUtil.getAvaAsyn(responseJsonObject.getString("avator"),binding.myAvator,getActivity());
                            if(getActivity() != null && binding.myAvator != null)
                            {
                                Tools.getImageBitmapAsyn(responseJsonObject.getString("avator"), binding.myAvator, getActivity());}
                            if(getActivity() != null && binding != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        binding.userID.setText("ID:" + userId);
                                        binding.userName.setText(userName);
                                        binding.followers.setText(String.valueOf(followerNum));
                                        binding.stars.setText(String.valueOf(subscribeNum));
//                            binding.myAvator.setImageBitmap(my_avamap);
                                    }
                                });
                            }
                            Log.d(this.toString(), "拿到profile数据");
                        } catch (JSONException | ParseException e) {
                            e.printStackTrace();
                            if(getActivity()!=null) {
                                Tools.my_toast(getActivity(), "解析profile信息失败");
                            }
                        }
                    }else{
                        if(getActivity()!=null)
                        {
                            Auth.Token token = new Auth.Token(getActivity());
                            token.clear();
                        }
                    }
                }
            });

        }
    }

}