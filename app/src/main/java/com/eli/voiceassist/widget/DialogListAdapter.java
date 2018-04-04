package com.eli.voiceassist.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eli.voiceassist.R;
import com.eli.voiceassist.util.Util;

import java.util.List;
import java.util.Map;

/**
 * Created by zhanbo.zhang on 2018/3/28.
 */
public class DialogListAdapter extends BaseAdapter {

    private List<Map<String, Object>> items;
    private LayoutInflater inflater;
    private Animation animation;
    private Context context;
    private int size;
    private boolean isAllAnimation;
    private boolean isAnimation = true;
    private boolean firstFlag = true;

    public DialogListAdapter(Context context, List<Map<String, Object>> items) {
        this.items = items;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.animation = AnimationUtils.loadAnimation(context, R.anim.dialog_item_enter);
    }

    private class Item {
        public LinearLayout outView;
        public TextView messageText;
    }

    @Override
    public int getCount() {
        return size;
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void notifyDataSetChanged() {
        size = items.size();
        firstFlag = true;
        super.notifyDataSetChanged();
    }

    public void setAllAnimation() {
        isAllAnimation = true;
        //Log.i("elifli", "all");
    }

    public void setAnimation(boolean isAnimation) {
        this.isAnimation = isAnimation;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Item item;
        if (convertView == null) {
            item = new Item();
            convertView = inflater.inflate(R.layout.message_list_item, null);
            item.outView = convertView.findViewById(R.id.out_view);
            item.messageText = convertView.findViewById(R.id.message);
            convertView.setTag(item);
        } else {
            item = (Item) convertView.getTag();
        }

        boolean isUser = (boolean) items.get(position).get("role");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.bottomMargin = Util.dip2px(context, 15);
        lp.topMargin = Util.dip2px(context, 15);
        if (isUser) {
            lp.rightMargin = Util.dip2px(context, 15);
            lp.gravity = Gravity.RIGHT;
            item.outView.setBackgroundResource(R.drawable.message_item_shape_right);
        } else {
            lp.leftMargin = Util.dip2px(context, 15);
            lp.gravity = Gravity.LEFT;
            item.outView.setBackgroundResource(R.drawable.message_item_shape_left);
        }
        item.outView.setLayoutParams(lp);
        item.messageText.setText((String) items.get(position).get("message"));

        if (isAnimation)
            convertView.startAnimation(animation);
        /*if (isAnimation) {
            if (!isAllAnimation) {
                if (position == size - 1) {
                    if (!firstFlag) {
                        convertView.startAnimation(animation);
                    }
                    firstFlag = false;
                }
            } else {
                convertView.startAnimation(animation);
            }
        }*/
        return convertView;
    }
}
