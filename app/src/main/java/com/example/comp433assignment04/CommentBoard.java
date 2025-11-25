package com.example.comp433assignment04;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class CommentBoard extends AppCompatActivity {

    /**
     * This adapter is for the search results list at the top of the activity.
     */
    CommentListAdapter adapter;

    /**
     * This adapter is for the comments that are posted about the selected image at the bottom of
     * the activity.
     */
    CommentListAdapter commentListAdapter;

    /**
     * Stores the tags of the currently selected image.
     */
    String currentTags = "";

    ArrayList<Commenter> commenters;

    ArrayList<CommentItem> searchResults = new ArrayList<>();

    ArrayList<CommentItem> allComments;

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

        // Get the commenters and the styles
        getCommentersFromDrawables();

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
                                    position + ", item=" + (item != null ? item.title : "unavailable"));

                            currentTags = "";
                            commentTextView.setText("Please make a selection.");

                            if (item != null) {
                                currentTags = item.title;
                                commentTextView.setText("You selected: " + currentTags);
                            }
                        });

                        lv.setAdapter(adapter);
                    }
            ).onClick(v);
        });

        // initialize the arraylist that keeps up with the comments at the bottom of the activity




        // Get comments from Gemini based on the different characters
        Button commentButton = findViewById(R.id.commentButton);
        ListView listCommentResults = findViewById(R.id.listCommentResults);
        allComments = new ArrayList<>();
        commentListAdapter = new CommentListAdapter(
                this,
                R.layout.list_item,
                allComments,
                CommentListAdapter.Mode.GENERATED_COMMENTS
        );
        listCommentResults.setAdapter(commentListAdapter);

            commentButton.setOnClickListener(v -> {
                        String theCurrentTags = currentTags;

                        // Empty case: no image is selected
                        if (currentTags == null || currentTags.isEmpty()) {
                            ClickUtils.showToastOnClick(this, "Must select an image to post new comments.");
                            return;
                        }

                        // Empty case: we have no commenters
                        if (commenters == null || commenters.isEmpty()) {
                            ClickUtils.showBlockingAlert(this, "Fatal Error", "Could not find commenters from drawables.");
                        }

                        ClickUtils.getCommentsOnClick(
                                this,
                                theCurrentTags, // this should be the tags
                                commenters,
                                allComments,
                                data -> {

                                    allComments.addAll(data);

                                    runOnUiThread(() -> {
                                        commentListAdapter.notifyDataSetChanged();
                                        // listCommentResults.setAdapter(commentListAdapter);
                                    });
                                }
                        ).onClick(v);
                    }
            ); // end of setOnClickListener
    } // end of onCreate

    /**
     * Loop through the drawables that contains the pictures and add comments.
     */
    public void getCommentersFromDrawables() {
        commenters = new ArrayList<>();

        // get all fields (resource names) from R.drawable
        Field[] fields = R.drawable.class.getFields();

        for (Field field : fields) {
            try {
                String name = field.getName();

                // skip this drawable if the name starts with "ic_launcher" which was not
                // created by me
                if (name.startsWith("ic_launcher")) {
                    continue;
                }

                // Split the name by the "_"
                String[] nameParts = name.split("_");

                if (nameParts.length < 2) {
                    continue;
                }

                // Get the resource Id so that you can get the drawable itself
                int resourceId = field.getInt(null);

                Commenter commenter = new Commenter();
                commenter.name = getTitleCase(nameParts[0]);
                commenter.commentStyle = nameParts[1];
                commenter.resourceId = resourceId;

                Log.v(MainActivity.TAG, "commenter name: " + commenter.name +
                        ", style: " + commenter.commentStyle + ", resourceId: " +
                        commenter.resourceId);

                Resources resources = getResources();
                commenter.bitmap = BitmapFactory.decodeResource(resources, resourceId);

                commenters.add(commenter);

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        Log.v(MainActivity.TAG, "getCommentersFromDrawable(); # of commenters: " + commenters.size());
    }

    /**
     * Converts a string to title case, meaning that the first letter of each word is capitalized.
     * @param word
     * @return
     */
    public static String getTitleCase(String word) {

        // Split words and capitalize each one
        word = word.replace("_", " ");

        String[] words = word.split("\\s+");

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String current = words[i];
            if (current.isEmpty()) { continue; }

            sb.append(Character.toUpperCase(current.charAt(0)));
            if (current.length() > 1) {
                sb.append(current.substring(1).toLowerCase());
            }

            if (i < words.length - 1) {
                sb.append(" ");
            }
        } // end of for

        return sb.toString();
    }
}