package com.serenade.lyricview.utils;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Serenade on 17/2/4.
 */

public class HttpUtils {
    public static String getString(String path) {
        if (!TextUtils.isEmpty(path)) {
            StringBuilder sb = null;
            InputStream is = null;
            BufferedReader br = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(path);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                sb = new StringBuilder();
                if (connection.getResponseCode() == 200) {
                    is = connection.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    String s = "";
                    while ((s = br.readLine()) != null) {
                        sb.append(s);
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (br != null)
                        br.close();
                    if (is != null)
                        is.close();
                    if (connection != null)
                        connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }
}
