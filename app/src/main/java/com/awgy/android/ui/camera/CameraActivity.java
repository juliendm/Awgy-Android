package com.awgy.android.ui.camera;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.awgy.android.R;
import com.awgy.android.models.Activity;
import com.awgy.android.models.GroupSelfie;
import com.awgy.android.models.PhoneBook;
import com.awgy.android.models.Relationship;
import com.awgy.android.models.SingleSelfie;
import com.awgy.android.ui.main.MainActivity;
import com.awgy.android.ui.ongoing.OngoingFragment;
import com.awgy.android.utils.BaseActivity;
import com.awgy.android.utils.Constants;
import com.google.common.base.Joiner;
import com.parse.ParseCloud;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import bolts.Continuation;
import bolts.Task;

public class CameraActivity extends BaseActivity {

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

    private static boolean staticNeedResetBitmap = false;
    public static void setNeedResetBitmap(boolean needResetBitmap) {
        staticNeedResetBitmap = needResetBitmap;
    }
    private static boolean staticNeedResetGroupSelfie = false;
    public static void setNeedResetGroupSelfie(boolean needResetGroupSelfie) {
        staticNeedResetGroupSelfie = needResetGroupSelfie;
    }

    private List<Relationship> mSponsored;
    private List<String> mWarnings;
    private List<String> mWarningsCopy;

    private Camera mCamera;
    private int mCameraIndex = 0;

    private CameraPreview mPreview;

    private FrameLayout mCameraPreview;
    private ImageView mImagePreview;

    private TextView mAttendeesTextView;

    private TextView mDetailsLabel;

    private Button mSnapButton;
    private ImageButton mRedoButton;
    private ImageButton mProceedButton;

    private View mFlash;
    private float mInitialBrightness;

    private boolean mPhotoTaken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (staticNeedResetBitmap) {
            staticNeedResetBitmap = false;
            staticBitmap = null;
        }

        if (staticNeedResetGroupSelfie) {
            staticNeedResetGroupSelfie = false;
            staticGroupSelfie = null;
        }

        mAttendeesTextView = (TextView) findViewById(R.id.attendeesLabel);

        mDetailsLabel =  (TextView) findViewById(R.id.detailsField);

        mPhotoTaken = false;

        mSnapButton = (Button) findViewById(R.id.snapButton);
        mSnapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPhotoTaken) {
                    snapButtonAction();
                }
            }
        });

        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) {

            int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
            int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());

            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.width = size;
            rlp.height = size;
            rlp.rightMargin = margin;
            rlp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

            mSnapButton.setLayoutParams(rlp);

        }

        mRedoButton = (ImageButton) findViewById(R.id.redoButton);
        mRedoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoTaken) {
                    redoButtonAction();
                }
            }
        });
        mRedoButton.setVisibility(View.INVISIBLE);

        mProceedButton = (ImageButton) findViewById(R.id.proceedButton);
        mProceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoTaken) {
                    proceedButtonAction();
                }
            }
        });
        mProceedButton.setVisibility(View.INVISIBLE);

        mFlash = findViewById(R.id.flashView);
        mFlash.setVisibility(View.INVISIBLE);

        // Notification
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mPhonebookPhonebookHasBeenUpdatedNotification, new IntentFilter(Constants.PHONEBOOK_PHONEBOOK_HAS_BEEN_UPDATED_NOTIFICATION));

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

        // Notification
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mPhonebookPhonebookHasBeenUpdatedNotification);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCamera == null) {

            mCamera = getCameraInstance();
            mCamera.setDisplayOrientation(getCameraOrientation(CameraActivity.this, mCameraIndex, true));

            mCameraPreview = (FrameLayout) findViewById(R.id.camera_preview);
            mImagePreview = (ImageView) findViewById(R.id.image_preview);
            mImagePreview.setVisibility(View.INVISIBLE);

            Point size = new Point();
            getWindowManager().getDefaultDisplay().getSize(size);

            layoutCamera(mCamera, mCameraPreview, mImagePreview, size);

            mPreview = new CameraPreview(this);
            mCameraPreview.addView(mPreview);

        }

        // Initialize

        if (staticGroupSelfie != null) {
            mGroupSelfie = staticGroupSelfie;
            staticGroupSelfie = null;

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                if (mGroupSelfie.getHashtag() != null) {
                    actionBar.setTitle(String.format("#%s", mGroupSelfie.getHashtag()));
                } else {
                    actionBar.setTitle("SMILE!");
                }
            }

            if (mGroupSelfie.getDetails() != null) {
                mDetailsLabel.setText(mGroupSelfie.getDetails());
                mDetailsLabel.setBackgroundColor(Color.parseColor("#80474747"));
            } else {
                mDetailsLabel.setText("");
                mDetailsLabel.setBackgroundColor(Color.TRANSPARENT);
            }

        }

        if (staticBitmap != null) {
            mBitmap = staticBitmap;
            staticBitmap = null;
        }

        // Check if need additional info

        checkWithDatabase();

        // Update List

        updateListOfPeople();

        // Buttons and Session

        if (mGroupSelfie != null && mGroupSelfie.getObjectId() != null) {
            mProceedButton.setImageResource(R.drawable.send_color);
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            mProceedButton.setPadding(padding,padding,padding,padding);
        }

        if (mBitmap != null) {

            mPhotoTaken = true;

            mImagePreview.setImageBitmap(mBitmap);
            mImagePreview.setVisibility(View.VISIBLE);

            mSnapButton.setVisibility(View.INVISIBLE);
            mRedoButton.setVisibility(View.VISIBLE);
            mProceedButton.setVisibility(View.VISIBLE);

        } else {

            mPhotoTaken = false;

            mImagePreview.setImageBitmap(null);
            mImagePreview.setVisibility(View.INVISIBLE);

            mSnapButton.setVisibility(View.VISIBLE);
            mRedoButton.setVisibility(View.INVISIBLE);
            mProceedButton.setVisibility(View.INVISIBLE);

            //dispatch_async(self.sessionQueue, ^{
            //        [self.session startRunning];
            //});

        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCamera != null){
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }
            mCamera.release();
            mCamera = null;
            mPreview.destroy();
            mPreview = null;
            mCameraPreview = null;
        }

    }


    public void checkWithDatabase() {

        if (mGroupSelfie != null && mGroupSelfie.getObjectId() != null) {

            // Warnings
            if (mGroupSelfie.getSponsoredIds() != null) {

                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereContainedIn(Constants.KEY_OBJECT_ID,mGroupSelfie.getSponsoredIds());
                query.findInBackground().continueWithTask(new Continuation<List<ParseUser>, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<List<ParseUser>> task) throws Exception {
                        if (!task.isFaulted() && !task.isCancelled()) { // If error, do not initialise so user cannot send picture
                            mWarnings = new ArrayList<String>();
                            for (ParseUser sponsor : task.getResult()){
                                if (sponsor.getString(Constants.KEY_USER_WARNING) != null) mWarnings.add(sponsor.getString(Constants.KEY_USER_WARNING));
                            }
                        }
                        return null;
                    }
                });

            } else {
                mWarnings = new ArrayList<String>();
            }

        } else {

            // Sponsors
            ParseCloud.callFunctionInBackground(Constants.KEY_RELATIONSHIP_SPONSORED_FUNCTION, new HashMap<String, Object>()).continueWithTask(new Continuation<Object, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Object> task) throws Exception {
                    if (!task.isFaulted() && !task.isCancelled()) {
                        List<Relationship> sponsored = new ArrayList<Relationship>();
                        List<HashMap<String,Object>> res = (List<HashMap<String,Object>>) task.getResult();
                        for (HashMap<String,Object> data : res) {
                            Relationship relationship = (Relationship) data.get("rel");
                            relationship.setWarning((String) data.get("warn"));
                            sponsored.add(relationship);
                        }
                        mSponsored = sponsored ;
                    } else {
                        mSponsored = new ArrayList<Relationship>();
                    }
                    return null;
                }
            });

        }

    }

    public void redoButtonAction() {
        mBitmap = null;
        mPhotoTaken = false;

        mImagePreview.setVisibility(View.INVISIBLE);
        mSnapButton.setVisibility(View.VISIBLE);
        mRedoButton.setVisibility(View.INVISIBLE);
        mProceedButton.setVisibility(View.INVISIBLE);
    }

    public void proceedButtonAction() {

        if (isNetworkAvailable()) {

            if (mBitmap != null) {

                if (mGroupSelfie != null && mGroupSelfie.getObjectId() != null) {

                    if (mWarnings != null) {

                        mWarningsCopy = new ArrayList<String>(mWarnings);

                        if (mWarningsCopy.size() > 0) {
                            showWarning();
                        } else {
                            saveImage();
                        }

                    }

                } else {

                    if (mSponsored != null) {

                        SetUpActivity.getStaticRecipientsAdpater().setStaticSponsored(mSponsored);
                        SetUpActivity.getStaticRecipientsAdpater().loadCache().continueWithTask(new Continuation<Void, Task<Void>>() {
                            @Override
                            public Task<Void> then(Task<Void> task) throws Exception {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SetUpActivity.setStaticBitmap(mBitmap);
                                        SetUpActivity.setStaticGroupSelfie(mGroupSelfie);
                                        SetUpActivity.setDelegate(MainActivity.getSetUpDelegate());
                                        Intent intent = new Intent(CameraActivity.this, SetUpActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                        startActivity(intent);
                                    }
                                });
                                return null;
                            }
                        });

                    }
                }

            }

        }

    }

    public void showWarning() {

        AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
        builder.setMessage(mWarningsCopy.get(0))
                .setTitle(getResources().getString(R.string.warning))
                .setPositiveButton(getResources().getString(R.string.accept), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (mWarningsCopy.size() > 0) {
                            showWarning();
                        } else {
                            saveImage();
                        }

                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel), null);
        mWarningsCopy.remove(0);
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void saveImage() {

        if (mDelegate != null) {
            mDelegate.onCameraSendButtonClick(mGroupSelfie, mBitmap);
        }

        if (mGroupSelfie.getSeenIds() == null || !mGroupSelfie.getSeenIds().contains(ParseUser.getCurrentUser().getObjectId())) {

            mGroupSelfie.addSeenId(false, false).continueWithTask(new Continuation<Void, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Void> task) throws Exception {

                    OngoingFragment.getStaticOngoingAdapter().loadCache().continueWithTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Void> task) throws Exception {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MainActivity.setTabToSelectOnResume(1);
                                    Intent intent = new Intent(CameraActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                }
                            });
                            return null;
                        }
                    });

                    return null;
                }
            });

        }

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (!mPhotoTaken) {
                        snapButtonAction();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (!mPhotoTaken) {
                        snapButtonAction();
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public void snapButtonAction() {

        mPhotoTaken = true;

        final WindowManager.LayoutParams layout = getWindow().getAttributes();
        mInitialBrightness = layout.screenBrightness;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFlash.setVisibility(View.VISIBLE);
                layout.screenBrightness = 1.0f;
                getWindow().setAttributes(layout);

                mSnapButton.setVisibility(View.INVISIBLE);

                if (mCamera != null) {
                    mCamera.takePicture(shutterCallback, null, pictureCallback);
                }
            }
        });

    }

    private final Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
        }
    };

    private PictureCallback pictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRedoButton.setVisibility(View.VISIBLE);
                    mProceedButton.setVisibility(View.VISIBLE);

                    mFlash.setVisibility(View.INVISIBLE);
                    WindowManager.LayoutParams layout = getWindow().getAttributes();
                    layout.screenBrightness = mInitialBrightness;
                    getWindow().setAttributes(layout);
                }
            });

            // Original Bitmap
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

            // Rotate
            Matrix matrix = new Matrix();
            matrix.postRotate(getCameraOrientation(CameraActivity.this, mCameraIndex, false));
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

            // Crop
            int size_width = bmp.getWidth();
            int size_height;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                size_height = size_width;
            } else {
                size_height = (int) (size_width * Constants.IMAGE_CANVAS / (2.0 * Constants.IMAGE_CANVAS + Constants.IMAGE_BORDER));
            }
            bmp = Bitmap.createBitmap(bmp,
                    (int) (0.5 * (bmp.getWidth() - size_width)),
                    (int) (0.5 * (bmp.getHeight() - size_height)),
                    size_width,
                    size_height);

            mBitmap = bmp;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mImagePreview.setImageBitmap(mBitmap);
                    mImagePreview.setVisibility(View.VISIBLE);
                    mCamera.startPreview();
                }
            });

        }
    };

    public Camera getCameraInstance(){

        int count = 0;
        Camera c = null;
        Camera.CameraInfo info = new Camera.CameraInfo();
        count = Camera.getNumberOfCameras();
        for (int index = 0; index < count; index++) {
            Camera.getCameraInfo(index, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCameraIndex = index;
                try {
                    c = Camera.open(index);
                } catch (Exception e) {
                    // Camera is not available (in use or does not exist)
                }
                break;
            }
        }
        return c;

    }

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        private SurfaceHolder mHolder;

        public CameraPreview(Context context) {
            super(context);

            mHolder = getHolder();
            mHolder.addCallback(this);
        }

        public void destroy() {
            mHolder.removeCallback(this);
            mHolder = null;
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // empty. surfaceChanged will take care of stuff
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

            if(mHolder != null) {

                if (mHolder.getSurface() == null) {
                    return;
                }

                try {
                    mCamera.stopPreview();
                } catch (Exception e) {
                    // ignore: tried to stop a non-existent preview
                }

                try {

                    Camera.Parameters params = mCamera.getParameters();


                    Camera.Size previewSize = params.getPreviewSize();

                    Camera.Size pictureSize = getOptimalPictureSize(params.getSupportedPictureSizes(), (int)Math.ceil(1000.0*(double)previewSize.width/(double)previewSize.height), 1000);

                    if (pictureSize != null) {

                        params.setPictureSize(pictureSize.width, pictureSize.height);
                        mCamera.setParameters(params);
                    }

                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();

                } catch (Exception e) {

                }

            }
        }

    }

    // Delegate

    public interface CameraActivityDelegate {
        void onCameraSendButtonClick(GroupSelfie groupSelfie, Bitmap bitmap);
    }

    private static CameraActivity.CameraActivityDelegate mDelegate;

    public static void setDelegate(CameraActivity.CameraActivityDelegate delegate) {
        mDelegate = delegate;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }

    public void layoutCamera(Camera camera, FrameLayout cameraPreview, ImageView imagePreview, Point size) {

        Camera.Parameters parameters = camera.getParameters();
        double ratio = (double)parameters.getPreviewSize().width/(double)parameters.getPreviewSize().height;
        int width;
        int height;
        int targetHeight;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            targetHeight = size.x;
            width = targetHeight;
            height = (int) Math.ceil(width*ratio);
        } else {
            int actionBarHeight = 0;
            TypedValue typedValue = new TypedValue();
            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true))
                actionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
            targetHeight = size.y-actionBarHeight;
            width = (int) Math.ceil(targetHeight*(2.0*Constants.IMAGE_CANVAS+Constants.IMAGE_BORDER)/Constants.IMAGE_CANVAS);
            if (width > size.x) {
                width = size.x;
                targetHeight = (int) Math.ceil(width*Constants.IMAGE_CANVAS/(2.0*Constants.IMAGE_CANVAS+Constants.IMAGE_BORDER));
            }
            height = (int) Math.ceil(width/ratio);
        }
        int marginHeight = (int) Math.ceil(0.5 * (targetHeight - height));

        // Camera Relative Layout
        RelativeLayout cameraRelativeLayout = (RelativeLayout) findViewById(R.id.cameraRelativeLayout);
        RelativeLayout.LayoutParams cameraRelativeLayoutParams = (RelativeLayout.LayoutParams)cameraRelativeLayout.getLayoutParams();
        cameraRelativeLayoutParams.height = height;
        cameraRelativeLayoutParams.topMargin = marginHeight;
        cameraRelativeLayoutParams.bottomMargin = marginHeight;
        cameraRelativeLayout.setLayoutParams(cameraRelativeLayoutParams);

        // Camera Preview
        ViewGroup.LayoutParams cameraPreviewParams = cameraPreview.getLayoutParams();
        cameraPreviewParams.width = width;
        cameraPreviewParams.height = height;
        cameraPreview.setLayoutParams(cameraPreviewParams);

        // Image Preview
        imagePreview.getLayoutParams().height = targetHeight;

    }

    private Camera.Size getOptimalPictureSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public int getCameraOrientation(android.app.Activity activity, int cameraId, boolean mirror) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            if (mirror)
                result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;

    }

    // Helper Methods

    public void updateListOfPeople() {

        if (mGroupSelfie != null && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            PhoneBook phoneBook = PhoneBook.getInstance();

            final List<String> namesIn = new ArrayList<String>();
            for (String username : mGroupSelfie.getGroupUsernames()) {
                if (!username.equals(ParseUser.getCurrentUser().getUsername())) {
                    namesIn.add(phoneBook.getFirstName(username));
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (namesIn.size() > 0) {
                        if (mAttendeesTextView != null)
                            mAttendeesTextView.setText(Joiner.on(", ").join(namesIn));
                    } else {
                        if (mAttendeesTextView != null) mAttendeesTextView.setText(null);
                    }
                }
            });

        } else {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mAttendeesTextView != null) mAttendeesTextView.setText(null);
                }
            });

        }

    }


    // Notification

    private BroadcastReceiver mPhonebookPhonebookHasBeenUpdatedNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    // Network

    private boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean available = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        if (!available) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
            builder.setMessage(getResources().getString(R.string.no_internet))
                    .setTitle(getResources().getString(R.string.oops))
                    .setPositiveButton(android.R.string.ok, null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        return available;

    }

}
