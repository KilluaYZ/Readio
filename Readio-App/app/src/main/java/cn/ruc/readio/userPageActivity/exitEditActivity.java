package cn.ruc.readio.userPageActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import cn.ruc.readio.R;

public class exitEditActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exit_edit);
        TextView exitDirect_button = (TextView) findViewById(R.id.exitDirectButton);
        TextView saveEdit_button = (TextView) findViewById(R.id.saveEditButton);
        TextView nothing_button = (TextView) findViewById(R.id.backEditButton);
        exitDirect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editWorkActivity.editworkActivity.finish();
                finish();
            }
        });
        saveEdit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                发送内容至服务器，更新数据库
                 */
                Toast.makeText(exitEditActivity.this, "已存入草稿箱", Toast.LENGTH_SHORT).show();
                editWorkActivity.editworkActivity.finish();
                finish();
            }
        });
        nothing_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}