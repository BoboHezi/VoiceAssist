package com.eli.voiceassist.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eli.voiceassist.R;
import com.eli.voiceassist.util.Util;

import java.util.List;

/**
 * Created by zhanbo.zhang on 2018/4/4.
 */

public class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.MyViewHolder> {

    private List<String> datas;
    private Context context;
    private LayoutInflater inflater;

    public MyRecyclerAdapter(Context context, List<String> datas) {
        this.context = context;
        this.datas = datas;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.message_list_item, null);
        MyViewHolder viewHolder = new MyViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.outView.setBackgroundResource(R.drawable.message_item_shape_right);
        holder.messageTextView.setText(datas.get(position));
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        LinearLayout outView;
        public MyViewHolder(View view) {
            super(view);
            outView = view.findViewById(R.id.out_view);
            messageTextView = view.findViewById(R.id.message);
        }
    }
}
