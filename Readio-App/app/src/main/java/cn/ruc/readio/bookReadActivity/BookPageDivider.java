package cn.ruc.readio.bookReadActivity;

import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import java.util.ArrayList;

public class BookPageDivider {
    book mbook;
    private ArrayList<String> pagesList = new ArrayList<>();
    private String content = "";
    private int numbersPerPage = 0;

    public BookPageDivider(book mbook, int numbersPerPage){
        this.mbook = mbook;
        ArrayList<Pair<String, String>> chapter =  mbook.getContent();
        chapter.forEach((item) -> {
            content += item.second;
        });
        this.numbersPerPage = numbersPerPage;
        splitPages();
    }

    public ArrayList<String> getPagesList() {
        return pagesList;
    }

    public void splitPages(){
        int maxPageSize = (int)(this.content.length() / this.numbersPerPage);
//        Log.d("hahaha", "content length = "+String.valueOf(this.content));
//        Log.d("hahaha", "maxPageSize = "+String.valueOf(maxPageSize));

        for(int i = 0;i < maxPageSize;++i){
            this.pagesList.add(this.content.substring(i* numbersPerPage, (i+1) * numbersPerPage));
        }
        this.pagesList.add(this.content.substring(maxPageSize*numbersPerPage));
    }


}
