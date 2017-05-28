package com.serenade.lyricview.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.serenade.lyricview.utils.HttpUtils;
import com.serenade.lyricview.utils.Parser;
import com.serenade.lyricview.service.PlayService;
import com.serenade.lyricview.R;
import com.serenade.lyricview.bean.Song;
import com.serenade.lyricview.widget.LyricView;

public class PlayActivity extends AppCompatActivity {
    private static LyricView lyric_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_play);

        lyric_view = (LyricView) findViewById(R.id.lyric_view);
        Bundle bundle = getIntent().getExtras();
        Song song = (Song) bundle.getSerializable("song");
        final String url = "http://route.showapi.com/213-2?musicid=" + song.getSongid() + "&showapi_appid=31490&showapi_sign=274e07e744d7457bbc3e6c60682327e2";
        new Thread(new Runnable() {
            @Override
            public void run() {
                String json = HttpUtils.getString(url);
                String lyric = Parser.parseLyric(json);
                lyric_view.setLyric(lyric);
            }
        }).start();

        Intent intent = new Intent(this, PlayService.class);
        intent.putExtras(bundle);
        startService(intent);


        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                final PlayService.PlayBinder binder = (PlayService.PlayBinder) service;
                lyric_view.setOnIndicatorPlayListener(new LyricView.IndicatorListener() {
                    @Override
                    public void onPlayClick(int position) {
                        binder.seekTo(position);
                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, BIND_AUTO_CREATE);
    }

    public static class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int current_position = bundle.getInt("current_position");
            int duration = bundle.getInt("duration");
            lyric_view.setDuration(duration);
            lyric_view.setCurrentPosition(current_position);
            lyric_view.start();
        }
    }
}
