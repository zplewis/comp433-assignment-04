package com.example.comp433assignment04;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SketchTagger extends AppCompatActivity {

    CommentListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sketch_tagger);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Add an onclick listener to go back to home.
        Button backButton = findViewById(R.id.btnBackToHome);
        backButton.setOnClickListener(ClickUtils.backToPrevious(this));

        Button saveButton = findViewById(R.id.saveButton);
        MyDrawingArea mda = findViewById(R.id.mydrawingarea_main);
        TextView tagsTextView = findViewById(R.id.tagsTextbox);
        saveButton.setOnClickListener(
                ClickUtils.saveImageOnClick(this, mda, tagsTextView)
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
                    DatabaseHelper.IMAGE_TYPE_SKETCH,
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
                ClickUtils.getGeminiTagsOnClick(this, mda, tagsTextView, data -> {})
        );
    }

    /**
     * Clears the drawing canvas and any tags.
     * @param view
     */
    public void onClickClearBtn(View view) {

        MyDrawingArea myDrawingArea = findViewById(R.id.mydrawingarea_main);
        myDrawingArea.resetPath();

        TextView textView = findViewById(R.id.tagsTextbox);
        textView.setText("");
    }
}