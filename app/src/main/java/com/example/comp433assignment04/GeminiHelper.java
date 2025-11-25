package com.example.comp433assignment04;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GeminiHelper {

    /**
     * This is the number of tags that will be shown to the user.
     */
    public static final int TAGLIMIT = 2;

    /**
     * Default constructor. Does nothing.
     */
    private GeminiHelper() {}

    /**
     * Defines an interface to act as a callback.
     */
    public interface GeminiCallback {
        void onSuccess(String text);
        void onError(Exception e);
    }

    /**
     *
     * @param response
     * @return A String[] of tags from the Vision API response.
     */
    public static String[] getVisionAPIDescriptions(BatchAnnotateImagesResponse response) {
        List<String> descriptions = new ArrayList<>();

        // response.getResponses() returns a List of AnnotateImageResponse objects
        for (AnnotateImageResponse annotateImageResponse : response.getResponses()) {

            if (annotateImageResponse == null || annotateImageResponse.getLabelAnnotations() == null) {
                continue;
            }

            for (EntityAnnotation label : annotateImageResponse.getLabelAnnotations()) {

                // No need to continue if the tag limit has been reached.
                if (descriptions.size() == TAGLIMIT) {
                    descriptions.toArray(new String[0]);
                }

                // This long if statement ensures only valid, unique, non-empty labels are added.
                if (label.getDescription() != null && !label.getDescription().isEmpty() &&
                        !descriptions.contains(label.getDescription())) {
                    descriptions.add(label.getDescription());
                }
            }
        }

        return descriptions.toArray(new String[0]);
    }

    /**
     * Returns the tags returned from the Vision API.
     * @param bitmap
     * @return
     * @throws IOException
     */
    public static String[] myVisionTester(Bitmap bitmap) throws IOException
    {

        if (bitmap == null) {
            Log.v(MainActivity.TAG, "The bitmap is null and cannot be used with Google Vision API.");
            return new String[0];
        }

        Log.v(MainActivity.TAG, "made it to myVisionTester function.");

        //1. ENCODE image.
        Log.v(MainActivity.TAG, "decoded the image into a Bitmap object");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bout);
        } catch (Exception e) {
            Log.e(MainActivity.TAG, "failed to compress the bitmap into a byte array: " + e.getMessage(), e);
            e.printStackTrace();
        }

        Log.v(MainActivity.TAG, "compressed the bitmap into a byte array (variable name 'bout').");

        Image myimage = new Image();
        myimage.encodeContent(bout.toByteArray());

        Log.v(MainActivity.TAG, "converted the bitmap into an Image object.");

        Log.v(MainActivity.TAG, "made it to creating AnnotateImageRequest.");


        //2. PREPARE AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myimage);
        Feature f = new Feature();
        f.setType("LABEL_DETECTION");
        f.setMaxResults(5);
        List<Feature> lf = new ArrayList<Feature>();
        lf.add(f);
        annotateImageRequest.setFeatures(lf);

        Log.v(MainActivity.TAG, "made it to creating the Vision.Builder object...");

        //3.BUILD the Vision
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);

        Log.v(MainActivity.TAG, "made to create the Vision object with the API key...");

        // This app now reads the API key from Gradle Scripts/local.properties, which prevents
        // committing the key accidentally to version control
        // get the api key from the BuildConfig class, which gets it from local.properties (Gradle Scripts)
        builder.setVisionRequestInitializer(new VisionRequestInitializer(BuildConfig.VISION_API_KEY));
        Vision vision = builder.build();

        Log.v(MainActivity.TAG, "made to creating the BatchAnnotateImagesRequest...");

        //4. CALL Vision.Images.Annotate
        // To understand the JSON that is returned, look at the documentation for the
        // AnnotateImageResponse object here:
        // https://cloud.google.com/vision/docs/reference/rest/v1/AnnotateImageResponse
        // Each object in the "labelAnnotations" array is an EntityAnnotation object:
        // https://cloud.google.com/vision/docs/reference/rest/v1/AnnotateImageResponse#EntityAnnotation
        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        List<AnnotateImageRequest> list = new ArrayList<AnnotateImageRequest>();
        list.add(annotateImageRequest);
        batchAnnotateImagesRequest.setRequests(list);
        Vision.Images.Annotate task = vision.images().annotate(batchAnnotateImagesRequest);
        Log.v(MainActivity.TAG, "About to execute the vision task; please wait up to 60 seconds for a response.");
        BatchAnnotateImagesResponse response = task.execute();
        Log.v(MainActivity.TAG, response.toPrettyString());

        // get a list of descriptions
        return getVisionAPIDescriptions(response);
    }

    /**
     * Reaches out to Gemini to retrieve 3 comments. These 3 comments are then added to
     * an arraylist of CommentItem objects which are then displayed on the screen in a ListView.
     */
    public static void getCommentsFromGemini(Context context, String geminiPrompt, GeminiCallback callback) {

        if (context == null) {
            return;
        }

        if (geminiPrompt == null || geminiPrompt.isEmpty()) {
            return;
        }

        if (callback == null) {
            Log.e(MainActivity.TAG, "getCommentsFromGemini(); a callback is required for this to work properly.");
            return;
        }

        try {

            String json = "{\"contents\": " +
                    "[{\"parts\": " +
                    "[{\"text\": " +
                    "\"" + geminiPrompt + "\"" +
                    "}]}]}";

            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

            // Go to https://aistudio.google.com/app/api-keys to get your API key
            // Add the API key to local.properties with the key name "api.key"
            // For the URL for this request, you can get it by clicking "Copy cURL quickstart" when
            // viewing the API key details within the Google AI Studio website.
            // The free tier was sufficient for this to work.
            // The cURL quickstart is good for confirming whether the API key works.
            Request r = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
                    .addHeader("X-goog-api-key", BuildConfig.GEMINI_API_KEY)
                    .post(body)
                    .build();

            // This is just to make sure that the API key is being loaded correctly
//            Log.v("TAG", "API_KEY: " + BuildConfig.API_KEY);

            OkHttpClient okHttpClient = new OkHttpClient();

            okHttpClient.newCall(r).enqueue(new Callback() {

                /**
                 * It is really important to log the error here if one occurs so that you can
                 * better troubleshoot what is going wrong.
                 * @param call
                 * @param e
                 */
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    callback.onError(e);
                    ClickUtils.showBlockingAlert((Activity) context, "Gemini API Failure", "Failed to retrieve comments from Gemini.");
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                    // You cannot call .string() on response.body() more than one because it is a stream.
                    ResponseBody body = response.body();
                    String json = "";

                    if (body == null) {
                        callback.onError(new IOException("Empty response body"));
                        return;
                    }

                    json = body.string();
                    body.close();
                    String extractedText = extractGeminiText(json);

                    Log.v(MainActivity.TAG, "getCommentsFromGemini(); successful extract text response from Gemini: " + extractedText);

                    callback.onSuccess(extractedText);

                    }
            }); // end of the callback

        } catch (Exception e) {
            callback.onError(e);
            Log.v("classwork04", e.getMessage());
        }
    }

    /**
     * Returns a JSONArray from a JSONObject if it exists.
     * @param obj
     * @param name
     * @return
     */
    public static JSONArray getJSONArray(JSONObject obj, String name) {
        if (obj == null || name == null || name.isEmpty() || !obj.has(name)) {
            return null;
        }

        JSONArray array = obj.optJSONArray(name);

        if (array == null || array.length() == 0) {
            return null;
        }

        return obj.optJSONArray(name);
    }

    /**
     * Retrieve the "text" property of the response from Gemini.
     * @param json
     * @return
     */
    public static String extractGeminiText(String json) {
        if (json == null || json.isEmpty()) {
            return "";
        }

        try {
            JSONObject root = new JSONObject(json);

            JSONArray candidates = getJSONArray(root, "candidates");

            if (candidates == null) {
                return "";
            }

            JSONObject firstCandidate = candidates.optJSONObject(0);

            if (firstCandidate == null) {
                return "";
            }

            // "content" must be an object
            JSONObject content = firstCandidate.optJSONObject("content");

            if (content == null) {
                return "";
            }

            JSONArray parts = getJSONArray(content, "parts");
            if (parts == null) {
                return "";
            }

            JSONObject firstPart = parts.optJSONObject(0);
            if (firstPart == null) {
                return "";
            }

            return firstPart.optString("text", "");

        } catch (JSONException e) {
            Log.e(MainActivity.TAG, "Error parsing JSON from Gemini response.");
            return "";
        }
    }

}
