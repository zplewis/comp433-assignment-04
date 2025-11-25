package com.example.comp433assignment04;

import android.graphics.Bitmap;

public class CommentItem {
    Bitmap photo;
    String description, title, date;

    CommentItem(Bitmap photo, String title, String description) {
        this.photo = photo;
        this.title = title;
        this.description = description;
    }

    CommentItem(Comment comment) {
        this.photo = comment.commenter.bitmap;
        this.title = comment.commenter.name;
        this.description = comment.text;
        this.date = comment.commentDate;
    }
}
