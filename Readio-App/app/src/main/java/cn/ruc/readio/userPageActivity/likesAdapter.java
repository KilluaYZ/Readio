package cn.ruc.readio.userPageActivity;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.ruc.readio.ui.works.Works;
import cn.ruc.readio.R;
import cn.ruc.readio.worksActivity.readWorksActivity;


public class likesAdapter extends RecyclerView.Adapter<likesAdapter.ViewHolder>{
    private List<Works> WorksList;
    public likesAdapter(Context context, List<Works> WorksList)
    {
        this.WorksList = WorksList;
    }
    class ViewHolder extends RecyclerView.ViewHolder{

        private TextView likedWorkTitle;
        private TextView likedWorkContent;
        private TextView likeNum;
        private TextView withdraw;

        public ViewHolder(View view)
        {
            super(view);
            Log.d("RECD","ViewHolder");
            likedWorkTitle = view.findViewById(R.id.likeWorkTitle);
            likedWorkContent = view.findViewById(R.id.likeWorkContent);
            likeNum = view.findViewById(R.id.likesWorkLikeNum);
            withdraw = view.findViewById(R.id.likesWorkButton);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("RECD","onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R .layout.item_likes, parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.likedWorkContent.setOnClickListener(new View.OnClickListener() {
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
                WorksList.get(position).subLike(likesFragment.likesFrag.getActivity(),toString().valueOf(WorksList.get(position).getWorkID()));
                viewHolder.withdraw.setText("已取消");
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d("RECD","onBindViewHolder");
        Works works = WorksList.get(position);
        holder.likedWorkTitle.setText(works.getPieceTitle());
        holder.likedWorkContent.setText(works.getContent());
        holder.likeNum.setText("点赞数："+toString().valueOf(works.getLikesNum()));
//        holder.author.setText(works.getWorkUser());
    }

    @Override
    public int getItemCount() {
        Log.d("RECD","getItemCount");
        return WorksList.size();
    }
}