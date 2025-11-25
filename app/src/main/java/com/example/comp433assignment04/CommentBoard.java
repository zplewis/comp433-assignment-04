package com.example.comp433assignment04;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CommentBoard extends AppCompatActivity {

    CommentListAdapter adapter;

    String currentTags = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_comment_board);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Add an onclick listener to go back to home.
        Button backButton = findViewById(R.id.btnBackToHome);
        backButton.setOnClickListener(ClickUtils.backToPrevious(this));

        // findImagesOnClick
        Button findButton = findViewById(R.id.findButton);
        TextView findTextView = findViewById(R.id.findTextbox);
        ListView lv = findViewById(R.id.listSearchResults);
        CheckBox chkIncludeSketches = findViewById(R.id.chkIncludeSketches);
        TextView commentTextView = findViewById(R.id.tvSelectedTextView);


        findButton.setOnClickListener(v -> {

            // This is how you evaluate a view prior to a click
            String findText = findTextView.getText().toString();
            int imageType = (chkIncludeSketches != null && chkIncludeSketches.isChecked()
                    ? DatabaseHelper.IMAGE_TYPE_BOTH
                    : DatabaseHelper.IMAGE_TYPE_PHOTO);

            ClickUtils.findImagesOnClick(
                    this,
                    findText,
                    imageType,
                    data -> {
                        Log.v(MainActivity.TAG, "SketchTagger.onCreate(); is data null: " + (data == null));

                        if (data != null) {
                            Log.v(MainActivity.TAG, "SketchTagger.onCreate(); data size: " + data.size());
                        }
                         adapter = new CommentListAdapter(
                                this,
                                R.layout.list_item,
                                data,
                                CommentListAdapter.Mode.COMBINED_SEARCH
                        );

                        // Now that you have the adapter, you need to know when items are selected
                        adapter.setOnSelectionChangedListener((position, item) -> {
                            Log.v(MainActivity.TAG, "CommentBoard search selection changed: position=" +
                                    position + ", item=" + (item != null ? item.tags : "unavailable"));

                            currentTags = "";
                            commentTextView.setText("Please make a selection.");

                            if (item != null) {
                                currentTags = item.tags;
                                commentTextView.setText("You selected: " + currentTags);
                            }
                        });

                        lv.setAdapter(adapter);
                    }
            ).onClick(v);
        });

        // This



        // Get comments from Gemini based on the different characters
        Button commentButton = findViewById(R.id.commentButton);
            commentButton.setOnClickListener(v -> {
                        String theCurrentTags = currentTags;

                        ClickUtils.getCommentsOnClick(
                                this,
                                theCurrentTags, // this should be the tags
                                data -> {
                                }
                        ).onClick(v);
                    }
            ); // end of setOnClickListener
    } // end of onCreate
}