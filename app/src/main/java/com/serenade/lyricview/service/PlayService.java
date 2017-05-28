package com.serenade.lyricview.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.serenade.lyricview.bean.Song;

public class PlayService extends Service {
    private MediaPlayer player;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new PlayBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle info = intent.getExtras();
        Song song = (Song) info.getSerializable("song");
        final String url = song.getM4a();

        initPlayer(url);
        return Service.START_STICKY;
    }

    public void stopPlayer() {
        if (player != null) {
            player.stop();
            player.reset();
            player.release();
            player = null;
        }
    }

    public void pausePlayer() {
        if (player.isPlaying())
            player.pause();
    }

    public void startPlayer() {
        if (player != null) {
            if (!player.isPlaying()) {
                player.start();
            }
        }
    }

    public void initPlayer(String url) {
        if (!TextUtils.isEmpty(url)) {
            if (player == null) {
                try {
                    //初始化播放器
                    player = new MediaPlayer();
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);// 设置媒体流类型
                    player.setDataSource(url);
                    player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            Intent broadcast = new Intent("startPlaying");
                            Bundle bundle = new Bundle();
                            int current_position = player.getCurrentPosition();
                            int duration = player.getDuration();
                            bundle.putInt("current_position", current_position);
                            bundle.putInt("duration", duration);
                            broadcast.putExtras(bundle);
                            sendBroadcast(broadcast);
                            mp.start();
                        }
                    });
                    player.prepareAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayer();
    }

    public class PlayBinder extends Binder {
        public void seekTo(int position) {
            if (player != null)
                player.seekTo(position);
        }
    }
}
