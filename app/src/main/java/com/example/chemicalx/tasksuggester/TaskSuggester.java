package com.example.chemicalx.tasksuggester;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskSuggester {
    private static final String TAG = "TaskSuggester";

    public static void trainModel(Context context) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        String url = "https://us-central1-chemical-x-d86cc.cloudfunctions.net/train_model";
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG + ".trainModel", "User ID: " + userID);
        JSONObject requestJSONObject = new JSONObject();
        try {
            requestJSONObject.put("userID", userID);
        } catch (JSONException e) {
            Log.e(TAG + ".trainModel", "Could not create the request JSON." + e.getMessage());
            return;
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, url, requestJSONObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(context, "Model successfully trained.", Toast.LENGTH_SHORT);
                        try {
                            String message = (String) response.get("message");
                            Log.d(TAG + ".trainModel", "Response message: " + message);
                            String modelUserID = (String) response.get("modelUserID");
                            Log.d(TAG + ".trainModel", "Model User ID: " + modelUserID);
                        } catch (JSONException e) {
                            Log.e(TAG + ".trainModel", e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, "Model could not be trained; there was an error.", Toast.LENGTH_LONG);
                        String volleyErrorMessage = error.getMessage();
                        if (volleyErrorMessage != null) {
                            Log.e(TAG + ".trainModel", volleyErrorMessage);
                        }
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }
}
