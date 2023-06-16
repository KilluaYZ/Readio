package cn.ruc.readio.userPageActivity;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.ruc.readio.ui.works.Works;
import cn.ruc.readio.util.HttpUtil;
import cn.ruc.readio.R;
import cn.ruc.readio.util.Tools;
import cn.ruc.readio.worksActivity.readWorksActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class collectionAdapter extends RecyclerView.Adapter<collectionAdapter.ViewHolder>{
    private List<Works> WorksList;
    public collectionAdapter(Context context, List<Works> WorksList)
    {

        this.WorksList = WorksList;
    }
    class ViewHolder extends RecyclerView.ViewHolder{

        private TextView collectWorkTitle;
        private TextView collectWorkContent;
        private TextView collectNum;
        private TextView withdraw;

        public ViewHolder(View view)
        {
            super(view);
            Log.d("RECD","ViewHolder");
            collectWorkTitle = view.findViewById(R.id.collectionWorkTitle);
            collectWorkContent = view.findViewById(R.id.collectionWorkContent);
            collectNum = view.findViewById(R.id.collectionWorkcollectNum);
            withdraw = view.findViewById(R.id.collectionWorkButton);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("RECD","onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection, parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.collectWorkContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                // 实际上点进每一个item都要传输作品的id
                Intent intent = new Intent(view.getContext(), readWorksActivity.class);
                intent.putExtra("extra_data",String.valueOf(WorksList.get(position).getWorkID()));
                Log.d("pieceid",String.valueOf(WorksList.get(position).getWorkID()));
                startActivity(view.getContext(), intent,null);
            }
        });

        viewHolder.withdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = viewHolder.getAdapterPosition();
                ArrayList<Pair<String, String>> pieceid = new ArrayList<>();
                pieceid.add(Pair.create("piecesId",toString().valueOf(WorksList.get(position).getWorkID())));
                HttpUtil.getRequestWithTokenAsyn(collectionFragment.collectionFrag.getActivity(), "/works/pieces/collect/del", pieceid, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Tools.my_toast(collectionFragment.collectionFrag.getActivity(),"取消点赞失败，请检查网络");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                    }
                });
                viewHolder.withdraw.setText("已取消");
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d("RECD","onBindViewHolder");
        Works works = WorksList.get(position);
        holder.collectWorkTitle.setText(works.getPieceTitle());
        holder.collectWorkContent.setText(works.getContent());
        holder.collectNum.setText("收藏数："+toString().valueOf(works.getCollectsNum()));
    }

    @Override
    public int getItemCount() {
        Log.d("RECD","getItemCount");
        return WorksList.size();
    }
}
