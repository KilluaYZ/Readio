package cn.ruc.readio.entity;

import android.graphics.Bitmap;

public class UserAvatarHistory {
    private String userId = null;
    private String fileId = null;
    private Bitmap userAvatar = null;

    public String getUserId() {
        return userId;
    }

    public String getFileId() {
        return fileId;
    }

    public Bitmap getUserAvatar() {
        return userAvatar;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setUserAvatar(Bitmap userAvatar) {
        this.userAvatar = userAvatar;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
