package com.chinamobile.cmti.sesame;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.chinamobile.cmti.faceclassification.R;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PhotoIntentActivity2 extends AppCompatActivity {

    private static final int ACTION_TAKE_PHOTO_B = 1;
    private static final int ACTION_TAKE_PHOTO_S = 2;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;

    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY =
            "imageviewvisibility";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final String galleryId = "employees";
    private static final String selector = "FULL";
    private static final String threshold = "0.75";
    private static final String minHeadScale = "0.25";
    private static final String maxNumResults = "2";
    private static final String SERVER_URL =
            "http://192.168.1.31:8019/face_recognition/recognizer/";
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse
            ("image/png");

    private ImageView mImageView;
    private Bitmap mImageBitmap;
    private ImageButton mPicSBtn;
    //private TextView mTextView;
    private WebView resultView;
    private String mCurrentPhotoPath;
    private MediaPlayer mp;
    Button.OnClickListener mTakePicSOnClickListener;
    private Bitmap thumbnail = null;
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

    private ProgressDialog mDialog;
    private TTSManager ttsManager = null;
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Indicates whether the specified action can be used as an
     * intent. This
     * method queries the package manager for installed packages that
     * can
     * respond to an intent with the specified action. If no suitable
     * package is
     * found, this method returns false.
     * http://android-developers.blogspot
     * .com/2009/01/can-i-use-this-intent.html
     *
     * @param context The application's environment.
     * @param action  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be
     * sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String
            action) {
        final PackageManager packageManager = context
                .getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /* Photo album for this application */
    private String getAlbumName() {
        return getString(R.string.album_name);
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {

            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir
                    (getAlbumName());

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not " +
                    "mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new
                Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX,
                albumF);
        return imageF;
    }

    private File setUpPhotoFile() throws IOException {

        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();

        return f;
    }

    private void setPic() {

		/* There isn't enough memory to open up more than a couple camera
        photos */
        /* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
//        thumbnail = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

		/* Associate the Bitmap to the ImageView */
//        mImageView.setImageBitmap(thumbnail);
        mImageView.setImageBitmap(bitmap);
        //mImageView.setVisibility(View.VISIBLE);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action" +
                ".MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchTakePictureIntent(int actionCode) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT",
                1);
        takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING",
                android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
        takePictureIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA",
                true);

        switch (actionCode) {
            case ACTION_TAKE_PHOTO_B:
                File f = null;

                try {
                    f = setUpPhotoFile();
                    mCurrentPhotoPath = f.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri
                            .fromFile(f));
                } catch (IOException e) {
                    e.printStackTrace();
                    f = null;
                    mCurrentPhotoPath = null;
                }
                break;

            default:
                break;
        } // switch

        startActivityForResult(takePictureIntent, actionCode);
    }

    private void handleSmallCameraPhoto(Intent intent) {
        Bundle extras = intent.getExtras();
        mImageBitmap = (Bitmap) extras.get("data");

        // classify the image just taken
   /*     try {
            // setup a progressdialog and set the init state to false

            mDialog.show();

            //charlie: launch asynctask to handle the face recog
            myKairos.recognize(mImageBitmap,
                    galleryId,
                    selector,
                    threshold,
                    minHeadScale,
                    maxNumResults,
                    kairosListener);
            //
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (mImageBitmap.compress(Bitmap.CompressFormat.PNG,
                100, out)) {
            new FaceRecogTask().execute(out);
        } else {
            // pop-up message: image data conversion failure
        }
        mImageView.setImageBitmap(mImageBitmap);
    }

    private void handleBigCameraPhoto() {

        if (mCurrentPhotoPath != null) {
            setPic();
            galleryAddPic();
            thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory
                    .decodeFile(mCurrentPhotoPath), 32, 32);
//            FaceClassification faceClassification =
// FaceClassificationService.classifyImage("input.jpg", mImageBitmap);
//            mTextView.setText(faceClassification.toString());
//            mTextView.setVisibility(View.VISIBLE);
            mCurrentPhotoPath = null;
        }

    }


    private class FaceRecogTask extends AsyncTask<ByteArrayOutputStream,
            Void, String> {

        protected String doInBackground(ByteArrayOutputStream... out) {
            try {


            } catch (Exception e) {
                e.printStackTrace();
            }

            byte[] bytes = out[0].toByteArray();
            RequestBody reqbody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "face.png",
                            RequestBody.create(MEDIA_TYPE_PNG, bytes))
                    .build();

            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(reqbody)
                    .build();

            Response response = null;
            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful())
                    throw new IOException("Unexpected code " + response);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (response == null) {
                return null;
            }

            try {
                String jsonData = response.body().string();
                JSONObject Jobject = new JSONObject(jsonData);
                String name = Jobject.getString("name");
                Boolean success = Jobject.getBoolean("success");
                Double simi = Jobject.getDouble("similarity");
                if (name != null && success && simi > 0.5) {
                    return name;
                } else
                    return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }


        protected void onPostExecute(String result) {
            //show ui on main
            if ( result == null)
                popAlexaInfo();
            else
                welcome(result);
        }
    }

    private void welcome(String name) {
        String msg = "Welcome, " + name + ", " +
                "the door is opened and the light " +
                "is turned on for you";

        Toast.makeText(getApplicationContext(),
                msg, Toast.LENGTH_LONG)
                .show();

        ttsManager.addQueue(msg);
        IoTManager.getInstance().setLifx(name);
        IoTManager.getInstance().unlockSchlage();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_intent);

        mImageView = (ImageView) findViewById(R.id.imageView1);
        mImageBitmap = null;

        mp = MediaPlayer.create(this, R.raw
                .doorbell_sound);
        mTakePicSOnClickListener = new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mp.start();
                dispatchTakePictureIntent(ACTION_TAKE_PHOTO_S);
            }
        };
        mPicSBtn = (ImageButton) findViewById(R.id.btnIntendS);
        setListenerOrDisable(
                mPicSBtn,
                mTakePicSOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }
        //mTextView = (TextView) findViewById(R.id.textView);
        //mTextView.setVisibility(View.INVISIBLE);

        // create a ProgressDialog
        mDialog = new ProgressDialog(this);
        // set indeterminate style
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // set title and message
        mDialog.setTitle("Please wait");
        mDialog.setMessage("Recognizing...");

        ttsManager = new TTSManager();
        ttsManager.init(this);

        try {


        } catch (Exception e) {
            e.printStackTrace();
        }

        View decorView = getWindow().getDecorView();
// Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    void popAlexaInfo() {

        String msg = "We are not able to identify you. Please " +
                "speak to the doorbell";

        Toast.makeText(getApplicationContext(),
                msg, Toast.LENGTH_LONG).show();

        ttsManager.addQueue(msg);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        switch (requestCode) {
            case ACTION_TAKE_PHOTO_B: {
                if (resultCode == RESULT_OK) {
                    handleBigCameraPhoto();
                }
                break;
            } // ACTION_TAKE_PHOTO_B

            case ACTION_TAKE_PHOTO_S: {
                if (resultCode == RESULT_OK) {
                    handleSmallCameraPhoto(data);
                }
                break;
            } // ACTION_TAKE_PHOTO_S
        } // switch
    }

    // Some lifecycle callbacks so that the image can survive
    // orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY,
                (mImageBitmap != null));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageBitmap = savedInstanceState.getParcelable
                (BITMAP_STORAGE_KEY);
        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(
                savedInstanceState.getBoolean
                        (IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
                        ImageView.VISIBLE : ImageView.INVISIBLE
        );

        View decorView = getWindow().getDecorView();
// Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        //mTextView.setVisibility(View.INVISIBLE);
    }

    private void setListenerOrDisable(
            View vi,
            View.OnClickListener onClickListener,
            String intentName
    ) {
        if (isIntentAvailable(this, intentName)) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                vi.setOnClickListener(onClickListener);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            }
        } else {
            vi.setClickable(false);
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[]
                                                   grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] != PackageManager
                        .PERMISSION_GRANTED) {
                    // permission denied
                    // disable the camera button
                    mPicSBtn.setClickable(false);
                }
                return;
            }

        }
    }

    /**
     * Releases the resources used by the TextToSpeech engine.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        ttsManager.shutDown();
    }

    @Override
    public void onStart() {
        super.onStart();  // Always call the superclass method first

        View decorView = getWindow().getDecorView();
// Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

}