package com.eli.voiceassist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.eli.voiceassist.R;
import com.eli.voiceassist.entity.VoiceEntity;

import java.util.List;
import java.util.Map;

/**
 * Created by zhanbo.zhang on 2018/4/8.
 */

public class SettingListAdapter extends BaseAdapter {

    private List<Map<String, Object>> data;
    private LayoutInflater inflater;
    private Context context;
    private OnItemClickListener listener;

    public SettingListAdapter(Context context, List<Map<String, Object>> data) {
        this.context = context;
        this.data = data;
        this.inflater = LayoutInflater.from(context);
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ItemGroup group;
        if (convertView == null) {
            group = new ItemGroup();
            convertView = inflater.inflate(R.layout.setting_list_item, null);
            group.image = convertView.findViewById(R.id.item_image);
            group.textTitle = convertView.findViewById(R.id.item_title);
            group.textSummary = convertView.findViewById(R.id.item_summary);
            group.switchButton = convertView.findViewById(R.id.item_switch);
            convertView.setTag(group);
        } else {
            group = (ItemGroup) convertView.getTag();
        }

        //set image
        int imageID = (Integer) data.get(position).get("image");
        if (imageID != 0)
            group.image.setBackground(context.getDrawable(imageID));
        else
            group.image.setVisibility(View.GONE);
        //set title
        group.textTitle.setText((String) data.get(position).get("title"));
        //set summary
        group.textSummary.setText((String) data.get(position).get("summary"));

        boolean hideSwitch = (boolean) data.get(position).get("hide");

        final View parentView = convertView;
        if (hideSwitch) {
            group.switchButton.setVisibility(View.GONE);
        } else {
            group.switchButton.setChecked(((String) data.get(position).get("summary")).equalsIgnoreCase(VoiceEntity.positive));
            group.switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (listener != null) {
                        listener.onItemClicked(parentView, position);
                    }
                    group.textSummary.setText(isChecked ? VoiceEntity.positive : VoiceEntity.negative);
                }
            });
        }
        convertView.setBackgroundResource(R.drawable.item_click_ripple);
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onItemClicked(parentView, position);
            }
        });
        return convertView;
    }

    class ItemGroup {
        public ImageView image;
        public TextView textTitle;
        public TextView textSummary;
        public Switch switchButton;
    }

    public interface OnItemClickListener {
        void onItemClicked(View parent, int position);
    }
}
