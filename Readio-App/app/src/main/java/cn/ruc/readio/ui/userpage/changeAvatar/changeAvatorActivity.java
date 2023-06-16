package cn.ruc.readio.ui.userpage.changeAvatar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import cn.ruc.readio.R;

import cn.ruc.readio.databinding.ActivityChangeAvatorBinding;
import cn.ruc.readio.entity.UserAvatarHistory;
import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.util.BitmapBase64;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import de.hdodenhof.circleimageview.CircleImageView;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class changeAvatorActivity extends AppCompatActivity {

    private List<UserAvatarHistory> userAvatarHistoryList = new ArrayList<>();

    private RecyclerView recyclerView = null;
    private CircleImageView curAvatarImageView = null;

    private ImageView topViewBackground = null;

    private Bitmap dataBitMap = null;

    private  User currentUser = new User();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_avator);

        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        recyclerView = (RecyclerView) findViewById(R.id.activity_change_avator_bottom_recycle_view);
        curAvatarImageView = (CircleImageView) findViewById(R.id.activity_change_avatar_user_cur_avatar);
        topViewBackground = (ImageView) findViewById(R.id.activity_change_avatar_top_view_bg);

        Tools.randomGetImgAsyn(changeAvatorActivity.this, topViewBackground);

        BlurView blurView = (BlurView) findViewById(R.id.activity_change_avatar_blurView);
        View decorView = getWindow().getDecorView();
        ViewGroup rootView = (ViewGroup) decorView.findViewById(R.id.activity_change_avator_top_view);
        blurView.setupWith(rootView, new RenderScriptBlur(this))
                .setBlurRadius(6F);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        recyclerView.setLayoutManager(layoutManager);
        ChangeAvatarAdapter adapter = new ChangeAvatarAdapter(this, userAvatarHistoryList);
        recyclerView.setAdapter(adapter);

//        final MyHandler handler = new MyHandler(this);

//        addImageView = (ImageView) findViewById(R.id.imageView2);
//        avatorImageView = (ImageView) findViewById(R.id.imageView);

//        Tools.randomGetImgAsyn(this, avatorImageView);

        curAvatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = choosePicture();
                startActivityForResult(intent, 2);
            }
        });

        refreshData();
//        Button button = (Button) findViewById(R.id.uploadButton);
//        EditText nameEdit = (EditText) findViewById(R.id.nameEdit);
//        EditText typeEdit = (EditText) findViewById(R.id.typeEdit);

//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mtoast("图片上传中，请稍等");
//
//
    }

    public void refreshData(){
        HttpUtil.getRequestWithTokenAsyn(this, "app/auth/avatar/get", new ArrayList<>(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Tools.my_toast(changeAvatorActivity.this, "请求失败，请检查网络连接");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try{
                    if(response.code() == 200){
                        userAvatarHistoryList.clear();
                        JSONArray dataArray = new JSONObject(response.body().string()).getJSONArray("data");
                        for(int i = 0;i < dataArray.length();++i){
                            JSONObject obj = dataArray.getJSONObject(i);
                            String fileId = obj.getString("fileId");
                            String userId = obj.getString("userId");
                            UserAvatarHistory history = new UserAvatarHistory();
                            history.setUserId(userId);
                            history.setFileId(fileId);
                            userAvatarHistoryList.add(history);
                        }

                        for(int i = 0;i < userAvatarHistoryList.size();++i){
                            UserAvatarHistory history = userAvatarHistoryList.get(i);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Bitmap userAvatarBitmap = Tools.getImageBitmapSyn(changeAvatorActivity.this, history.getFileId());
                                        history.setUserAvatar(userAvatarBitmap);
                                        changeAvatorActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                recyclerView.getAdapter().notifyDataSetChanged();
                                            }
                                        });
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    } catch (ParseException e) {
                                        throw new RuntimeException(e);
                                    }

                                }
                            }).start();
                        }
                    }else{
                        String msg = new JSONObject(response.body().string()).getString("msg");
                        Tools.my_toast(changeAvatorActivity.this, msg);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        HttpUtil.getRequestWithTokenAsyn(this, "app/auth/profile", new ArrayList<>(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Tools.my_toast(changeAvatorActivity.this, "请求失败，请检查网络连接");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if(response.code() == 200){
                        JSONObject userInfoObj = new JSONObject(response.body().string()).getJSONObject("data").getJSONObject("userInfo");
                        currentUser.fromJSONObject(userInfoObj);
                        Tools.getImageBitmapAsyn(currentUser.getAvaID(), curAvatarImageView, changeAvatorActivity.this);
                    }else{
                        String msg = new JSONObject(response.body().string()).getString("msg");
                        Tools.my_toast(changeAvatorActivity.this, msg);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }



    public Intent choosePicture() {
        if (Build.VERSION.SDK_INT >=30) {// Android 11 (API level 30)
            return new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            return Intent.createChooser(intent, null);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 2){
            if(data != null){
                Log.d(this.toString(),"成功从图库取到data");
                Uri uri = data.getData();
                curAvatarImageView.setImageURI(uri);
                try {
                    dataBitMap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                    String lastPathSegment = uri.getLastPathSegment();
                    int pointIdx = lastPathSegment.lastIndexOf(".");
                    String fileName = null;
                    String fileType = null;
                    if(pointIdx != -1){
                        fileName = lastPathSegment.substring(0,pointIdx);
                        fileType = lastPathSegment.substring(pointIdx+1,lastPathSegment.length());
                    }else{
                        fileName = lastPathSegment;
                        fileType = "default";
                    }
                    JSONObject postBody = new JSONObject();
                    postBody.put("fileName", fileName);
                    postBody.put("fileType", fileType);
                    postBody.put("fileContent", BitmapBase64.bitmapToBase64(dataBitMap));
                    HttpUtil.postRequestWithTokenJsonAsyn(changeAvatorActivity.this,"/app/auth/avatar/add", postBody.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Tools.my_toast(changeAvatorActivity.this, "上传失败，请检查网络连接");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if(response.code() == 200){
                                Tools.my_toast(changeAvatorActivity.this, "上传成功");
                                refreshData();
                            }else{
                                String msg = null;
                                try {
                                    msg = new JSONObject(response.body().string()).getString("msg");
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                Tools.my_toast(changeAvatorActivity.this, msg);
                            }
                        }
                    });

                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


//    public static class MyHandler extends Handler {
//
//        //弱引用持有HandlerActivity , GC 回收时会被回收掉
//        private WeakReference<changeAvatorActivity> weakReference;
//
//        public MyHandler(changeAvatorActivity activity) {
//            this.weakReference = new WeakReference(activity);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            changeAvatorActivity activity = weakReference.get();
//            super.handleMessage(msg);
//            if (null != activity) {
//                //执行业务逻辑
//                activity.refreshData();
//            }
//        }
//    }
}