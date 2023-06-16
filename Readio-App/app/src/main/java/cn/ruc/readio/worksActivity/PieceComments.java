package cn.ruc.readio.worksActivity;

import android.app.Activity;
import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PieceComments {
    private int commentId;
    private String bookId;
    private String content;
    private int piecesId;
    private User user;
    private String date;
    private int likesNum = 0;
    private int childCommentNum;
    private String if_liked;
    private Boolean isYours;
    private User toUser;

    private Boolean liked = false;

    public Boolean getLiked() {
        return liked;
    }

    public void setLiked(Boolean liked) {
        this.liked = liked;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }

    public PieceComments(String content, int LikesNum, User user) {
        this.content = content;
        this.likesNum = LikesNum;
        this.user = user;
    }

    public PieceComments(){

    }

    public int getPiecesId() {
        return piecesId;
    }

    public void setPiecesId(int piecesId) {
        this.piecesId = piecesId;
    }

    public int getCommentId(){
        return commentId;
    }
    public String getContent(){
        return content;
    }
    public int getLikesNum(){
        return likesNum;
    }
    public int getChildCommentNum(){
        return childCommentNum;
    }
    public String getUserName(){return user.getUserName();}
    public Bitmap getUserAvator(){return user.getAvator();}
    public User getUser(){return user;}
    public String getDate(){return date;}
    public String getIf_liked(){return if_liked;}
    public String getBookId(){return bookId;}

    public void setCommentId(int id){
        this.commentId = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setLikesNum(int likesNum) {
        this.likesNum = likesNum;
    }
    public void setChildCommentNum(int childCommentNum) {
        this.childCommentNum = childCommentNum;
    }
    public void setIf_liked(String if_liked){this.if_liked=if_liked;}

    public void setDate(String date) {
        this.date = date;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public void setYours(Boolean yours) {
        isYours = yours;
    }

    public Boolean getYours() {
        return isYours;
    }

    public void bookcomment_like(Activity act, int like) throws JSONException {
        if(like==1){
            likesNum++;
        }else{
            likesNum--;
        }
        JSONObject json = new JSONObject();
        json.put("bookId",bookId);
        json.put("commentId",String.valueOf(commentId));
        json.put("like",like);
        HttpUtil.postRequestWithTokenJsonAsyn(act, "/app/book/"+bookId+"/comments/"+ commentId +"/update", json.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Tools.my_toast(act,"点赞失败，请检查网络");
                //Toast.makeText(context,"点赞失败，请检查网络",Toast.LENGTH_SHORT);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //Tools.my_toast(act,"点赞成功！");
            }
        });
    }

    public void fromJsonObj(JSONObject jsonObject) throws JSONException {
        if(jsonObject.has("commentId")){
            this.commentId = jsonObject.getInt("commentId");
        }

        if(jsonObject.has("content")){
            this.content = jsonObject.getString("content");
        }

        if(jsonObject.has("createTime")){
            this.date = jsonObject.getString("createTime");
        }
        if(jsonObject.has("user")){
            User tmp_user = new User();
            tmp_user.fromJSONObject(jsonObject.getJSONObject("user"));
            this.user = tmp_user;
        }
        if(jsonObject.has("toUser") && !jsonObject.get("toUser").toString().equals("null")){
            User tmp_user = new User();
            tmp_user.fromJSONObject(jsonObject.getJSONObject("toUser"));
            this.toUser = tmp_user;
        }
        if(jsonObject.has("isYours")){
            this.isYours = jsonObject.getBoolean("isYours");
        }
        if(jsonObject.has("liked")){
            this.liked = jsonObject.getBoolean("liked");
        }
        if(jsonObject.has("likes")){
            this.likesNum = jsonObject.getInt("likes");
        }
    }
}
