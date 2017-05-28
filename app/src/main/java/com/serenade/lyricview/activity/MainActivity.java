package com.serenade.lyricview.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.serenade.lyricview.utils.HttpUtils;
import com.serenade.lyricview.utils.Parser;
import com.serenade.lyricview.R;
import com.serenade.lyricview.bean.Song;
import com.serenade.lyricview.adapter.SongAdapter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, TextView.OnEditorActionListener {
    private Button search;
    private EditText song;
    private ListView list;
    private SongAdapter adapter;
    private List<Song> data;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        init();
    }

    private void initView() {
        song = (EditText) findViewById(R.id.song);
        search = (Button) findViewById(R.id.search);
        list = (ListView) findViewById(R.id.list);
    }

    private void initListener() {
        search.setOnClickListener(this);
        song.setOnEditorActionListener(this);
        list.setOnItemClickListener(this);
    }

    private void init() {
        data = new ArrayList<>();
        adapter = new SongAdapter(this, data);
        list.setAdapter(adapter);
    }

    private void getSongs() {
        String songName = song.getText().toString().trim();
        if (!TextUtils.isEmpty(songName)) {
            try {
                songName = URLEncoder.encode(songName, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            final String url = "http://route.showapi.com/213-1?showapi_appid=31490&keyword=" + songName + "&page=1&showapi_sign=274e07e744d7457bbc3e6c60682327e2";
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String json = HttpUtils.getString(url);
                    List<Song> songs = Parser.parse(json);
                    data.addAll(songs);
                    handler.sendEmptyMessage(1);
                }
            }).start();
        }
    }

    @Override
    public void onClick(View v) {
        getSongs();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        getSongs();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Song song = (Song) (view.findViewById(R.id.songName)).getTag();
        Intent intent = new Intent(this, PlayActivity.class);
        Bundle info = new Bundle();
        info.putSerializable("song", song);
        intent.putExtras(info);
        startActivity(intent);
    }
}
