package cn.ruc.readio.worksManageFragment;

import androidx.recyclerview.widget.RecyclerView;

import cn.ruc.readio.ui.works.Works;
import cn.ruc.readio.userPageActivity.ReEditActivity;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.util.Tools;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cn.ruc.readio.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class draftManageAdapter extends RecyclerView.Adapter<draftManageAdapter.ViewHolder>{
    private List<Works> WorksList;
    private draftManageFragment fragment;
    public draftManageAdapter(draftManageFragment fragment, List<Works> WorksList){
        this.fragment = fragment;
        this.WorksList = WorksList;
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView draftTitle;
        private TextView draftSeriesName;
        private TextView startTime;
        private TextView publishButton;
        private TextView deleteButton;
        private TextView editButton;

        public ViewHolder(View view){
            super(view);
            draftTitle = view.findViewById(R.id.manageDraftTitle);
//            workContent = view.findViewById(R.id.manageWorkContent);
            publishButton = view.findViewById(R.id.publishDraftButton);
            draftSeriesName = view.findViewById(R.id.manageDraftSeriesTitle);
            startTime = view.findViewById(R.id.startDraftTime);
            editButton = view.findViewById(R.id.editDraftButton);
            deleteButton = view.findViewById(R.id.deleteDraftButton);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_draft_manage,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                // 实际上点进每一个item都要传输作品的id
                Intent intent = new Intent(view.getContext(), ReEditActivity.class);
                intent.putExtra("piecesId", String.valueOf(WorksList.get(position).getWorkID()));
                intent.putExtra("piecesContent", String.valueOf(WorksList.get(position).getContent()));
                intent.putExtra("status","0");
                Log.d("pieceid", String.valueOf(WorksList.get(position).getWorkID()));
                startActivity(view.getContext(), intent, null);
            }
        });
        viewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                Works worki = WorksList.get(position);
                ArrayList<Pair<String,String>> queryParam = new ArrayList<>();
                queryParam.add(new Pair<>("piecesId",toString().valueOf(worki.getWorkID())));

                HttpUtil.getRequestWithTokenAsyn(draftManageFragment.draftManageFrag.getActivity(), "/works/delPieces", queryParam, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if(draftManageFragment.draftManageFrag.getActivity()!=null) {
                            Tools.my_toast(draftManageFragment.draftManageFrag.getActivity(), "删除草稿失败，请检查网络");
                        }
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if(draftManageFragment.draftManageFrag.getActivity()!=null) {
                            Tools.my_toast(draftManageFragment.draftManageFrag.getActivity(), "删除草稿成功！");
//                            mAdapter.notifyDataSetChanged();
                            fragment.refreshData();
                        }
                    }
                });
            }
        });
        viewHolder.publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                Works worki = WorksList.get(position);
                ArrayList<Pair<String,String>> queryParam = new ArrayList<>();
                queryParam.add(new Pair<>("piecesId",toString().valueOf(worki.getWorkID())));
                HttpUtil.getRequestWithTokenAsyn(draftManageFragment.draftManageFrag.getActivity(),"/works/changePiecesStatus", queryParam, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (draftManageFragment.draftManageFrag.getActivity() != null) {
                            Tools.my_toast(draftManageFragment.draftManageFrag.getActivity(), "发布作品失败，请检查网络");
                        }
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (draftManageFragment.draftManageFrag.getActivity() != null) {
                            Tools.my_toast(draftManageFragment.draftManageFrag.getActivity(), "发布作品成功");
                            fragment.refreshData();
                        }
                    }
                });
            }
        });
        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Works works = WorksList.get(position);
        holder.draftTitle.setText(works.getPieceTitle());
//        holder.workContent.setText(works.getContent());
        holder.draftSeriesName.setText("系列名：" + works.getSerialTitle());
        holder.startTime.setText("创建时间："+works.getPublishedTime());
    }

    @Override
    public int getItemCount() {
        return WorksList.size();
    }
}
