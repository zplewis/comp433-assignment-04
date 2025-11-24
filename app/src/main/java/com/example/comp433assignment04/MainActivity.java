package com.example.comp433assignment04;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    /**
     * This is the tag used for LogCat entries for this application.
     */
    public static final String TAG = "assignment04";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // region Home Buttons
    private void onClickHomeButtons(View view, Class activityClass) {

        if (activityClass == null) {
            return;
        }

        Intent intent = new Intent(this, activityClass);

        // Check to see if there is an app that can handle this intent. If not, then return.
        // There is a warning here:
        // Consider adding a <queries> declaration to your manifest when calling this method
        // Why? Never did it and this works.
        ComponentName componentName;
        componentName = intent.resolveActivity(getPackageManager());

        // Stop here if componentName is null; this means that no activity from any other app
        // matches our requested Intent type
        if (componentName == null) {
            Log.v(TAG, "Activity could not be found");
            return;
        }

        startActivity(intent);
    }

    public void onClickBtnPhotoTagger(View view) {
        onClickHomeButtons(view, PhotoTagger.class);
    }

    public void onClickBtnSketchTagger(View view) {
        onClickHomeButtons(view, SketchTagger.class);
    }

    public void onClickBtnCommentBoard(View view) {
        onClickHomeButtons(view, CommentBoard.class);
    }

    public void onClickBtnClearDB(View view) {
    }

    // endregion
}