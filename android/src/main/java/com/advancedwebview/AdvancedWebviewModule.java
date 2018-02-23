package com.advancedwebview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.common.annotations.VisibleForTesting;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import facilit.net.helpdesk.R;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;
import static android.content.Context.NOTIFICATION_SERVICE;

public class CustomWebviewModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadCallbackAboveL;
    private String mCM;
    public static final int MY_PERMISSIONS_REQUEST_STORAGE = 1;
    public static final int MY_PERMISSIONS_REQUEST_ALL = 2;
    private final int NOTIFICATION_ID = 1;
    public String downUrl = null;
    private final static int FCR=1;
    private ValueCallback<Uri[]> mUMA;
    private String[] permissions = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @VisibleForTesting
    public static final String REACT_CLASS = "CustomWebview";
    public ReactContext REACT_CONTEXT;

    public CustomWebviewModule(ReactApplicationContext context){

        super(context);
        REACT_CONTEXT = context;
        context.addActivityEventListener(this);
    }

    private CustomWebviewPackage aPackage;

    public void setPackage(CustomWebviewPackage aPackage) {
        this.aPackage = aPackage;
    }

    public CustomWebviewPackage getPackage() {
        return this.aPackage;
    }

    @Override
    public String getName(){
        return REACT_CLASS;
    }

    @SuppressWarnings("unused")
    public Activity getActivity() {
        return getCurrentActivity();
    }

    public ReactContext getReactContext(){
        return REACT_CONTEXT;
    }

    @ReactMethod
    public void getUrl(Callback errorCallback,
                       final Callback successCallback) {
        try {
            final WebView view = getPackage().getManager().webview;

            if(getPackage().getManager().webview != null) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        successCallback.invoke(view.getUrl());
                    }
                });
            }else{
                successCallback.invoke("");
            }
        }catch(Exception e){
            errorCallback.invoke(e.getMessage());
        }
    }

    public void setUploadMessage(ValueCallback<Uri> uploadMessage) {
        mUploadMessage = uploadMessage;
    }


    public void setmUploadCallbackAboveL(ValueCallback<Uri[]> mUploadCallbackAboveL) {
        this.mUploadCallbackAboveL = mUploadCallbackAboveL;
    }

    public void setmCM(String mCM) {
        this.mCM = mCM;
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(activity,requestCode, resultCode, intent);
        if (requestCode == 1) {
            if (null == mUploadMessage && null == mUploadCallbackAboveL) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (mUploadCallbackAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }


    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent data) {
        if (requestCode != 1
                || mUploadCallbackAboveL == null) {
            return;
        }
        Uri[] results = null;
        if (resultCode == RESULT_OK) {
            if (data == null) {
                if(mCM != null){
                    results = new Uri[]{Uri.parse(mCM)};
                }
            } else {
                String dataString = data.getDataString();
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        mUploadCallbackAboveL.onReceiveValue(results);
        mUploadCallbackAboveL = null;
        return;
    }

    private PermissionListener listener = new PermissionListener() {
        @Override
        public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            switch (requestCode) {
                case MY_PERMISSIONS_REQUEST_ALL: {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if(mUploadCallbackAboveL != null){
                            uploadImage(mUploadCallbackAboveL);
                        }
                    } else {
                        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.no_up_img_permission), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                case MY_PERMISSIONS_REQUEST_STORAGE: {
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (downUrl != null) {
                            downloadImage(downUrl);
                        }
                    } else {
                        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.no_down_img_permission), Toast.LENGTH_LONG).show();
                    }
                }
            }
            return false;
        }
    };

    public boolean grantPermissions() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return true;
        }
        boolean result = true;
        final CustomWebviewModule module = this.aPackage.getModule();
        for (String permission:permissions){
            if (ContextCompat.checkSelfPermission(module.getActivity(),
                    permission)
                    != PackageManager.PERMISSION_GRANTED) {
                result = false;
            }

        }

        if(!result){
            PermissionAwareActivity activity = getPermissionAwareActivity();

            activity.requestPermissions(permissions, MY_PERMISSIONS_REQUEST_ALL,listener);
            /*ActivityCompat.requestPermissions(module.getActivity(),
                    permissions,
                    module.MY_PERMISSIONS_REQUEST_ALL);*/
        }
        return result;
    }

    public boolean grantStoragePermission() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return true;
        }
        final CustomWebviewModule module = this.aPackage.getModule();

        if(ContextCompat.checkSelfPermission(module.getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            PermissionAwareActivity activity = getPermissionAwareActivity();

            activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE,listener);
            /*ActivityCompat.requestPermissions(module.getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_STORAGE);*/
            return false;
        }else {
            return true;
        }
    }

    private PermissionAwareActivity getPermissionAwareActivity() {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            throw new IllegalStateException("Tried to use permissions API while not attached to an " +
                    "Activity.");
        } else if (!(activity instanceof PermissionAwareActivity)) {
            throw new IllegalStateException("Tried to use permissions API but the host Activity doesn't" +
                    " implement PermissionAwareActivity.");
        }
        return (PermissionAwareActivity) activity;
    }


    public void downloadImage(String url){
        final CustomWebviewModule module = this.aPackage.getModule();
        displayNotification(NOTIFICATION_ID,module.getActivity().getResources().getString(R.string.down_title),module.getActivity().getResources().getString(R.string.down_desc),android.R.drawable.stat_sys_download,null);
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            throw new IOException("Failed to download file: " + response);
                        }
                        Headers responseHeaders = response.headers();
                        String content = responseHeaders.get("Content-Disposition");
                        String contentSplit[] = content.split("filename=");
                        String filename = contentSplit[1].replace("filename=", "").replace("\"", "").trim();

                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(response.body().bytes());
                        fos.close();

                        //Scan media to make the file visible
                        MediaScannerConnection.scanFile(module.getActivity(),
                                new String[]{file.toString()}, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        displayNotification(NOTIFICATION_ID, module.getActivity().getResources().getString(R.string.down_title), module.getActivity().getResources().getString(R.string.down_desc_done), android.R.drawable.stat_sys_download_done, uri);
                                    }
                                });
                    } catch (Exception ex) {
                        //Log.d("Exception", "" + ex);
                        displayNotification(NOTIFICATION_ID, module.getActivity().getResources().getString(R.string.down_title), module.getActivity().getResources().getString(R.string.down_desc_fail), android.R.drawable.stat_sys_download_done, null);
                    }
                }
            });
        }catch(Exception ex){
            displayNotification(NOTIFICATION_ID, module.getActivity().getResources().getString(R.string.down_title), module.getActivity().getResources().getString(R.string.down_desc_fail), android.R.drawable.stat_sys_download_done, null);
        }
    }

    public void displayNotification(int mNotificationId,String title,String description,int icon,Uri imageUri){
        final CustomWebviewModule module = this.aPackage.getModule();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(module.getActivity(),"default")
                        .setSmallIcon(icon)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setContentText(description);

        if(imageUri != null){
            Intent intent = new Intent(Intent.ACTION_VIEW,imageUri);
            PendingIntent contentIntent =
                    PendingIntent.getActivity(module.getActivity(),
                            0,
                            intent,
                            PendingIntent.FLAG_ONE_SHOT
                    );
            mBuilder.setContentIntent(contentIntent);

        }
// Sets an ID for the notification
// Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) module.getActivity().getSystemService(NOTIFICATION_SERVICE);

// Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    public void uploadImage(ValueCallback<Uri[]> filePathCallback){
        mUMA = filePathCallback;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCM);
            } catch (IOException ex) {
                Log.e(TAG, "Image file creation failed", ex);
            }
            if (photoFile != null) {
                mCM = "file:" + photoFile.getAbsolutePath();
                setmCM(mCM);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }
        }
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        Intent[] intentArray;
        if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, getActivity().getResources().getString(R.string.image_chooser));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        getActivity().startActivityForResult(chooserIntent, FCR);
    }

    // Create an image file
    private File createImageFile() throws IOException{
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }
}