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
import com.kairos.Kairos;
import com.kairos.KairosListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class PhotoIntentActivity extends AppCompatActivity {

    private static final int ACTION_TAKE_PHOTO_B = 1;
    private static final int ACTION_TAKE_PHOTO_S = 2;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;

    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY =
            "imageviewvisibility";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    // Kairos related params
    private static final String app_id = "faaa8087";
    private static final String api_key = "0eee0b3a727cf973c499fbe1170ae230";
    private static final String galleryId = "employees";
    private static final String selector = "FULL";
    private static final String threshold = "0.75";
    private static final String minHeadScale = "0.25";
    private static final String maxNumResults = "2";
    private static final String TAG_ERRORS = "Errors";
    private static final String TAG_IMAGES = "images";
    private static final String TAG_TRANSACTION = "transaction";
    private static final String TAG_CANDIDATES = "candidates";
    private static final String TAG_SUBJECT = "subject";
    private static final String TAG_SUBJECTID = "subject_id";
    private static final String TAG_STATUS = "status";
    private static final String TAG_CONFIDENCE = "confidence";
    private static final String TAG = "PhotoIntentActivity";
    String[] employStrings = {"lisa", "charlie", "rui", "jian", "qingfeng"};
    HashSet<String> employees = new HashSet<>(Arrays.asList(employStrings));
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
    private KairosListener kairosListener = null;
    private Kairos myKairos = null;
    private ProgressDialog mDialog;
    private TTSManager ttsManager = null;

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

        takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
        takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
        takePictureIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);

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
//        FaceClassification faceClassification = FaceClassificationService
// .classifyImage("input.jpg", mImageBitmap);
        // classify the image just taken
        try {
            // setup a progressdialog and set the init state to false

            mDialog.show();

            myKairos.recognize(mImageBitmap,
                    galleryId,
                    selector,
                    threshold,
                    minHeadScale,
                    maxNumResults,
                    kairosListener);

            saveImages(mImageBitmap);
            //
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mImageView.setImageBitmap(mImageBitmap);
        //mImageView.setVisibility(View.VISIBLE);
//        mTextView.setText(faceClassification.toString());
//        mTextView.setVisibility(View.VISIBLE);
    }

    private void saveImages(Bitmap bm) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/face_images");
        myDir.mkdirs();

        String fjpg = "charlie.jpg";
        String fpng = "charlie.png";
        String fbmp = "charlie.bmp";

        //writeFile(myDir, fbmp, bm);
        writeFile(myDir, fjpg, bm);
        writeFile(myDir, fpng, bm);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
    }

    private void writeFile(File dir, String fname, Bitmap bm) {
        File file = new File(dir, fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bm.compress(fname.endsWith(".png")?
                    Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG,
                    100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        //startWebRTC();
//        initDigitalLife();
        try {
            // listener
            kairosListener = new KairosListener() {

                @Override
                public void onSuccess(String response) {
                    if (mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                    Log.d("Kairos resp success:", response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);

                        if (jsonObject.has(TAG_ERRORS)) {
                            // image error
                            popAlexaInfo();
                        } else {
                            JSONArray images = (JSONArray) jsonObject
                                    .getJSONArray(TAG_IMAGES);
                            if (((JSONObject) ((JSONObject) images.get(0))
                                    .get(TAG_TRANSACTION)).get(TAG_STATUS)
                                    .equals("failure")) {
                                // no match
                                popAlexaInfo();
                            } else {
                                JSONObject transaction = (JSONObject) (
                                        (JSONObject) images.get(0)).get
                                        (TAG_TRANSACTION);
                                String name = transaction.has(TAG_SUBJECT) ?
                                        (String) transaction.get(TAG_SUBJECT)
                                        : (String) transaction.get
                                        (TAG_SUBJECTID);
                                int confidence = (int) ((double) transaction.get
                                        (TAG_CONFIDENCE) * 100);
//                                mTextView.setText("Photo matches with " +
// name + " with confidence: " + confidence + "%");
                                //mTextView.setText("Welcome " + name + "!
                                // The door will be opened for you.");

                                welcome(name);

                                // post the photo to Kairos
//                                myKairos.enroll(mImageBitmap,
//                                        name,
//                                        galleryId,
//                                        selector,
//                                        "false",
//                                        minHeadScale,
//                                        kairosListener);
                            }
                        }
                    } catch (JSONException ex) {//
                        ex.printStackTrace();
                    }
                    //mTextView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFail(String response) {
                    if (mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                    Log.d("Kairos response - fail:", response);
                    popAlexaInfo();
                }
            };


        /* * * instantiate a new kairos instance * * */
            myKairos = new Kairos();

        /* * * set authentication * * */
            myKairos.setAuthentication(this, app_id, api_key);

//            // enroll coworkers photos once
//            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R
// .drawable.charliegz1);
//            String selector = "FULL";
//            String multipleFaces = "false";
//            String minHeadScale = "0.25";
//            myKairos.enroll(bitmap,
//                    "charlie",
//                    galleryId,
//                    selector,
//                    multipleFaces,
//                    minHeadScale,
//                    kairosListener);

//            bitmap = BitmapFactory.decodeResource(getResources(), R
// .drawable.rui);
//            myKairos.enroll(bitmap,
//                    "rui",
//                    galleryId,
//                    selector,
//                    multipleFaces,
//                    minHeadScale,
//                    kairosListener);
//
//            bitmap = BitmapFactory.decodeResource(getResources(), R
// .drawable.qingfeng2);
//            myKairos.enroll(bitmap,
//                    "qingfeng",
//                    galleryId,
//                    selector,
//                    multipleFaces,
//                    minHeadScale,
//                    kairosListener);
//
//            bitmap = BitmapFactory.decodeResource(getResources(), R
// .drawable.lisa);
//            myKairos.enroll(bitmap,
//                    "lisa",
//                    galleryId,
//                    selector,
//                    multipleFaces,
//                    minHeadScale,
//                    kairosListener);
//            mImageView.setImageBitmap(bitmap);
//            mImageView.setVisibility(View.VISIBLE);
//            myKairos.listGalleries(kairosListener);

// end of enroll coworkers photos once


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

        //new AlertDialog.Builder(this).setTitle("欢迎").setMessage
        //        (msg).create().show();

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