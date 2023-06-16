package cn.ruc.readio.bookReadActivity;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

public class bookReadAdapter extends PagerAdapter {
    private List<View> newViewList = new ArrayList<>();

    public List<View> getNewViewList() {
        return newViewList;
    }

    void setNewViewList(List<View> newViewList){
        this.newViewList = newViewList;
        notifyDataSetChanged();
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
//        container.removeView((View) newViewList.get(position));
        container.removeView((View)object);
    }
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        if(newViewList.isEmpty()){
            return super.instantiateItem(container,position);
        }
        else{
            View view = newViewList.get(position);
            ViewParent parent = view.getParent();
            if(parent != null)
            {
                ((ViewPager)parent).removeView(view);
            }
            container.addView(newViewList.get(position),0);
            return newViewList.get(position);}
    }

    @Override
    public int getCount() {
        if (newViewList.size() > 2) {
//            return Integer.MAX_VALUE;
            return 10000;
        } else {
            return newViewList.size();
        }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }
}

