
package com.advancedwebview;

import android.content.Intent;
import android.net.Uri;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.webview.ReactWebViewManager;

public class AdvancedWebviewManager extends ReactWebViewManager {

    private ValueCallback<Uri> mUM;
    private final static int FCR=1;

    public WebView webview = null;

    private AdvancedWebviewPackage aPackage;
    public String getName() {

        return "AdvancedWebview";
    }

    @ReactProp(name = "uploadEnabledAndroid")
    public void uploadEnabledAndroid(WebView view, boolean enabled) {
        if(enabled) {
            webview = view;
            final AdvancedWebviewModule module = this.aPackage.getModule();
            view.setWebChromeClient(new WebChromeClient(){

                //For Android 3.0+
                public void openFileChooser(ValueCallback<Uri> uploadMsg){
                    module.setUploadMessage(uploadMsg);
                    mUM = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    module.getActivity().startActivityForResult(Intent.createChooser(i, "File Chooser"), FCR);
                }
                // For Android 3.0+, above method not supported in some android 3+ versions, in such case we use this
                public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType){
                    module.setUploadMessage(uploadMsg);
                    mUM = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    module.getActivity().startActivityForResult(
                            Intent.createChooser(i, "File Browser"),
                            FCR);
                }
                //For Android 4.1+
                public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
                    module.setUploadMessage(uploadMsg);
                    mUM = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    module.getActivity().startActivityForResult(Intent.createChooser(i, "File Chooser"), FCR);
                }
                //For Android 5.0+
                public boolean onShowFileChooser(
                        WebView webView, ValueCallback<Uri[]> filePathCallback,
                        WebChromeClient.FileChooserParams fileChooserParams) {
                    module.setmUploadCallbackAboveL(filePathCallback);
                    /*if(mUMA != null){
                        mUMA.onReceiveValue(null);
                    }*/
                    if (module.grantPermissions()) {
                        module.uploadImage(filePathCallback);
                    }
                    return true;
                }
            });

        }
    }

    @ReactProp(name = "downloadEnabledAndroid")
    public void downloadEnabledAndroid(WebView view, boolean enabled) {
        if(enabled) {
            final AdvancedWebviewModule module = this.aPackage.getModule();
            view.setDownloadListener(new DownloadListener() {

                @Override
                public void onDownloadStart(final String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                    module.downUrl = url;
                    if(module.grantStoragePermission()) {
                        module.downloadImage(url);
                    }else{

                    }
                }
            });
        }
    }

    public static void setTimeout(final Runnable runnable, final int delay){
        new Thread(new Runnable () {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                    runnable.run();
                }
                catch (Exception e){
                    System.err.println(e);
                }
            }
        }).start();
    }

    public String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }


    public void setPackage(AdvancedWebviewPackage aPackage){
        this.aPackage = aPackage;
    }

    public AdvancedWebviewPackage getPackage(){
        return this.aPackage;
    }
}