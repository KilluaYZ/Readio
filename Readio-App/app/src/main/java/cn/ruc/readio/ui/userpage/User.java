package cn.ruc.readio.ui.userpage;

import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private String userId;
    private String userName;
    private String eMail;
    private String phoneNumber;
    private Bitmap avator;
    private String avaID;

    private Boolean isSubscribed;

    public User(){};
    public User(String userName,String eMail, String phoneNumber){
        this.userName = userName;
        this.eMail = eMail;
        this.phoneNumber = phoneNumber;
    }

    public String geteMail() {
        return eMail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getUserName() {
        return userName;
    }

    public Bitmap getAvator() {
        return avator;
    }
    public String getAvaID(){return avaID;}

    public void setAvator(Bitmap avator) {
        this.avator = avator;
    }

    public void seteMail(String eMail) {
        this.eMail = eMail;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    public void setAvaID(String id){this.avaID = id;}

    public void setUserId(String userId){
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public Boolean getSubscribed() {
        return isSubscribed;
    }

    public void setSubscribed(Boolean subscribed) {
        isSubscribed = subscribed;
    }

    public void fromJSONObject(JSONObject obj) throws JSONException {
        if(obj.has("userName")){
            setUserName(obj.getString("userName"));
        }

        if(obj.has("userId")){
            setUserId(obj.getString("userId"));
        }

        if(obj.has("id")){
            setUserId(obj.getString("id"));
        }

        if(obj.has("avator")){
            setAvaID(obj.getString("avator"));
        }

        if(obj.has("email")){
            seteMail(obj.getString("email"));
        }
        if(obj.has("phoneNumber")){
            setPhoneNumber(obj.getString("phoneNumber"));
        }
        if(obj.has("isSubscribed")){
            setSubscribed(obj.getInt("isSubscribed") == 0 ? false : true);
        }
    }

}
