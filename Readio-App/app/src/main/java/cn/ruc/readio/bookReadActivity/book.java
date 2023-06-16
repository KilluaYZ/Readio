package cn.ruc.readio.bookReadActivity;

import android.util.Pair;

import java.util.ArrayList;


public class book {
    private String bookName;
    private String authorName;
    private String bookAbstract;
    private String bookId;
    private int size;
    private int progress;
    private ArrayList<Pair<String, String>> content;

    public book(){}

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    public void setBookAbstract(String bookAbstract){
        this.bookAbstract=bookAbstract;
    }
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    public void setSize(int size) {
        this.size = size;
    }
    public void setContent(ArrayList<Pair<String, String>> content){
        this.content=content;
    }
    public void setProgress(int page)
    {
        progress = page;
    }
    public String getBookName(){return bookName;}
    public String getAuthorName(){return authorName;}
    public int getProgress()
    {
        return progress;
    }
    public String getBookId()
    {
        return bookId;
    }

    public ArrayList<Pair<String, String>> getContent() {
        return content;
    }
    public String getChapterName(int i){
        return content.get(i).first;
    }
    public String getChapter(int i){
        return content.get(i).second;
    }
}
