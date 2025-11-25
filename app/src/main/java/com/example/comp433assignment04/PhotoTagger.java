package com.example.comp433assignment04;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoTagger extends AppCompatActivity {

    int imageIndex = -1;

    String currentPhotoPath;

    // Used to uniquely identify the "session of using the camera" to capture an image
    int REQUEST_IMAGE_CAPTURE = 1000;

    CommentListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_photo_tagger);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Add an onclick listener to go back to home.
        Button backButton = findViewById(R.id.btnBackToHome);
        backButton.setOnClickListener(ClickUtils.backToPrevious(this));

        Button saveButton = findViewById(R.id.saveButton);
        ImageView iv = findViewById(R.id.ivMain);
        TextView tagsTextView = findViewById(R.id.tagsTextbox);
        saveButton.setOnClickListener(
            ClickUtils.saveImageOnClick(this, iv, tagsTextView)
        );

        // findImagesOnClick
        Button findButton = findViewById(R.id.findButton);
        TextView findTextView = findViewById(R.id.findTextbox);
        ListView lv = findViewById(R.id.photolist);
        findButton.setOnClickListener(v -> {
            String findText = findTextView.getText().toString();

            ClickUtils.findImagesOnClick(
                    this,
                    findText,
                    DatabaseHelper.IMAGE_TYPE_PHOTO,
                    data -> {
                        this.adapter = new CommentListAdapter(this, R.layout.list_item, data, CommentListAdapter.Mode.NORMAL);
                        lv.setAdapter(adapter);
                    }
            ).onClick(v);
        });

        // get tags on click
        Button tagsButton = findViewById(R.id.getTagsButton);
        tagsButton.setOnClickListener(
            // the callback parameter of this function MUST NOT be NULL
            ClickUtils.getGeminiTagsOnClick(this, iv, tagsTextView, data -> {})
        );
    }

    // region Button Clicks

    public void onClickCameraBtn(View view) {

        Log.v(MainActivity.TAG, "Camera clicked from from PhotoTagger");

        // There are two types of Intent objects: explicit (when you specify the class),
        // and implicit, when you are asking for whether an app can meet the need without having
        // to know the class
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Check to see if there is an app that can handle this intent. If not, then return.
        // There is a warning here:
        // Consider adding a <queries> declaration to your manifest when calling this method
        // Why? Never did it and this works.
        ComponentName componentName = takePictureIntent.resolveActivity(getPackageManager());

        // Stop here if componentName is null; this means that no activity from any other app
        // matches our requested Intent type
        if (componentName == null) {
            Log.v(MainActivity.TAG, "No app found to take the picture.");
            return;
        }

        // Create the File where the photo should go
        File photoFile;

        try {
            // This will always be not null unless an error occurs
            photoFile = createImageFile();

        } catch (IOException ex) {

            Log.v(MainActivity.TAG, "Error occurred creating the image file.");
            return;
        }

        Uri photoURI = FileProvider.getUriForFile(this,
                "com.example.comp433assignment04.fileprovider", // "com.example.android.fileprovider",
                photoFile);

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    /**
     * This method waits for the picture to be returned from the camera and then updates
     * the imageview. Without using this, the application will be checking for the photo
     * before it exists yet.
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.v(MainActivity.TAG, "The camera activity has been returned.");

            File recentPhoto = new File(currentPhotoPath);

            if (!recentPhoto.isFile()) {
                Log.v(MainActivity.TAG, "The file for the newest photo does NOT exist.");
                return;
            }

            // This shows the picture on the screen
            ImageView iv = findViewById(R.id.ivMain);
            // Update the imageview with the appropriate image
            Bitmap image = BitmapFactory.decodeFile(currentPhotoPath);
            iv.setImageBitmap(image);
        }

        // reset the absolute path for the next photo
        currentPhotoPath = "";

        // set the image index
        imageIndex = 0;

        // increment the REQUEST_IMAGE_CAPTURE by 1
        REQUEST_IMAGE_CAPTURE++;
    }

    // endregion

    // region Helper Functions

    /**
     * Returns a File object for saving the full-size photo.
     * @return File
     * @throws IOException
     * <a href="https://developer.android.com/media/camera/camera-deprecated/photobasics#TaskPath">...</a>
     */
    private File createImageFile() throws IOException {
        // Create the filename first
        // The Locale.US is optional, sets the timezone for the date
        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
        ).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".png";

        // Seems like you have to create a File object for the parent directory of the photo
        // that will be returned from the camera
        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                imageDir      /* directory */
        );

        // save the absolute path of the image file (just in case, I'm not sure it's needed)
        currentPhotoPath = image.getAbsolutePath();

        return image;
    }

    // endregion
}