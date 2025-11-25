package com.example.comp433assignment04;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class CommentListAdapter extends ArrayAdapter<CommentItem> {

    /**
     * Allows setting the "mode" for the comment list, which will configure the layout accordingly.
     */
    public enum Mode {
        NORMAL,
        COMBINED_SEARCH,
        GENERATED_COMMENTS
    }

    /**
     * Variable for storing the mode that will configure the ListView.
     */
    private Mode mode = Mode.NORMAL;

    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects
     */
    public CommentListAdapter(Context context, int resource, ArrayList<CommentItem> objects, Mode mode) {
        // the "objects" parameter is required here for this to work!
        super(context, resource, objects);
        Log.v(MainActivity.TAG, "CommentListAdapter.Constructor(); is mode null: " + (mode == null));
        this.mode = mode;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item, parent, false);
        }
        CommentItem currentItem = getItem(position);

        if (currentItem == null) {
            Log.d(MainActivity.TAG, "CommentListAdapter.getView(); currentItem is null");
            return convertView;
        }

        ImageView cImage = convertView.findViewById(R.id.photo);
        TextView cName = convertView.findViewById(R.id.name);
        TextView cComment = convertView.findViewById(R.id.comment);
        CheckBox checkbox = convertView.findViewById(R.id.chkSelectedImage);
        LinearLayout commentSection = convertView.findViewById(R.id.commentSection);
        TextView commentDateTime = convertView.findViewById(R.id.commentDateTime);

        View[] views = {cImage, cName, cComment, checkbox, commentSection, commentDateTime};

        for (View view : views) {
            if (view == null) {
                Log.e(MainActivity.TAG, "CommentListAdapter.getView(); one of the views is null.");
            }
        }

        cImage.setImageBitmap(currentItem.photo);
        cImage.setBackgroundColor(Color.parseColor("#f0f0f0"));
        cName.setText(currentItem.tags);
        cComment.setText(currentItem.date);

        checkbox.setVisibility(View.GONE);
        commentSection.setBackground(null);
        commentDateTime.setVisibility(View.GONE);

        if (mode == Mode.COMBINED_SEARCH) {
            checkbox.setVisibility(View.VISIBLE);
        }

        if (mode == Mode.GENERATED_COMMENTS) {
            commentSection.setBackgroundColor(Color.parseColor("#f0f0f0"));
            commentDateTime.setVisibility(View.VISIBLE);
        }

        return convertView;
    }
}

