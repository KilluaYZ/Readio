package cn.ruc.readio.util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.SimpleFormatter;

import cn.ruc.readio.MainActivity;
import cn.ruc.readio.entity.FileInfo;

public class FileReader {
    private Activity activity;
    FileReader(Activity activity){
        this.activity = activity;
    }

    private static FileInfoDBHelper fileInfoDBHelperInstance = null;
    public synchronized FileInfoDBHelper getFileInfoDBHelperInstance(){
        if(fileInfoDBHelperInstance == null){
            fileInfoDBHelperInstance = new FileInfoDBHelper(activity,"readio.db",null,1);
        }
        return fileInfoDBHelperInstance;
    }

    class FileInfoDBHelper extends SQLiteOpenHelper{

        public FileInfoDBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version){
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE `file_info` (\n" +
                    "  `fileId` char(64) NOT NULL,\n" +
                    "  `fileName` varchar(256)  NOT NULL,\n" +
                    "  `fileType` varchar(64)  NOT NULL,\n" +
                    "  `filePath` varchar(128)  DEFAULT NULL,\n" +
                    "  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,\n" +
                    "  `visitTime` datetime DEFAULT CURRENT_TIMESTAMP\n" +
                    ")";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public void insert(FileInfo fileInfo) {
        FileInfoDBHelper fileInfoDBHelper = getFileInfoDBHelperInstance();
        SQLiteDatabase db = fileInfoDBHelper.getWritableDatabase();

        try{

            writeFileContent(activity, fileInfo);
            ContentValues values = new ContentValues();
            values.put("fileId", fileInfo.getFileId());
            values.put("fileName", fileInfo.getFileName());
            values.put("fileType", fileInfo.getFileType());
            values.put("filePath", fileInfo.getFilePath());
//        values.put("createTime", fileInfo.getCreateTime().getTime());
//        values.put("visitTime", fileInfo.getVisitTime().getTime());
            db.insert("file_info", null, values);
        } catch (Exception e) {
            //只要出错了就返回false
            e.printStackTrace();
        }

    }

    public void delete(String fileId){
        FileInfoDBHelper fileInfoDBHelper = getFileInfoDBHelperInstance();
        SQLiteDatabase db = fileInfoDBHelper.getWritableDatabase();
        String[] fileIds = new String[]{fileId};
        db.delete("file_info", "fileId=?", fileIds);
    }

    public void updateVisitTime(String fileId){
        FileInfoDBHelper fileInfoDBHelper = getFileInfoDBHelperInstance();
        SQLiteDatabase db = fileInfoDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        Date visitTime = new Date(System.currentTimeMillis());
        values.put("visitTime", visitTime.toString());
        String[] fileIds = new String[]{fileId};
        db.update("file_info", values, "fileId=?", fileIds);
    }

    public FileInfo getFileInfoByFileId(String fileId) throws IOException, ParseException {
        FileInfoDBHelper fileInfoDBHelper = getFileInfoDBHelperInstance();
        SQLiteDatabase db = fileInfoDBHelper.getReadableDatabase();
        String[] fileIds = new String[]{fileId};
        if(fileId != null && !fileIds.equals("null")){
            Cursor cursor = db.query("file_info", new String[]{"fileId", "fileName", "fileType", "filePath"},"fileId=?", fileIds, null, null, null);
            while(cursor.moveToNext()){
                FileInfo fileInfo = new FileInfo();
                int coloumnId = cursor.getColumnIndex("fileId");
                if(coloumnId != -1){
                    fileInfo.setFileId(cursor.getString(coloumnId));
                }

                coloumnId = cursor.getColumnIndex("fileName");
                if(coloumnId != -1){
                    fileInfo.setFileName(cursor.getString(coloumnId));
                }

                coloumnId = cursor.getColumnIndex("filePath");
                if(coloumnId != -1){
                    fileInfo.setFilePath(cursor.getString(coloumnId));
                }

                coloumnId = cursor.getColumnIndex("fileType");
                if(coloumnId != -1){
                    fileInfo.setFileType(cursor.getString(coloumnId));
                }

//            coloumnId = cursor.getColumnIndex("createTime");
//            if(coloumnId != -1){
//                fileInfo.setCreateTime(Tools.formatter.parse(cursor.getString(coloumnId)));
//            }
//
//            coloumnId = cursor.getColumnIndex("visitTime");
//            if(coloumnId != -1){
//                fileInfo.setCreateTime(Tools.formatter.parse(cursor.getString(coloumnId)));
//            }
            fileInfo.setContent(readFileContent(activity, fileInfo));
            return fileInfo;
        }}

        return null;
    }

    private void writeFileContent(Activity activity,FileInfo fileInfo) throws IOException {
        if(activity == null){
            return ;
        }
        String dirPath = activity.getFilesDir().getPath()+"/"+fileInfo.getFilePath();
        String filePath = activity.getFilesDir().getPath()+"/"+fileInfo.getFilePath() + "/" +fileInfo.getFileId()+"."+fileInfo.getFileType();
        File dir = new File(dirPath);
        File file = new File(filePath);
        if(!dir.exists()){
            dir.mkdirs();
        }
        if(!file.exists()){
            file.createNewFile();
        }

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(fileInfo.getContent());
        fos.close();
    }

    private byte[] readFileContent(Activity activity, FileInfo fileInfo) throws IOException {
        if(activity == null){
            return null;
        }
        String dataPath = activity.getFilesDir().getPath()+ "/" + fileInfo.getFilePath()+"/"+fileInfo.getFileId()+"."+fileInfo.getFileType();
//        FileInputStream fileInputStream = activity.openFileInput(dataPath);
        File inputFile = new File(dataPath);
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        byte[] data = toByteArray(fileInputStream);
        fileInputStream.close();
        return data;
    }

    private byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }



}
