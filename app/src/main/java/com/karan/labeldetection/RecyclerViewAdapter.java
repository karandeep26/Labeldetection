package com.karan.labeldetection;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by stpl on 3/24/2017.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private ArrayList<Model> data;
    private Context mContext;
    RecyclerViewAdapter(ArrayList<Model> data, Context context) {
        this.data = data;
        mContext=context;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent,
                false);
        mContext = parent.getContext();
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Model data=this.data.get(position);
        holder.textView.setText(data.getTag());
        if(data.isSelected()){
            holder.textView.setBackground(ContextCompat.getDrawable(mContext,R.drawable.shape_selected));
        }
        else{
            holder.textView.setBackground(ContextCompat.getDrawable(mContext,R.drawable.shape_unselected));
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ViewHolder(View itemView) {
            super(itemView);
            textView= (TextView) itemView.findViewById(R.id.tag);
        }
    }


}
