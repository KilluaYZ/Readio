package cn.ruc.readio.ui.shelf;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import java.util.Objects;

import cn.ruc.readio.R;

public class BookItem {
    private String BookName;
    private String BookID;
    private final String Author;
    private Bitmap Cover;
    private String CoverID;
    private String bookAbstract;
    private String likes;
    private String views;

    @NonNull
    public String getBookName() {
        return BookName;
    }
    public String getAuthor() {
        return Author;
    }
    public Bitmap getCover() {
        return Cover;
    }
    public String getCoverID() {
        return CoverID;
    }
    public String getBookID(){return BookID;}
    public String getBookAbstract(){
        if(bookAbstract==null){
            return "该书籍暂无简介~";
        }
        return bookAbstract;
    }
    public String getLikes(){return likes;}
    public String getViews(){return views;}

    public BookItem(String BookName, String Author, String coverID) {
        //this.img=img;
        this.BookName = BookName;
        this.Author=Author;
        this.CoverID =coverID;
    }
    public BookItem(String BookName,String Author){
        this.BookName = BookName;
        this.Author=Author;
    }

    public void setBookName(String BookName){
        this.BookName = BookName;
    }
    public void setAuthor(String Author){
        this.BookName = Author;
    }
    public void setCover(Bitmap Cover){
        this.Cover = Cover;
    }
    public void setCoverID(String CoverID){
        this.CoverID = CoverID;
    }
    public void setBookID(String BookID){
        this.BookID=BookID;
    }
    public void setBookAbstract(String bookAbstract){this.bookAbstract=bookAbstract;}
    public void setLikes(String likes){this.likes=likes;}
    public void setViews(String views){this.views=views;}


}

