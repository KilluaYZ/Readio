package cn.ruc.readio.userPageActivity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;
import cn.ruc.readio.R;

public class tagAdapter extends BaseAdapter{
    private final Context context;
    private final onItemViewClickListener listener;
    private List<String> list;

    public tagAdapter(Context context, List<String> list, onItemViewClickListener listener){

        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    public Object getItem(int position){
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertview, ViewGroup parent) {
        ViewHolder holder = null;
        if(convertview == null){
            holder = new ViewHolder();
            convertview = LayoutInflater.from(context).inflate(R.layout.tag_item,null);
            holder.nameTv = convertview.findViewById(R.id.item_tagTv);
            convertview.setTag(holder);
        }else{
            holder = (ViewHolder) convertview.getTag();
        }
        holder.nameTv.setText(list.get(position));
        holder.nameTv.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(listener!=null){
                    listener.onItemViewClick(list.get(position));
                }
            }
        });
        return convertview;
    }
    class ViewHolder{
        TextView nameTv;
    }
    public interface onItemViewClickListener{
        void onItemViewClick(String name);
    }
    public void setNewList(List<String> list){
        this.list = list;
        notifyDataSetChanged();
    }
}
