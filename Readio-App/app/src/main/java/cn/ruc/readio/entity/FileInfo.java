package cn.ruc.readio.entity;

import java.util.Date;

public class FileInfo {
    private String fileId;
    private String fileName;
    private String fileType;
    private String filePath;
    private Date createTime;
    private Date visitTime;

    private byte[] content;

    public Date getCreateTime() {
        return createTime;
    }

    public Date getVisitTime() {
        return visitTime;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setVisitTime(Date visitTime) {
        this.visitTime = visitTime;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

}
