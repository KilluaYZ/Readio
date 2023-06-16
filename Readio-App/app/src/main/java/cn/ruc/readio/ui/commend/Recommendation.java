package cn.ruc.readio.ui.commend;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import java.util.Objects;

public class Recommendation {

    private final String quote;
    private final String author;
    private final String book_name;
    private final int book_id;
    private Bitmap pic;

    public void setPic(Bitmap pic){
        this.pic=pic;
    }
    @NonNull
    public String getQuote() {
        return quote;
    }
    public String getAuthor() {
        return author;
    }
    public String getBookName() {
        return book_name;
    }
    public int getBookId() {
        return book_id;
    }
    public Bitmap getPic() {
        return pic;
    }
    public String getSource() {

        if(Objects.equals(author, "null") && !Objects.equals(book_name, "null")) {
            return "《"+book_name+"》";
        }
        else if (Objects.equals(book_name, "null") && !Objects.equals(author, "null")) {
            return author;
        }
        else if(Objects.equals(book_name, "null") && Objects.equals(author, "null")){
            return "null";
        }
        else return author +"《"+book_name+"》";
    }

    public Recommendation(String quote, String book_name,String author,int book_id) {

        this.quote = quote;
        this.author = author;
        this.book_name =book_name;
        this.book_id=book_id;
    }

}
