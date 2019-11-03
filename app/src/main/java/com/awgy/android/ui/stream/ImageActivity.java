package com.awgy.android.ui.stream;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.awgy.android.R;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.utils.BaseActivity;
import com.awgy.android.utils.ScaleImageView;
import com.google.common.base.Joiner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ImageActivity extends BaseActivity implements ScaleImageView.ScaleImageViewDelegate {

    private static GroupSelfie staticGroupSelfie;
    private GroupSelfie mGroupSelfie;
    public static void setStaticGroupSelfie(GroupSelfie groupSelfie) {
        staticGroupSelfie = groupSelfie;
    }

    private static Bitmap staticBitmap;
    private Bitmap mBitmap;
    public static void setStaticBitmap(Bitmap bitmap) {
        staticBitmap = bitmap;
    }

    private ImageButton mCloseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        //getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_image);

        if (staticGroupSelfie != null) {
            mGroupSelfie = staticGroupSelfie;
            staticGroupSelfie = null;
        }

        if (staticBitmap != null) {
            mBitmap = staticBitmap;
            staticBitmap = null;
        }

        ScaleImageView scaleImageView = (ScaleImageView) findViewById(R.id.scaleImageView);
        scaleImageView.setImageBitmap(mBitmap);
        scaleImageView.setDelegate(this);

        mCloseButton = (ImageButton) findViewById(R.id.closeButton);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (staticGroupSelfie == null) {
            staticGroupSelfie = mGroupSelfie;
        }

        if (staticBitmap == null) {
            staticBitmap = mBitmap;
        }

    }

    @Override
    public void onLongPress(View view) {

        PopupMenu popupMenu = new PopupMenu(ImageActivity.this, mCloseButton) {
            @Override
            public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_overflow_save:

                        MediaStore.Images.Media.insertImage(getContentResolver(), mBitmap, String.format("#%s", mGroupSelfie.getHashtag()), null);
                        Toast.makeText(ImageActivity.this, getResources().getString(R.string.image_saved), Toast.LENGTH_SHORT).show();

                        return true;

                    case R.id.menu_overflow_email:

                        File file = savebitmap(mBitmap);

                        try {
                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
                            emailIntent.setType("application/image");
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, String.format("Awgy: #%s", mGroupSelfie.getHashtag()));
                            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                            startActivity(emailIntent);
                        } catch (Exception e) {

                        }

                        return true;

                    default:
                        return super.onMenuItemSelected(menu, item);
                }
            }
        };

        popupMenu.inflate(R.menu.menu_image);
        popupMenu.show();

    }

    private File savebitmap(Bitmap bmp) {

        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        OutputStream outStream = null;

        File file = new File(extStorageDirectory, "awgy.jpg");
        if (file.exists()) {
            file.delete();
            file = new File(extStorageDirectory, "awgy.jpg");
        }

        try {
            outStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return file;

    }

}
