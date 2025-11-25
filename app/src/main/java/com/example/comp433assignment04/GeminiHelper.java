package com.example.comp433assignment04;

import android.graphics.Bitmap;
import android.util.Log;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
}
