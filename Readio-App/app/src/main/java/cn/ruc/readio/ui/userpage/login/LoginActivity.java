package cn.ruc.readio.ui.userpage.login;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import cn.ruc.readio.MainActivity;
import cn.ruc.readio.util.Auth;
import cn.ruc.readio.util.Tools;
import eightbitlab.com.blurview.BlurView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;

import cn.ruc.readio.R;
import eightbitlab.com.blurview.RenderScriptBlur;
import kotlin.jvm.internal.Intrinsics;

public class LoginActivity extends AppCompatActivity {
//    private ActivityLoginBinding binding;

    private boolean isLogin = true;
    private FragmentTransaction transaction;
    private FragmentManager manager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
//        binding = ActivityLoginBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
        View decorView = getWindow().getDecorView();
        ViewGroup rootView = (ViewGroup) decorView.findViewById(R.id.login_activity_view);
        fullScreen(this);
        Drawable windowBackground = decorView.getBackground();
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) BlurView blurView = (BlurView) findViewById(R.id.blurView);
            blurView.setupWith(rootView, new RenderScriptBlur(this))
            .setBlurRadius(6F);


        manager = getSupportFragmentManager();
        transaction = manager.beginTransaction();
        transaction.add(R.id.login_register_form_layout, new LoginFormFragment());
        transaction.commit();

        TextView changeView = (TextView) findViewById(R.id.login_activity_topbar_change_view);
        changeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLayout();
            }
        });

    }

    public void changeLayout(){
        Log.d("isLogin", toString().valueOf(isLogin));
        if(isLogin){
            loadRegisterForm();
        }else {
            loadLoginForm();
        }
        isLogin = !isLogin;
    }

    private void loadLoginForm(){
        transaction = manager.beginTransaction();
        transaction.setCustomAnimations(
                R.animator.slide_right_in,
                R.animator.slide_left_out,
                R.animator.slide_left_in,
                R.animator.slide_right_out
        );
        transaction.replace(R.id.login_register_form_layout, new LoginFormFragment());
        TextView updateView = (TextView) findViewById(R.id.login_activity_topbar_change_view);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateView.setText("register");
            }
        });
        transaction.commit();
    }

    private void loadRegisterForm(){
        transaction = manager.beginTransaction();
        transaction.setCustomAnimations(
                R.animator.slide_right_in,
                R.animator.slide_left_out,
                R.animator.slide_left_in,
                R.animator.slide_right_out
        );
        transaction.replace(R.id.login_register_form_layout, new RegisterFormFragment());

        TextView updateView = (TextView) findViewById(R.id.login_activity_topbar_change_view);
        updateView.setText("login");
        transaction.commit();
    }



    private void fullScreen(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
                Window window = activity.getWindow();
                View decorView = window.getDecorView();
                //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
                int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(option);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
            } else {
                Window window = activity.getWindow();
                WindowManager.LayoutParams attributes = window.getAttributes();
                int flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
                attributes.flags |= flagTranslucentStatus;
                window.setAttributes(attributes);
            }
        }
    }

}