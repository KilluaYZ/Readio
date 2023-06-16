package cn.ruc.readio.worksManageFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import cn.ruc.readio.databinding.FragmentPublishedBinding;
import cn.ruc.readio.ui.works.Works;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.worksManageFragment.publishedManageFragment;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class publishedManageFragment extends Fragment {
    private FragmentPublishedBinding binding;
    public static Fragment worksManageFrag;
    ArrayList<Works> works = new ArrayList<Works>();
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState){
        binding = FragmentPublishedBinding.inflate(inflater, container,false);
//        View view = inflater.inflate(R.layout.fragment_published,container);
        worksManageFrag = this;
        View view = binding.getRoot();
        refreshData();
        RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(1,StaggeredGridLayoutManager.VERTICAL);
//        RecyclerView recyclerView = (RecyclerView) getActivity().findViewById(R.id.publishedManageBar);
        RecyclerView recyclerView = binding.publishedManageBar;
        recyclerView.setLayoutManager(layoutManager);
        worksManageAdapter works_manage_Adapter = new worksManageAdapter(this,works);
        recyclerView.setAdapter(works_manage_Adapter);
        return view;
    }

    public void refreshData(){
        HttpUtil.getRequestWithTokenAsyn(getActivity(),"/works/getUserPiecesList",new ArrayList<>(),new Callback(){
            @Override
            public void onFailure(Call call, IOException e) {
                mtoast("请求异常");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try{
                    works.clear();
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray data = jsonObject.getJSONArray("data");
                    for(int i = 0; i < data.length(); i++){
                        JSONObject datai = data.getJSONObject(i);
                        if(datai.getString("status").equals("1")) {
                            Works work = new Works();
                            work.setWorkID(Integer.parseInt(datai.getString("piecesId")));
                            work.setPieceTitle(datai.getString("title"));
                            work.setContent(datai.getString("content"));
                            work.setLikesNum(datai.getInt("likes"));
                            Log.d("data__", datai.getString("title"));
                            work.setCollectsNum(datai.getInt("collect"));
//                            work.setCommentsNum(datai.getInt("comment"));
                            work.setSerialTitle(datai.getJSONObject("series").getString("seriesName"));
                            work.setPublishedTime(datai.getString("updateTime").substring(0, 10));
                            works.add(work);
                        }
                    }
                    if(getActivity()!=null)
                    {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.publishedManageBar.getAdapter().notifyDataSetChanged();
                        }
                    });}
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void  mtoast(String msg){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(),msg,Toast.LENGTH_LONG).show();
            }
        });
    }
}
