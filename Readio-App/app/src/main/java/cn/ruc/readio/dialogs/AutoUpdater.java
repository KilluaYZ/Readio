package cn.ruc.readio.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.ruc.readio.R;

public class AutoUpdater {
    // 下载安装包的网络路径
    private String apkUrl = "http://server.killuayz.top:9000/readio-apk/";
    protected String checkUrl = apkUrl + "output-metadata.json";

    // 保存APK的文件名
    private static final String saveFileName = "readio.apk";
    private static File apkFile;

    // 下载线程
    private Thread downLoadThread;
    private int progress;// 当前进度
    // 应用程序Context
    private Context mContext;
    // 是否是最新的应用,默认为false
    private boolean isNew = false;
    private boolean intercept = false;
    // 进度条与通知UI刷新的handler和msg常量
    private ProgressBar mProgress;
    private TextView txtStatus;

    private static final int DOWN_UPDATE = 1;
    private static final int DOWN_OVER = 2;
    private static final int SHOWDOWN = 3;

    public AutoUpdater(Context context) {
        mContext = context;
        apkFile = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), saveFileName);
    }

    public void ShowUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);


        builder.setTitle("软件版本更新");
        builder.setMessage("\n有最新的软件包，请下载并安装!\n");
        builder.setPositiveButton("立即下载", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ShowDownloadDialog();
            }
        });
        builder.setNegativeButton("以后再说", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xD41B85EF);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF5F5F5F);
    }

    private void ShowDownloadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("软件版本更新");
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.progress, null);
        mProgress = (ProgressBar) v.findViewById(R.id.progress);
        txtStatus = v.findViewById(R.id.txtStatus);
        builder.setView(v);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                intercept = true;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF5F5F5F);
        DownloadApk();
    }

    /**
     * 检查是否更新的内容
     */
    public void CheckUpdate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String localVersion = "1";
                try {
                    localVersion = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                String versionName = "1";
                String outputFile = "";
                String config = doGet(checkUrl);
                if (config != null && config.length() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Matcher m = Pattern.compile("\"outputFile\":\\s*\"(?<m>[^\"]*?)\"").matcher(config);

                        if (m.find()) {
                            outputFile = m.group("m");
                        }
                        m = Pattern.compile("\"versionName\":\\s*\"(?<m>[^\"]*?)\"").matcher(config);
                        if (m.find()) {
                            String v = m.group("m");
                            versionName = m.group("m").replace("v1.0.", "");
                        }
                    }
                }
//                if (Long.parseLong(localVersion) < Long.parseLong(versionName)) {
//                    apkUrl = apkUrl + outputFile;
//                    mHandler.sendEmptyMessage(SHOWDOWN);
//                } else {
//                    return;
//                }
                if (localVersion.compareTo(versionName) < 0) {
                    apkUrl = apkUrl + outputFile;
                    mHandler.sendEmptyMessage(SHOWDOWN);
                } else {
                    return;
                }
            }
        }).start();
    }

    /**
     * 从服务器下载APK安装包
     */
    public void DownloadApk() {
        downLoadThread = new Thread(DownApkWork);
        downLoadThread.start();
    }

    private Runnable DownApkWork = new Runnable() {
        @Override
        public void run() {
            URL url;
            try {
                url = new URL(apkUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                int length = conn.getContentLength();
                InputStream ins = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(apkFile);
                int count = 0;
                byte[] buf = new byte[1024];
                while (!intercept) {
                    int numread = ins.read(buf);
                    count += numread;
                    progress = (int) (((float) count / length) * 100);
                    // 下载进度
                    mHandler.sendEmptyMessage(DOWN_UPDATE);
                    if (numread <= 0) {
                        // 下载完成通知安装
                        mHandler.sendEmptyMessage(DOWN_OVER);
                        break;
                    }
                    fos.write(buf, 0, numread);
                }
                fos.close();
                ins.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 安装APK内容
     */
    public void installAPK() {
        try {
            if (!apkFile.exists()) {
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//安装完成后打开新版本
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 给目标应用一个临时授权
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//判断版本大于等于7.0
                //如果SDK版本>=24，即：Build.VERSION.SDK_INT >= 24，使用FileProvider兼容安装apk
                String packageName = mContext.getApplicationContext().getPackageName();
                String authority = new StringBuilder(packageName).append(".fileprovider").toString();
                Uri apkUri = FileProvider.getUriForFile(mContext, authority, apkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            mContext.startActivity(intent);
            android.os.Process.killProcess(android.os.Process.myPid());//安装完之后会提示”完成” “打开”。


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case SHOWDOWN:
                    ShowUpdateDialog();
                    break;
                case DOWN_UPDATE:
                    txtStatus.setText(progress + "%");
                    mProgress.setProgress(progress);
                    break;
                case DOWN_OVER:
                    Toast.makeText(mContext, "下载完毕", Toast.LENGTH_SHORT).show();
                    installAPK();
                    break;
                default:
                    break;
            }
        }

    };

    public static String doGet(String httpurl) {
        HttpURLConnection connection = null;
        InputStream is = null;
        BufferedReader br = null;
        String result = null;
        try {
            URL url = new URL(httpurl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.connect();
            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuffer sbf = new StringBuffer();
                String temp = null;
                while ((temp = br.readLine()) != null) {
                    sbf.append(temp);
                    sbf.append("\r\n");
                }
                result = sbf.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            connection.disconnect();
        }
        return result;
    }
}