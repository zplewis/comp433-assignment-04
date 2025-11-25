package com.example.comp433assignment04;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class DatabaseHelper {

    /**
     * The name of the database created for this application.
     */
    public static final String DB_NAME = "mydb";

    /**
     * Used with the SQLite database for storing the images. This makes it so that the
     * "Find" button only returns the correct type of images.
     */
    static final int IMAGE_TYPE_PHOTO = 1;
    static final int IMAGE_TYPE_SKETCH = 2;

    /**
     * There is no type "0" in the database; this is to signal that the results should not be
     * limited by type at all, which will be useful for the Comment Board class.
     */
    static final int IMAGE_TYPE_BOTH = 0;

    /**
     * Maximum size of image BLOB in bytes (2 MB). This is to be in compliance with the CursorWindow.
     */
    static final int MAX_BLOB_SIZE = 1024 * 1024 * 2;

    /**
     * The minimum allowed image quality.
     */
    static final int MIN_IMAGE_QUALITY = 30;

    /**
     * The maximum image quality percentage.
     */
    static final int FULL_IMAGE_QUALITY = 100;

    private DatabaseHelper() {}

    public static String getImageTypeName(int imageType) {
        if (imageType < 0 || imageType > 2) {
            return "";
        }

        if (imageType == IMAGE_TYPE_BOTH) {
            return "IMAGE_TYPE_BOTH";
        }

        if (imageType == IMAGE_TYPE_PHOTO) {
            return "IMAGE_TYPE_PHOTO";
        }

        return "IMAGE_TYPE_SKETCH";
    }

    /**
     * Makes sure the database and required tables have been created, if they do not exist already.
     * Enables dropping the tables and recreating them as well.
     * @param context
     * @param clearTables
     */
    public static void setupDBAndTables(Context context, boolean clearTables) {

        // context is required
        if (context == null) {
            Log.e(MainActivity.TAG, "setupDBAndTables(); no context was provided.");
            return;
        }

        SQLiteDatabase mydb = null;

        try {

            mydb = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

            // allegedly, you can enable support for foreign keys, so let's do that:
            // we don't actually use them, but whatever.
            mydb.execSQL("PRAGMA foreign_keys = ON;");

            // both of these lines are required to clear the tables
            if (clearTables) {
                mydb.execSQL("DROP TABLE IF EXISTS images");
                mydb.execSQL("DROP TABLE IF EXISTS image_tags");
                mydb.execSQL("DROP TABLE IF EXISTS image_comments");
                ClickUtils.showToastOnClick(context, "All SQLite tables for this app have been dropped and created again.");
            }

            // Create a table that keeps up with image blobs if it does not exist already
            // SQLite does not have a dedicated DATETIME type, so use text storing in this format:
            // YYYY-MM-DD HH:MM:SS (ISO8601)
            String sql = "CREATE TABLE IF NOT EXISTS images (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "IMAGE BLOB NOT NULL, " +
                    "CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "IMAGE_TYPE_ID INTEGER NOT NULL)";
            mydb.execSQL(sql);

            sql = "CREATE TABLE IF NOT EXISTS image_tags ( " +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "IMAGE_ID INTEGER NOT NULL, " +
                    "TAG TEXT NOT NULL COLLATE NOCASE)";
            mydb.execSQL(sql);

            // Store the comments in the database
            sql = "CREATE TABLE IF NOT EXISTS image_comments (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "IMAGE_ID INTEGER NOT NULL, " +
                    "COMMENT_TEXT TEXT NOT NULL, " +
                    "COMMENTER_ID INTEGER NOT NULL)";
            mydb.execSQL(sql);

            // Create a table to keep up with the image types.
            // Make sure photo = 1 and sketch = 2 for consistency
            sql = "CREATE TABLE IF NOT EXISTS image_types ( " +
                    "ID INTEGER PRIMARY KEY, " +
                    "IMAGE_TYPE TEXT NOT NULL); " +
                    "INSERT INTO image_types (ID, IMAGE_TYPE) VALUES (" + IMAGE_TYPE_PHOTO + ", 'Photo'); " +
                    "INSERT INTO image_types (ID, IMAGE_TYPE) VALUES (" + IMAGE_TYPE_SKETCH + ", 'Sketch') ";
            mydb.execSQL(sql);

        } catch (SQLiteException e) {
            ClickUtils.showBlockingAlert((Activity) context, "Create tables database error", e.getMessage());
        } finally {
            if (mydb != null && mydb.isOpen()) {
                mydb.close();
            }
        }
    }

    /**
     * Regardless if it is a photo or a sketch, attempt to save the bitmap to the SQLite database
     * as a BLOB. This function reduces the quality as necessary to make sure it is within the
     * CursorWindow. The CursorWindow does not limit how large the BLOB is while saving but seems
     * to affect retrieving a BLOB via SELECT query.
     * @param bitmap
     * @return
     */
    public static byte[] getBitmapAsBytes(Bitmap bitmap) {
        if (bitmap == null) {
            return new byte[0];
        }

        ByteArrayOutputStream stream;
        int quality = FULL_IMAGE_QUALITY;
        int deltaQuality = 10;

        while (true) {
            stream = new ByteArrayOutputStream();
            Log.v(MainActivity.TAG, "Bitmap will be compressed to quality: " + quality);
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                throw new IllegalStateException("Bitmap.compress() failed");
            }

            byte[] data = stream.toByteArray();
            int size = data.length;

            if (size <= MAX_BLOB_SIZE) {
                Log.v(MainActivity.TAG, "getBitmapAsBytes(); Photo is 2 MB or smaller and safe for saving to the SQLite database.");
                return data;
            }

            // give up if the quality is too low
            if (quality == MIN_IMAGE_QUALITY) {
                Log.v(MainActivity.TAG, "Photo is the minimum quality allowed.");
                return data;
            }

            quality = Math.max(MIN_IMAGE_QUALITY, quality - deltaQuality);
        }
    }

    /**
     * Converts a String containing a comma-separated list of values to an actual String array.
     * @param text
     * @return
     */
    public static String[] cslToArray(String text) {
        String[] parts = text.split(",");
        ArrayList<String> list = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }

        return list.toArray(new String[0]);
    }

    /**
     * Saves the specified bitmap image and tags from the specified TextView to the SQLite database.
     * @param bitmap
     * @param tagsText
     */
    public static boolean saveImageToDB(Context context, Bitmap bitmap, String tagsText, int imageType) {
        // this is all code to make sure we have valid values prior to attempting to save anything
        // to the database; input validation!
        if (!(context instanceof Activity)) { // this covers whether the context variable is null also
            // This needed because you cannot call openOrCreateDatabase from a static context.
            Log.e(MainActivity.TAG, "Context was null, which is required for a SQLite connection.");
            return false;
        }

        if (bitmap == null) {
            ClickUtils.showBlockingAlert((Activity) context, "No photo", "Bitmap was null, which is required to save the photo data.");
            return false;
        }

        byte[] bitmapByteArray = getBitmapAsBytes(bitmap);

        if (bitmapByteArray.length == 0) {
            ClickUtils.showBlockingAlert((Activity) context, "No photo", "Byte array from the bitmap was null, which means the photo or sketch was empty.");
            return false;
        }

        // is the image too big?
        if (bitmapByteArray.length > MAX_BLOB_SIZE) {
            String errorMessage = "Image size in bytes must be smaller than " + MAX_BLOB_SIZE + " (2 MB); actual: " + bitmapByteArray.length;
            ClickUtils.showBlockingAlert((Activity) context, "Image Too Big", errorMessage);
            return false;
        }

        String[] tags = cslToArray(tagsText);

        Log.v(MainActivity.TAG, "Original text: " + tagsText + "; # of tags: " + tags.length);

        // no need to artificially limit the tags: || tags.length > TAGLIMIT
        if (tags.length == 0) {
            String errorMessage = "No tag was specified; a tag is required for saving an image.";
            ClickUtils.showBlockingAlert((Activity) context, "No tags specified", errorMessage);
            return false;
        }

        if (imageType != IMAGE_TYPE_PHOTO && imageType != IMAGE_TYPE_SKETCH) {
            Log.e(MainActivity.TAG, "Invalid imageType was specified");
            return false;
        }

        // At this point, we should have a valid image, tags, and image type. Use a ContentValues
        // variable for inserting these items into the database.
        // 3. The "created_at" column defaults to the current timestamp,
        // so we do not have to insert that.
        // Insert the values into the database. You'll need to insert the
        // image into the database.
        ContentValues cv = new ContentValues();
        cv.put("IMAGE", bitmapByteArray);
        cv.put("IMAGE_TYPE_ID", imageType);

        // Insert the image into the database and save the new ID; that will be
        // useful for inserting the tags into the mapping table.
        SQLiteDatabase mydb = null;

        try {

            mydb = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

            mydb.beginTransaction();

            // insert the image blob with its type into the "images" table
            long newId = mydb.insert("images", null, cv);

            if (newId == -1) {
                Log.e(MainActivity.TAG, "Failed to save the image to the database and retrieve the new row ID.");
                throw new SQLiteException("Failed to save image to the database.");
            }

            // Loop through all of the tags, skipping any null or empty ones.
            // Add the tags to the "image_tags" mapping table.
            for (String tag : tags) {
                if (tag == null) {
                    continue;
                }
                tag = tag.trim();
                if (tag.isEmpty()) {
                    continue;
                }

                cv.clear();
                cv.put("IMAGE_ID", newId);
                cv.put("TAG", tag);
                mydb.insert("image_tags", null, cv);
            }

            mydb.setTransactionSuccessful();

            Log.v(MainActivity.TAG, "Image should be saved to the database with ID: " + newId);

            return true;

        } catch (SQLiteException e) {
            String errorMessage = e.getMessage();
            ClickUtils.showBlockingAlert((Activity) context, "saveImageToDB(); Error Caught", errorMessage);
            return false;
        } finally {

            if (mydb != null) {
                mydb.endTransaction();
            }

            if (mydb != null && mydb.isOpen()) {
                mydb.close();
            }

            // Show that the image saved successfully.
            ClickUtils.showToastOnClick(context, "Image saved successfully.");
        }
    }

    /**
     * Performs a search query using the "find" textbox. If no tag is provided, then all images
     * are returned.
     * @param context
     * @param tags
     * @param imageType
     * @return
     */
    public static ArrayList<CommentItem> findImages(Context context, String tags, int imageType) {
        if (context == null) {
            Log.e(MainActivity.TAG, "findImages(); context is null");
            return new ArrayList<>();
        }

        if (imageType != IMAGE_TYPE_PHOTO && imageType != IMAGE_TYPE_SKETCH && imageType != IMAGE_TYPE_BOTH) {
            Log.e(MainActivity.TAG, "findImages(); imageType is invalid");
            return new ArrayList<>();
        }

        // It is allowed to search and provide no search terms!
//        if (tags == null || tags.isEmpty()) {
//            Log.e(MainActivity.TAG, "findImages(); TextView containing the tags is null");
//            return new ArrayList<>();
//        }

        Log.v("findImages", "findImages(); findText: " + tags);

        String sql = getFindSQL(imageType, tags);

        return getCommentItems(context, sql);
    }

    private static ArrayList<CommentItem> getCommentItems(Context context, String sql) {

        ArrayList<CommentItem> comments = new ArrayList<>();

        if (context == null) {
            Log.e(MainActivity.TAG, "getCommentItems(); context was null for some reason.");
            return comments;
        }

        if (sql == null || sql.isEmpty()) {
            Log.e(MainActivity.TAG, "getCommentItems(); SQL is null or empty.");
            return comments;
        }

        Log.v(MainActivity.TAG, "getCommentItems(); SQL: " + sql);

        SQLiteDatabase mydb = null;

        // artificial limit, really
        int numSearchResults = 100;

        try {

            mydb = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

            // Perform the search for the images and display them.
            Cursor c = mydb.rawQuery(sql, null);

            boolean hasData = false;
            int imageIndex = c.getColumnIndexOrThrow("IMAGE");
            int imageTagsIndex = c.getColumnIndexOrThrow("TAGS");
            int dateColIndex = c.getColumnIndexOrThrow("CREATED_AT");

            for (int i = 0; i < numSearchResults; i++) {

                if (i == 0) {
                    hasData = c.moveToFirst();
                }

                Log.v(MainActivity.TAG, "getCommentItems(); index: " + i + ", hasData: " + hasData);

                // stop here if there is no more data
                if (!hasData) {
                    break;
                }

                byte[] ba = c.getBlob(imageIndex);
                String tags = c.getString(imageTagsIndex);
                if (tags == null || tags.trim().isEmpty()) {
                    tags = "Unavailable";
                }
                String imageDate = c.getString(dateColIndex);
                if (imageDate == null || imageDate.trim().isEmpty()) {
                    imageDate = "MMM DD, YYYY - HH AMPM";
                }

                Bitmap bmp = null;
                if (ba != null && ba.length > 0) {
                    Log.v(MainActivity.TAG, "Should be able to create a Bitmap object from the DB BLOB.");
                    bmp = BitmapFactory.decodeByteArray(ba, 0, ba.length);
                }

                comments.add(new CommentItem(bmp, tags, imageDate));

                hasData = c.moveToNext();
            }

            c.close();
        } catch (SQLiteException e) {
            ClickUtils.showBlockingAlert((Activity) context, "Find images database error", e.getMessage());
        } finally {
            if (mydb != null && mydb.isOpen()) {
                mydb.close();
            }
        }

        return comments;
    }

    /**
     * Generates the SQL necessary to find images based on type and a LIKE match on tags
     * @param imageType
     * @param findText
     * @return
     */
    @NonNull
    private static String getFindSQL(int imageType, String findText) {

        // We need to first identify the image that we want based on the tags, and then,
        // return all of the tags in our response
        String sql = "; WITH selected_images AS (SELECT DISTINCT t7.IMAGE_ID FROM image_tags AS t7 ";

        if (findText != null && !findText.trim().isEmpty()) {
            sql += " WHERE LOWER(t7.tag) LIKE '%' || LOWER('" + findText + "') || '%' ";
        }

        sql += ") " +
                "SELECT t1.IMAGE, GROUP_CONCAT(DISTINCT t2.TAG) AS TAGS, datetime(t1.CREATED_AT, 'localtime') AS CREATED_AT " +
                "FROM IMAGES AS t1 " +
                "INNER JOIN image_tags AS t2 ON t2.IMAGE_ID = t1.ID " +
                "INNER JOIN selected_images AS t3 ON t3.IMAGE_ID = t1.ID ";

        if (imageType > IMAGE_TYPE_BOTH) {
            sql += " WHERE t1.IMAGE_TYPE_ID = " + imageType;
        }

        sql += " GROUP BY t1.IMAGE, t1.CREATED_AT ";
        sql += " ORDER BY t1.CREATED_AT DESC";
        return sql;
    }

}
