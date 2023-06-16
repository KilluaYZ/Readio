package cn.ruc.readio.userPageActivity;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import cn.ruc.readio.R;
import cn.ruc.readio.databinding.FragmentCollectionBinding;
import cn.ruc.readio.ui.works.Works;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
public class collectionFragment extends Fragment {
    private FragmentCollectionBinding binding;
    private ArrayList<Works> works = new ArrayList<Works>();
    static public Fragment collectionFrag;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCollectionBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        collectionFrag = this;
        setData();
        RecyclerView.LayoutManager layoutManager = new StaggeredGridLayoutManager(1,StaggeredGridLayoutManager.VERTICAL);
        RecyclerView recyclerView = binding.collectWorksBar;
        recyclerView.setLayoutManager(layoutManager);
        collectionAdapter collectAdapter = new collectionAdapter(getContext(),works);
        recyclerView.setAdapter(collectAdapter);
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    public void setData()
    {
        HttpUtil.getRequestWithTokenAsyn(getActivity(),"/works/pieces/collect/get", new ArrayList<>(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Tools.my_toast(getActivity(),"请求异常，加载不出来");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray data = jsonObject.getJSONArray("data");
                    for(int i = 0; i < data.length(); i++){
                        JSONObject datai = data.getJSONObject(i);
//                        JSONObject useri = datai.getJSONObject("user");
                        Works work = new Works();
                        work.setPieceTitle(datai.getString("title"));
                        work.setContent(datai.getString("content"));
                        work.setCollectsNum(datai.getInt("collect"));
                        work.setWorkID(datai.getInt("piecesId"));
//                        User user = new User(useri.getString("userName"),useri.getString("email"),useri.getString("phoneNumber"));
//                        work.setUser(user);
                        works.add(work);
                    }
                    if(getActivity() != null)
                    {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(getActivity() != null) {
                                    RecyclerView recyclerView = (RecyclerView) getActivity().findViewById(R.id.collectWorksBar);
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

}
