package com.serenade.lyricview.utils;

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import com.serenade.lyricview.bean.Song;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Serenade on 17/2/6.
 */

public class Parser {
    public static List<Song> parse(String json) {
        List<Song> data = null;
        if (!TextUtils.isEmpty(json)) {
            try {
                data = new ArrayList<>();
                JSONObject object = new JSONObject(json);
                object = object.getJSONObject("showapi_res_body");
                object = object.getJSONObject("pagebean");
                JSONArray array = object.getJSONArray("contentlist");
                for (int i = 0; i < array.length(); i++) {
                    object = array.getJSONObject(i);
                    Song song = new Song();
                    song.setM4a(object.getString("m4a"));
                    Log.e("media_mid:", object.getString("media_mid"));
                    song.setMedia_mid(object.getString("media_mid"));
                    song.setSongid(object.getString("songid"));
                    song.setSingerid(object.getString("singerid"));
                    song.setAlbumname(object.getString("albumname"));
                    song.setDownUrl(object.getString("downUrl"));
                    song.setSingername(object.getString("singername"));
                    song.setSongname(object.getString("songname"));
                    song.setStrMediaMid(object.getString("strMediaMid"));
                    song.setAlbummid(object.getString("albummid"));
                    song.setSongmid(object.getString("songmid"));
                    song.setAlbumpic_big(object.getString("albumpic_big"));
                    song.setAlbumpic_small(object.getString("albumpic_small"));
                    song.setAlbumid(object.getString("albumid"));
                    data.add(song);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    public static String parseLyric(String json) {
        if (!TextUtils.isEmpty(json)) {
            try {
                JSONObject object = new JSONObject(json);
                object = object.getJSONObject("showapi_res_body");
                String lyrics = object.getString("lyric");
                //转换歌词中的Html标记
                lyrics = Html.fromHtml(lyrics).toString();
                lyrics = lyrics.replace("[", "\n\t[");
                lyrics.substring(1);
                return lyrics;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
