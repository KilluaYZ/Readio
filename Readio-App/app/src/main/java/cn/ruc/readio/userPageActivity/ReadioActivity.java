package cn.ruc.readio.userPageActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import cn.ruc.readio.R;
import cn.ruc.readio.util.Tools;

public class ReadioActivity extends AppCompatActivity {

    private ClipData mClipData;
    private ClipboardManager mClipboardManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readio);
        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ImageView exitReadioInfo = (ImageView) findViewById(R.id.exitReadioInfo);
        ConstraintLayout Weibo = (ConstraintLayout) findViewById(R.id.Weibo);
        ConstraintLayout Tele = (ConstraintLayout) findViewById(R.id.Tele);
        ConstraintLayout Mail = (ConstraintLayout) findViewById(R.id.Mail);
        TextView mailText = (TextView) findViewById(R.id.myMail);
        TextView version = (TextView) findViewById(R.id.Version);
        try {
            version.setText(this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
        }
        exitReadioInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Weibo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://weibo.com/ruc75"));
                startActivity(intent);
            }
        });

        Tele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:19800380215"));
                startActivity(intent);
            }
        });

        Mail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mail = (String) mailText.getText();
                mClipData = ClipData.newPlainText("Simple text",mail);
                mClipboardManager.setPrimaryClip(mClipData);
                Tools.my_toast(ReadioActivity.this,"复制成功！");
            }
        });
    }
}