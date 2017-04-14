package com.northwestern.habits.datagathering.webapi;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by Y.Misal on 4/10/2017.
 */

public class DecodeResponseData {

    public static JSONObject encodeData(String flag, Object value) {

//        String[] inFlag = flag.split("_");

        // json data
        JSONObject jsonObject = new JSONObject();
        try {

            if ( flag.equalsIgnoreCase("login") ) {
                String[] values = (String[]) value;
                jsonObject.put( "username", values[0] );
                jsonObject.put( "password", values[1] );
                Log.e("DecodeJSONData Data: ", flag + ": " + jsonObject);
                byte[] param = jsonObject.toString().getBytes("UTF-8");
                String par_base64 = Base64.encodeToString(param, Base64.DEFAULT);

                jsonObject = new JSONObject();
                jsonObject.put("data", par_base64);
                Log.d("DecodeJSONData Data: ", flag + ": " + jsonObject);
            } else if ( flag.equalsIgnoreCase("reset") ) {
                String[] values = (String[]) value;
                jsonObject.put( "username", values[0] );
                jsonObject.put( "new_password", values[1] );
                jsonObject.put( "old_password", values[2] );
                Log.e("DecodeJSONData Data: ", flag + ": " + jsonObject);
                byte[] param = jsonObject.toString().getBytes("UTF-8");
                String par_base64 = Base64.encodeToString(param, Base64.DEFAULT);

                jsonObject = new JSONObject();
                jsonObject.put("data", par_base64);
                Log.d("DecodeJSONData Data: ", flag + ": " + jsonObject);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    public static JSONObject decodeData(String response) {
        Log.e("DecodeJSONData: ", "encode: " + response);
        // json data
        JSONObject jsonObject1;
        try {
            JSONObject jsonObject = new JSONObject(response);
            String dv = jsonObject.getString("data");

            byte[] data1 = Base64.decode(dv, Base64.DEFAULT);
            String text = new String(data1, "UTF-8");

            jsonObject1 = new JSONObject(text);
        } catch (JSONException e) {
            e.printStackTrace();
            jsonObject1 = null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            jsonObject1 = null;
        }
        return jsonObject1;
    }

}
