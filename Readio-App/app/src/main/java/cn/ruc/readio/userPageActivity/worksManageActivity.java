package cn.ruc.readio.userPageActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import cn.ruc.readio.R;
import cn.ruc.readio.worksManageFragment.draftManageFragment;
import cn.ruc.readio.worksManageFragment.publishedManageFragment;

public class worksManageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_works_manage);
        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        TextView publishedZone_button = (TextView) findViewById(R.id.publishedManageButton);
        TextView draftZone_button = (TextView) findViewById(R.id.draftManageButton);
        ImageView exitWorksManage_button = (ImageView) findViewById(R.id.exitManagementButton);
        publishedManageFragment pub_fragment = new publishedManageFragment();
        replaceFragment(pub_fragment);
        exitWorksManage_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        publishedZone_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Log.d("color","halo");
                    publishedZone_button.setTextColor(0xD41B85EF);
                    draftZone_button.setTextColor(0xD8555554);
                    publishedManageFragment pub_fragment_new = new publishedManageFragment();
                    replaceFragment(pub_fragment_new);
                }
        });
        draftZone_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    publishedZone_button.setTextColor(0xD8555554);
                    draftZone_button.setTextColor(0xD41B85EF);
                    draftManageFragment dra_fragment_new = new draftManageFragment();
                    replaceFragment(dra_fragment_new);
                                    }

        });
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.workManageBar,fragment);
        transaction.commit();
    }
}