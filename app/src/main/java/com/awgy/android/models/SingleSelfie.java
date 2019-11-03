package com.awgy.android.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;

import com.awgy.android.utils.Constants;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseObject;

import bolts.Continuation;
import bolts.Task;

@ParseClassName(Constants.CLASS_SINGLESELFIE)

public class SingleSelfie extends ParseObject {

    public void setImage(ParseFile file) {
        put(Constants.KEY_SINGLESELFIE_IMAGE, file);
    }

    public void setToGroupSelieId(String toGroupSelieId) {
        put(Constants.KEY_SINGLESELFIE_TO_GROUPSELFIE_ID, toGroupSelieId);
    }

    public void saveInBackgroundWithBitmap(Bitmap bmp, final GroupSelfie groupSelfie) {

        AsyncTask<Bitmap,Void,Void> task = new AsyncTask<Bitmap, Void, Void>() {
            @Override
            protected Void doInBackground(Bitmap... params) {

                // Resize
                int resized_width = Constants.IMAGE_CANVAS;
                if (params[0].getWidth() > params[0].getHeight())
                    resized_width = 2 * Constants.IMAGE_CANVAS + Constants.IMAGE_BORDER;
                Bitmap bmp = Bitmap.createScaledBitmap(params[0], resized_width, Constants.IMAGE_CANVAS, false);

                // Compress
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);

                // Parse File
                byte[] byteArray = stream.toByteArray();
                final ParseFile file = new ParseFile("image.jpg", byteArray);

                file.saveInBackground().continueWithTask(new Continuation<Void, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<Void> task) throws Exception {
                        if (!task.isFaulted() && !task.isCancelled()) {
                            setImage(file);
                            setToGroupSelieId(groupSelfie.getObjectId());
                            saveInBackground();
                        }
                        return null;
                    }
                });

                return null;
            }
        };

        task.execute(bmp);

    }


}