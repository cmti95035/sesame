package com.chinamobile.cmti.sesame;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IoTManager {

    private static final String LIFX_URL = "https://api.lifx" +
            ".com/v1/lights/all/state?power=on";
    private static final String LIFX_TOKEN =
            "c6ecbd16a6cfe373cb0edb7dc8d40e395501fee045556e4c3824e024e52c6530";

    private static final boolean isWink = true;

    private static final String SMARTTHINGS_TOKEN = "011eb775-149c-424e-82df-7cf40489ffce";
    private static final String SMARTTHINGS_ENDPOINT = "https://graph-na02-useast1.api.smartthings.com/api/smartapps/installations/1e0f70e8-4fbc-452b-b1bb-4fb70feb7077/lock/";

    private static final String WINK_TOKEN =
            "kCpyrPc2ODMrqbGeghEAwSWtFWT4RIjV";
    private static final String WINK_ENDPOINT = "https://api.wink" +
            ".com/locks/189689";

    private static final String JSON_WINK_LOCK = "{'desired_state':"
            +"{'locked':'true'}}";

    private static final String JSON_WINK_UNLOCK = "{'desired_state':"
            +"{'locked':'false'}}";


    private static final HashMap<String, LifxParam> lifxParams = new
            HashMap<>();
    private static volatile IoTManager instance = null;
    private final OkHttpClient client = new OkHttpClient();

    private IoTManager() {
    }

    public static IoTManager getInstance() {
        if (instance == null) {
            synchronized (IoTManager.class) {
                if (instance == null) {
                    instance = new IoTManager();
                    initLifx();
                }
            }
        }
        return instance;
    }

    private static void initLifx() {
        lifxParams.put("charlie", new LifxParam("0.4", "green saturation:0.4"));
        lifxParams.put("jian", new LifxParam("0.6", "blue saturation:0.6"));
        lifxParams.put("rui", new LifxParam("0.8", "orange saturation:0.8"));
        lifxParams.put("nomatch", new LifxParam("0.9", "yellow " +
                "saturation:0.9"));
    }

    void setLifx(String name) {
        LifxParam profile = lifxParams.get(name);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(LIFX_URL).newBuilder();
        urlBuilder.addQueryParameter("color", profile.getColor());
        urlBuilder.addQueryParameter("brightness", profile.getBrightness());
        String url = urlBuilder.build().toString();

        RequestBody formBody = new FormBody.Builder()
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + LIFX_TOKEN)
                .put(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws
                    IOException {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    System.out.println(responseHeaders.name(i) + ": " +
                            responseHeaders.value(i));
                }

                System.out.println(response.body().string());
            }
        });
    }

    void lockSchlage() {
        if (isWink) {
            lockWink();
        } else {
            lockSmartthings();
        }
    }

    void unlockSchlage() {
        if (isWink) {
            unlockWink();
        } else {
            unlockSmartthings();
        }
    }

    void lockWink() {
        setWink(1);
    }

    void unlockWink() {
        setWink(0);
    }

    void lockSmartthings() {
        setSmartthings(1);
    }

    void unlockSmartthings() {
        setSmartthings(0);
    }

    private void setWink(int status) {

        // the real lock status is reversed anyhow
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        HttpUrl.Builder urlBuilder = HttpUrl
                .parse(WINK_ENDPOINT)
                .newBuilder();
        String url = urlBuilder.build().toString();

        String jsonString = null;

        try {
            JSONObject lockStatus = new JSONObject();
            lockStatus.put("locked",  status==1?"false":"true");
            JSONObject desiredStatus = new JSONObject();
            desiredStatus.put("desired_state", lockStatus);
            jsonString = desiredStatus.toString();
        }  catch(JSONException ex) {
            ex.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, jsonString);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + WINK_TOKEN)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws
                    IOException {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    System.out.println(responseHeaders.name(i) + ": " +
                            responseHeaders.value(i));
                }

                Log.d("DEBUG", response.body().string());
            }
        });
    }

    private void setSmartthings(int status) {

        // the real lock status is reversed anyhow
        HttpUrl.Builder urlBuilder = HttpUrl
                .parse(SMARTTHINGS_ENDPOINT + (status==1?"off":"on"))
                .newBuilder();
        String url = urlBuilder.build().toString();

        RequestBody formBody = new FormBody.Builder()
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + SMARTTHINGS_TOKEN)
                .put(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws
                    IOException {
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);

                Headers responseHeaders = response.headers();
                for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                    System.out.println(responseHeaders.name(i) + ": " +
                            responseHeaders.value(i));
                }
                Log.d("DEBUG", response.body().string());

            }
        });
    }

}
