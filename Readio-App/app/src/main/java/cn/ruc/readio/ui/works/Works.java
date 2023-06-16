package cn.ruc.readio.ui.works;
import android.app.Activity;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Works {
    private int workID;
    private String serialTitle;
    private String pieceTitle;
    private String content;
    private User user;
    private Date date;
    private int likesNum;
    private tags tag;
    private tags tag2;
    private int mylike;
    private int collectsNum;
    private int commentsNum;
    private String publishedTime;
    private int tagNum;

    public Works(String content, String serialTitle, String pieceTitle, User user) {
        this.content = content;
        this.pieceTitle = pieceTitle;
        this.serialTitle = serialTitle;
        this.user = user;
        this.mylike = 0;
        tagNum = 0;
    }

    public Works(){
        mylike = 0;
        tagNum = 0;
    }
    public String getSerialTitle(){
        return serialTitle;
    }
    public String getPieceTitle(){
        return pieceTitle;
    }
    public String getContent(){
        return content;
    }
    public int getLikesNum(){
        return likesNum;
    }

    public int getWorkID(){ return workID;}
    public tags getTag(){ return tag;}
    public tags getTag2(){ return tag2;}
    public String getWorkUser(){
        return user.getUserName();
    }
    public String getAvaId(){return user.getAvaID();}
    public int getTagNum(){return tagNum;}
    public User getUser() {return user;}

    public int getMylike(){return mylike;}
    public String getPublishedTime(){return publishedTime;}
    public int getCollectsNum(){return collectsNum;}
    public int getCommentsNum(){return commentsNum;}
    public void setWorkID(int id){
        this.workID = id;
    }
    public void setSerialTitle(String serialTitle) {
        this.serialTitle = serialTitle;
    }

    public void setPieceTitle(String pieceTitle) {
        this.pieceTitle = pieceTitle;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setLikesNum(int likesNum) {
        this.likesNum = likesNum;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public void setTag(tags tag){
        this.tag = tag;
    }
    public void setTag2(tags tag2){ this.tag2 = tag2;}
    public void setPublishedTime(String publishedTime){ this.publishedTime = publishedTime; }
    public void setCollectsNum(int collectsNum){this.collectsNum = collectsNum;}
    public void setCommentsNum(int commentsNum){this.commentsNum = commentsNum;}
    public void setTagNum(int num){ this.tagNum = num;}
    public void changeMyLike(){
        if (mylike==0){
            mylike = 1;
        }
        else{
            mylike = 0;
        }
    }
    public void addLike(Activity act, String pieceId){
        Log.d("lalala", "+1");
        likesNum++;
        ArrayList<Pair<String, String>> pieceid = new ArrayList<>();
        pieceid.add(Pair.create("piecesId",pieceId));
        HttpUtil.getRequestWithTokenAsyn(act, "/works/pieces/like/add", pieceid, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Tools.my_toast(act,"点赞失败，请检查网络");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
            }
        });
    }
    public void subLike(Activity act, String pieceId){

        likesNum--;
        ArrayList<Pair<String, String>> pieceid = new ArrayList<>();
        pieceid.add(Pair.create("piecesId",pieceId));
        HttpUtil.getRequestWithTokenAsyn(act, "/works/pieces/like/del", pieceid, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Tools.my_toast(act,"取消点赞失败，请检查网络");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
            }
        });
    }
}

