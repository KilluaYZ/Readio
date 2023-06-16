package cn.ruc.readio;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import cn.ruc.readio.databinding.ActivityMainBinding;
import cn.ruc.readio.dialogs.AutoUpdater;
import cn.ruc.readio.util.HttpUtil;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    static public MainActivity mainAct;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //去除默认标题栏
        setSupportActionBar(binding.toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_userpage, R.id.navigation_works, R.id.navigation_shelf, R.id.navigation_commend).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        //初始化HttpUtil
        String tmpUrl = HttpUtil.readHttpHost(this);
        if(tmpUrl != null && !tmpUrl.isEmpty()){
            if(tmpUrl.equals("killuayz.top")){
                HttpUtil.setBaseUrl_Tencent(this);
            }else{
                HttpUtil.setBaseUrl_550w(this);
            }
        }else{
            HttpUtil.setBaseUrl_Tencent(this);
        }

        AutoUpdater autoUpdater = new AutoUpdater(this);
        autoUpdater.CheckUpdate();


    }
}