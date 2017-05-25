package com.northwestern.habits.datagathering.webapi;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by Y.Misal on 4/10/2017.
 */

public class WebAPIManager {

    public static String URL_H = "http://14.141.176.221:8082/habits/public/";
    public static String URL = "http://174.138.56.143/public/";
    public static final String URL_STRING_CB = "http://174.138.56.143/db/";

    // fetch data from remote server with flag
    public static String httpPOSTRequest(String urlStr, Object val, String flag) {
        Log.e("WebAPIManager URL: ", flag + ": " + urlStr);
        HttpURLConnection conn = null;
        try {
            URL url1 = new URL(urlStr);
            conn = (HttpURLConnection) url1.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonObject = null;
            jsonObject = DecodeResponseData.encodeData( flag, val );
            DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
            if (jsonObject == null)
                return "fail";

            // what should I write here to output stream to post params to server ?
            outputStream.writeBytes(jsonObject.toString());
            Log.d("WebAPIManager Data: ", flag + ": " + jsonObject.toString());
            outputStream.flush();
            outputStream.close();

            conn.connect();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String responseString = "";
        int resCode = -1;
        try {
            // get response
            resCode = conn.getResponseCode();
            String msg = conn.getResponseMessage();
            Log.e("WebAPIManager: ", flag + ": response_code" + resCode);
            Log.e("WebAPIManager: ", flag + ": response_msg" + msg);

            if (resCode == HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_ACCEPTED || resCode == HttpURLConnection.HTTP_CREATED || msg.equalsIgnoreCase("Created")) {
                InputStream responseStream = new BufferedInputStream(conn.getInputStream());
                BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));
                String line = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                responseStreamReader.close();

                String response = stringBuilder.toString();
                JSONObject jsonObject = DecodeResponseData.decodeData(response);
//                Map<String,List<String>> mHeaders = conn.getHeaderFields();
                try { // "Authorization" -> " size = 1"
                    jsonObject.put("auth", conn.getHeaderField("Authorization"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (jsonObject == null)
                    return "fail";
                responseString = jsonObject.toString();
                responseString = responseString  + "~" + flag;
                Log.e("WebAPIManager: ", flag + ": response_str" + responseString);

            } else {
                responseString = "fail";
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            responseString = "fail";
        } catch (IOException e) {
            e.printStackTrace();
            responseString = "fail";
        }
        return responseString;
    }

    // fetch data from remote server with flag
    public static String httpPOSTUploadRequest(String urlStr, Object val, String path, String flag) {
        Log.e("WebAPIManager URL: ", flag + ": " + urlStr);
        HttpURLConnection conn = null;
        try {
            URL url1 = new URL(urlStr);
            conn = (HttpURLConnection) url1.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonObject = null;
            jsonObject = DecodeResponseData.encodeData( flag, val );
            DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());

            if (jsonObject == null)
                return "fail";

            // what should I write here to output stream to post params to server ?
            outputStream.writeBytes(jsonObject.toString());
            Log.d("WebAPIManager Data: ", flag + ": " + jsonObject.toString());
            outputStream.flush();
            outputStream.close();

            conn.connect();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String responseString = "";
        int resCode = -1;
        try {
            // get response
            resCode = conn.getResponseCode();
            String msg = conn.getResponseMessage();
            Log.e("WebAPIManager: ", flag + ": response_code" + resCode);
            Log.e("WebAPIManager: ", flag + ": response_msg" + msg);

            if (resCode == HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_ACCEPTED || resCode == HttpURLConnection.HTTP_CREATED || msg.equalsIgnoreCase("Created")) {
                InputStream responseStream = new BufferedInputStream(conn.getInputStream());
                BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));
                String line = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                responseStreamReader.close();

                String response = stringBuilder.toString();
                JSONObject jsonObject = DecodeResponseData.decodeData(response);
//                Map<String,List<String>> mHeaders = conn.getHeaderFields();
                try { // "Authorization" -> " size = 1"
                    jsonObject.put("auth", conn.getHeaderField("Authorization"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (jsonObject == null)
                    return "fail";
                responseString = jsonObject.toString();
                responseString = responseString  + "~" + flag;
                Log.e("WebAPIManager: ", flag + ": response_str" + responseString);

            } else {
                responseString = "fail";
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            responseString = "fail";
        } catch (IOException e) {
            e.printStackTrace();
            responseString = "fail";
        }
        return responseString;
    }
}
