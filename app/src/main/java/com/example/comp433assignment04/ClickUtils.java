package com.example.comp433assignment04;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.example.comp433assignment04.DataCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ClickUtils {

        private ClickUtils() {}

    /**
     * The proper way to return back to the main activity (home page) is to close the secondary
     * activity (photo tagger, sketch tagger, comment board). This is better because you are not
     * creating additional copies of the same activity, MainActivity. This code is responding to
     * the original Intent object that opened the secondary activity in the first place.
     * @param activity
     * @return
     */
    public static View.OnClickListener backToPrevious(Activity activity) {
            Log.v(MainActivity.TAG, "Clicked Back from " + getActivityClass(activity));
            return v -> activity.finish();
    }

    /**
     * Shows a quick message at the top of the screen that goes away in a few seconds.
     * @param context
     * @param message
     * @return
     */
    public static void showToastOnClick(Context context, String message) {
        Log.v(MainActivity.TAG, message);

        if (context == null) {
            Log.w(MainActivity.TAG, "showToastOnClick: context is null");
            return;
        }

        // Always post to the main (UI) thread
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
           Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Shows a message that cannot be dismissed until the user clicks OK.
     * @param activity
     * @param title
     * @param message
     */
    public static void showBlockingAlert(Activity activity, String title, String message) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return; // avoid crashes if activity is not valid
        }

        Log.e(MainActivity.TAG, message);

        activity.runOnUiThread(() -> {
            new AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)        // dialog CANNOT be dismissed except by button
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    /**
     * If the context is a subclass of Activity, get the class associated with it.
     * @param context
     * @return
     */
    private static String getContextClass(Context context) {
        if (!(context instanceof Activity)) {
            return "";
        }

        return getActivityClass((Activity) context);
    }

    /**
     * Get the class associated with the specified Activity object.
     * @param activity
     * @return
     */
    private static String getActivityClass(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return ""; // avoid crashes if activity is not valid
        }

        return activity.getClass().getSimpleName();
    }

    /**
     * For both types of views, attempt to get the bitmap for saving to the database.
     * @param sourceView
     * @return
     */
    private static Bitmap getBitmapFromView(View sourceView) {

        if (sourceView == null) {
            return null;
        }

        if (sourceView instanceof MyDrawingArea) {
            return ((MyDrawingArea) sourceView).getBitmap();
        }

        if (!(sourceView instanceof ImageView)) {
            return null;
        }

        Drawable drawable = ((ImageView) sourceView).getDrawable();

        if (!(drawable instanceof BitmapDrawable)) {
            return null;
        }

        return ((BitmapDrawable) drawable).getBitmap();
    }

    /**
     *
     * @param context
     * @param sourceView
     * @param textView
     * @return
     */
    public static View.OnClickListener saveImageOnClick(
            Context context,
            View sourceView,
            TextView textView
    ) {
        return v -> {

            Log.v(MainActivity.TAG, "Clicked save from " + getContextClass(context));

            int imageType = DatabaseHelper.IMAGE_TYPE_PHOTO;
            MyDrawingArea mda = null;
            ImageView iv = null;

            if (sourceView instanceof MyDrawingArea) {
                imageType = DatabaseHelper.IMAGE_TYPE_SKETCH;
                mda = ((MyDrawingArea) sourceView);
            }

            if (sourceView instanceof ImageView) {
                iv = ((ImageView) sourceView);
            }

            // Determine the type of view we are working with and get the bitmap accordingly
            Bitmap bitmap = getBitmapFromView(sourceView);

            if (textView == null) {
                showBlockingAlert((Activity) context, "Error saving image", "Failed to read tags textbox.");
                return;
            }

            boolean result = DatabaseHelper.saveImageToDB(context, bitmap, textView.getText().toString(), imageType);

            // TODO: What should I do if the result fails? This is already handled by DatabaseHelper.saveImageToDB.

            if (result) {
                textView.setText("");

                // clear the image
                if (mda != null) {
                    mda.resetPath();
                }

                if (iv != null) {
                    iv.setImageBitmap(null);
                }
            }
        };
    }

    public static View.OnClickListener findImagesOnClick(
            Context context,
            String tags,
            int imageType,
            DataCallback<ArrayList<CommentItem>> callback
    ) {
        return v -> {

            Log.v(MainActivity.TAG, "Clicked find from " + getContextClass(context));
            Log.v(MainActivity.TAG, "Photos to return: " + DatabaseHelper.getImageTypeName(imageType));

            ArrayList<CommentItem> comments = DatabaseHelper.findImages(context, tags, imageType);

            int numComments = comments.size();

            showToastOnClick(context, numComments + " images found.");

            callback.onData(comments);
        };
    }

    public static View.OnClickListener getCommentsOnClick(
            Context context,
            String tags,
            DataCallback<ArrayList<CommentItem>> callback
    ) {
        return v -> {

        };
    }

    /**
     * Retrieve tags from Google Vision API based on the image in the main view of the activity.
     * This could be a photo or a sketch.
     * @param context
     * @param sourceView
     * @param textView
     * @param callback
     * @return
     */
    public static View.OnClickListener getGeminiTagsOnClick(
            Context context,
            View sourceView,
            TextView textView,
            DataCallback<String> callback
    ) {
        return v -> {

            if (textView == null) {
                showBlockingAlert((Activity) context,
                        "Could not retrieve tags",
                    "Fatal error prevented the app from calling Google Vision API for the tags."
                );
                return;
            }

            Bitmap bitmap = getBitmapFromView(sourceView);

            if (bitmap == null) {
                showBlockingAlert((Activity) context, "No image", "Must have a valid image before retrieving tags.");
                return;
            }

            showToastOnClick(context, "Fetching tags...");

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.v(MainActivity.TAG, "Entering vision test try block...");
                        String[] tags = GeminiHelper.myVisionTester(bitmap);

                        String tagsJoined = String.join(", ", tags);

                        textView.setText(tagsJoined);

                        Log.v(MainActivity.TAG, "The vision test has completed.");
                        showToastOnClick(context, "Tags have been retrieved for the current image.");

                        callback.onData(tagsJoined);

                        return;
                    } catch (IOException e) {
                        showBlockingAlert((Activity) context, "Could not get tags", "Failed to tag image via Google Vision API.");
                        e.printStackTrace();

                        String errorMessage = e.getMessage();
                        if (errorMessage != null && !errorMessage.isEmpty()) {
                            Log.e(MainActivity.TAG, errorMessage);
                        }

                        textView.setText(null);
                        callback.onData("");
                    }
                }
            });

            thread.start();
        };
    }
}
