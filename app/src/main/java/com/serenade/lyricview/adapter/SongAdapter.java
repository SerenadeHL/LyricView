package com.serenade.lyricview.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.serenade.lyricview.R;
import com.serenade.lyricview.bean.Song;

import java.util.List;


/**
 * Created by Serenade on 17/2/6.
 */

public class SongAdapter extends BaseAdapter {
    private List<Song> data;
    private Context context;

    public SongAdapter(Context context, List<Song> data) {
        this.data = data;
        this.context = context;
    }

    @Override
    public int getCount() {
        return data == null ? 0 : data.size();
    }

    @Override
    public Object getItem(int position) {
        return data == null ? null : data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
            holder = new ViewHolder();
            holder.songName = (TextView) convertView.findViewById(R.id.songName);
            holder.singer = (TextView) convertView.findViewById(R.id.singer);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Song song = data.get(position);
        holder.songName.setText(song.getSongname());
        holder.songName.setTag(song);
        holder.singer.setText(song.getSingername());
        return convertView;
    }

    class ViewHolder {
        TextView songName;
        TextView singer;
    }
}
