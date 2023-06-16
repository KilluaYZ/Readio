package cn.ruc.readio.ui.works;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import cn.ruc.readio.R;
import cn.ruc.readio.databinding.FragmentWorksBinding;
import cn.ruc.readio.ui.userpage.User;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class worksFragment extends Fragment {

    static public Fragment workFrag;
    public int refreshTimes = -1;

    private FragmentWorksBinding binding;
    User user = new User("zyy","123456","123456");
    ArrayList<Works> works = new ArrayList<Works>();
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentWorksBinding.inflate(inflater, container, false);
        workFrag = this;
        ImageView searchButton = binding.searchButton;
        View root = binding.getRoot();
        RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL);
        RecyclerView recyclerView = binding.worksColumn;
        recyclerView.setLayoutManager(layoutManager);
        WorkAdapter workAdapter = new WorkAdapter(getContext(),works);
        recyclerView.setAdapter(workAdapter);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            private int lastVisibleItemPosition = 0;
            private Boolean isSlidingToLast = false;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                StaggeredGridLayoutManager manager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                manager.invalidateSpanAssignments(); //防止第一行到顶部有空白
                int currentScrollState = newState;
                int visibleItemCount = manager.getChildCount();
                int totItemCount = manager.getItemCount();
                if(isSlidingToLast && visibleItemCount > 0 && currentScrollState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItemPosition >= totItemCount - 1){
                    refreshData();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(dy > 0){
                    StaggeredGridLayoutManager manager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                    int[] lastPositions = null;
                    lastPositions = new int[manager.getSpanCount()];
                    manager.findLastVisibleItemPositions(lastPositions);
                    lastVisibleItemPosition = findMax(lastPositions);
                    isSlidingToLast = true;
                }else{
                    isSlidingToLast = false;
                }
            }
        });
        binding.edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(binding.edittext.getText().length()==0)
                {
                    works.clear();
                    refreshData();
                }
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshSearchData();
            }
        });
        return root;
    }

    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {

        super.onResume();
        refreshTimes = -1;
        refreshData();
    }

    public void refreshData(){
        refreshTimes++;
        ArrayList<Pair<String,String>> queryParam = new ArrayList<>();
        queryParam.add(Pair.create("mode","recommend"));
        queryParam.add(Pair.create("queryTimes",toString().valueOf(refreshTimes)));
        HttpUtil.getRequestWithTokenAsyn(getActivity(),"/works/getPiecesBrief", queryParam, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(getActivity()!=null)
                {
                    Tools.my_toast(getActivity(),"请求异常，加载不出来");
                }

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray data = jsonObject.getJSONArray("data");
                    for(int i = 0; i < data.length(); i++){
                        JSONObject datai = data.getJSONObject(i);
                        JSONObject useri = datai.getJSONObject("user");
                        Works work = new Works();
                        work.setPieceTitle(datai.getString("title"));
                        work.setContent(datai.getString("content"));
                        work.setLikesNum(datai.getInt("likes"));
                        work.setWorkID(datai.getInt("piecesId"));
                        if(datai.getInt("isLiked")==1)
                        {
                            work.changeMyLike();
                        }
                        User user = new User(useri.getString("userName"),useri.getString("email"),useri.getString("phoneNumber"));
                        user.setAvaID(useri.getString("avator"));
                        tags tagi = new tags();
                        tags tagj = new tags();
                        if(datai.has("tag")){
                            JSONArray tagList = datai.getJSONArray("tag");
                            work.setTagNum(tagList.length());
                            if(tagList.length() == 1 ){
                                JSONObject tagObj = (JSONObject)tagList.get(0);
                                String tagConent = tagObj.getString("content");
                                int tagID = tagObj.getInt("tagId");
                                int tagLinkedTimes = tagObj.getInt("linkedTimes");
                                tagi.setContent(tagConent);
                                tagi.setLinkedTimes(tagLinkedTimes);
                                tagi.setTagId(tagID);
                                work.setTag(tagi);
                            }
                            if(tagList.length() >= 2 ){
                                JSONObject tagObj = (JSONObject)tagList.get(0);
                                String tagConent = tagObj.getString("content");
                                int tagID = tagObj.getInt("tagId");
                                int tagLinkedTimes = tagObj.getInt("linkedTimes");
                                tagi.setContent(tagConent);
                                tagi.setLinkedTimes(tagLinkedTimes);
                                tagi.setTagId(tagID);
                                work.setTag(tagi);

                                tagObj = (JSONObject) tagList.get(1);
                                String tagConent1 = tagObj.getString("content");
                                int tagID1 = tagObj.getInt("tagId");
                                int tagLinkedTimes1 = tagObj.getInt("linkedTimes");
                                tagj.setContent(tagConent1);
                                tagj.setLinkedTimes(tagLinkedTimes1);
                                tagj.setTagId(tagID1);
                                work.setTag2(tagj);
                            }
                        }
                        work.setUser(user);
                        works.add(work);

                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for(int i = 0;i < works.size(); ++i){
//                                Bitmap pic = HttpUtil.getAvaSyn(works.get(i).getUser().getAvaID());
                                Bitmap pic = null;
                                try {
                                    if(getActivity() != null) {
                                        pic = Tools.getImageBitmapSyn(getActivity(), works.get(i).getUser().getAvaID());
                                    }
                                    } catch (IOException | JSONException | ParseException e) {
                                    if(getActivity()!=null) {
                                        Tools.my_toast(getActivity(), "图片加载出错啦！");
                                    }
                                }
                                works.get(i).getUser().setAvator(pic);
                                Log.d("workadpter","需要更新");
                                if(getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(binding != null) {
                                                binding.worksColumn.getAdapter().notifyDataSetChanged();
                                            }
                                        }
                                    });
                                }
                            }

                        }
                    }).start();
                    if(getActivity() != null)
                    {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(getActivity() != null) {
                                    RecyclerView recyclerView = (RecyclerView) getActivity().findViewById(R.id.works_column);
                                    if (recyclerView != null) {
                                        recyclerView.getAdapter().notifyDataSetChanged();
                                    }
                                }
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void refreshSearchData(){

        String mode = "search";
        String keyword = binding.edittext.getText().toString();
        ArrayList<Pair<String,String>> queryParam = new ArrayList<>();
        queryParam.add(Pair.create("mode",mode));
        queryParam.add(Pair.create("keyword",keyword));
        HttpUtil.getRequestAsyn("/works/getPiecesBrief", queryParam, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(getActivity()!=null) {
                    Tools.my_toast(getActivity(), "api启用失败");
                }
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject jsonObject1 = new JSONObject(response.body().string());
                    JSONArray data = jsonObject1.getJSONArray("data");
                    works.clear();
                    for(int i = 0; i < data.length(); i++){
                        JSONObject datai = data.getJSONObject(i);
                        JSONObject useri = datai.getJSONObject("user");
                        Works work = new Works();
                        work.setPieceTitle(datai.getString("title"));
                        work.setContent(datai.getString("content"));
                        work.setLikesNum(datai.getInt("likes"));
                        work.setWorkID(datai.getInt("piecesId"));
                        if(datai.getInt("isLiked")==1)
                        {
                            work.changeMyLike();
                        }
                        User user = new User(useri.getString("userName"),useri.getString("email"),useri.getString("phoneNumber"));
                        user.setAvaID(useri.getString("avator"));
                        tags tagi = new tags();
                        tags tagj = new tags();
                        if(datai.has("tag")){
                            JSONArray tagList = datai.getJSONArray("tag");
                            work.setTagNum(tagList.length());
                            if(tagList.length() == 1 ){
                                JSONObject tagObj = (JSONObject)tagList.get(0);
                                String tagConent = tagObj.getString("content");
                                int tagID = tagObj.getInt("tagId");
                                int tagLinkedTimes = tagObj.getInt("linkedTimes");
                                tagi.setContent(tagConent);
                                tagi.setLinkedTimes(tagLinkedTimes);
                                tagi.setTagId(tagID);
                                work.setTag(tagi);
                            }
                            if(tagList.length() >= 2 ){
                                JSONObject tagObj = (JSONObject)tagList.get(0);
                                String tagConent = tagObj.getString("content");
                                int tagID = tagObj.getInt("tagId");
                                int tagLinkedTimes = tagObj.getInt("linkedTimes");
                                tagi.setContent(tagConent);
                                tagi.setLinkedTimes(tagLinkedTimes);
                                tagi.setTagId(tagID);
                                work.setTag(tagi);

                                tagObj = (JSONObject) tagList.get(1);
                                String tagConent1 = tagObj.getString("content");
                                int tagID1 = tagObj.getInt("tagId");
                                int tagLinkedTimes1 = tagObj.getInt("linkedTimes");
                                tagj.setContent(tagConent1);
                                tagj.setLinkedTimes(tagLinkedTimes1);
                                tagj.setTagId(tagID1);
                                work.setTag2(tagj);
                            }
                        }
                        work.setUser(user);
                        works.add(work);

                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for(int i = 0;i < works.size(); ++i){
//                                Bitmap pic = HttpUtil.getAvaSyn(works.get(i).getUser().getAvaID());
                                Bitmap pic = null;
                                try {
                                    if(getActivity() != null) {
                                        pic = Tools.getImageBitmapSyn(getActivity(), works.get(i).getUser().getAvaID());
                                    }
                                } catch (IOException | JSONException | ParseException e) {
                                    if(getActivity()!=null) {
                                        Tools.my_toast(getActivity(), "图片加载出错啦！");
                                    }
                                }
                                works.get(i).getUser().setAvator(pic);
                                Log.d("workadpter","需要更新");
                                if(getActivity() != null) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(binding != null) {
                                                binding.worksColumn.getAdapter().notifyDataSetChanged();
                                            }
                                        }
                                    });
                                }
                            }

                        }
                    }).start();
                    if(getActivity() != null)
                    {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(getActivity() != null) {
                                    RecyclerView recyclerView = (RecyclerView) getActivity().findViewById(R.id.works_column);
                                    if (recyclerView != null) {
                                        recyclerView.getAdapter().notifyDataSetChanged();
                                    }
                                }
                            }
                        });
                    }
                } catch (JSONException e) {
                    if(getActivity()!=null) {
                        Tools.my_toast(getActivity(), "搜索失败，请检查网络");
                    }

                }
            }
        });
    }
}