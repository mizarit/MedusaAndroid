package app.medusa.nl.medusa;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.net.Uri;
import java.io.ByteArrayOutputStream;

public class Main extends app.medusa.nl.medusa.GcmActivity {
	WebView myWebView;
	ProgressBar myProgressBar;
	ImageView mySplash;
	RelativeLayout mySplashLayout;
    Context context;
    boolean hasSensor;

    static final String TAG = "Medusa";

    public static final String PROPERTY_PAYLOAD = "payload";
    public static final String PROPERTY_PAYLOAD_ARGS = "payload_args";
    public static final String PROPERTY_BACK_CALLBACK = "back_callback";

    app.medusa.nl.medusa.AndroidJS myAndroidJS;

    private static final int REQUEST_CODE = 6666; // onActivityResult request code

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == REQUEST_CODE) {
            // If the file selection was successful
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    // Get the URI of the selected file
                    final Uri uri = data.getData();
                    Log.i(TAG, "Uri = " + uri.toString());

                    long imageId = Long.parseLong(uri.getLastPathSegment());

                    Log.i(TAG, Long.toString(imageId));
                        Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                                getContentResolver(),
                                imageId,
                                MediaStore.Images.Thumbnails.MINI_KIND,
                                (BitmapFactory.Options) null );


                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        bitmap.recycle();
                        bitmap = null;

                        byte[] b = baos.toByteArray();
                        String encodedImage=Base64.encodeToString(b, Base64.NO_WRAP);

                        String javascript = "javascript:imageSelected('"+encodedImage+"');";
                        myWebView.loadUrl(javascript);
                }
            }
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main );

        final SharedPreferences prefs = getSharedPreferences(app.medusa.nl.medusa.Main.class.getSimpleName(), Context.MODE_PRIVATE);

        initGCM();
        hasSensor = (initSensors() != null);

        CookieManager.getInstance().setAcceptCookie(true);

        myProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mySplash = (ImageView) findViewById(R.id.splash);
        myWebView = (WebView) findViewById(R.id.webview);
   	 	mySplashLayout = (RelativeLayout) findViewById(R.id.InnerRelativeOverallLayout);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDomStorageEnabled(true);

        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        Log.i(TAG, appCachePath);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCachePath(appCachePath);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        myWebView.setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView view, int progress) {
                myProgressBar.setProgress(progress);

                if (progress == 100) {
                    mySplashLayout.setVisibility(View.GONE);
                    myProgressBar.setVisibility(View.GONE);
                    mySplash.setVisibility(View.GONE);
                }
            }
        });

        myWebView.setWebViewClient(new app.medusa.nl.medusa.CustomWebViewClient() { });

        // disable text selection
        myWebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        myWebView.setLongClickable(false);

        // disable the vibrate feedback on long clicks
        myWebView.setHapticFeedbackEnabled(false);

        myAndroidJS = new app.medusa.nl.medusa.AndroidJS(getApplicationContext(), this);
        myWebView.addJavascriptInterface(myAndroidJS, "Android");

        String url = this.getString(R.string.app_url_alt);
        if (this.isNetworkAvailable()) {
            url = url.concat("?device=android");
            if (checkPlayServices()) {
                url = url.concat("&android_id=").concat(regid);
            }
            if (hasSensor) {
                url = url.concat("&sensor=1");
            }
        }
        else {
            url = url.concat("/main/index");
        }


        Log.i(TAG, url);
        myWebView.loadUrl(url);

        (new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                while (!Thread.interrupted())
                    try
                    {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() // start actions in UI thread
                        {

                            @Override
                            public void run()
                            {

                                String payload = prefs.getString(PROPERTY_PAYLOAD, "");
                                String payload_args = prefs.getString(PROPERTY_PAYLOAD_ARGS, "");
                                if (!payload.isEmpty()) {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString(PROPERTY_PAYLOAD, "");
                                    editor.putString(PROPERTY_PAYLOAD_ARGS, "");
                                    editor.commit();

                                    Log.i(TAG, payload.toString());
                                    Log.i(TAG, payload_args.toString());

                                    String javascript = "javascript:"+payload.toString()+"("+payload_args.toString()+");";
                                    Log.i(TAG, javascript);
                                    myWebView.loadUrl(javascript);


                                }
                            }
                        });
                    }
                    catch (InterruptedException e)
                    {
                        // ooops
                    }
            }
        })).start(); // the while thread will start in BG thread

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_BACK:
                    if(myWebView.canGoBack()){
                        myWebView.goBack();
                    }else{
                        final SharedPreferences prefs = getApplicationContext().getSharedPreferences(app.medusa.nl.medusa.Main.class.getSimpleName(), Context.MODE_PRIVATE);
                        String back_callback = prefs.getString(PROPERTY_BACK_CALLBACK, "");
                        if(back_callback.length() > 0) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(PROPERTY_BACK_CALLBACK, "");
                            editor.commit();
                            String javascript = "javascript:"+back_callback;
                            Log.i(TAG, javascript);
                            myWebView.loadUrl(javascript);
                        }
                        else {
                            finish();
                        }
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}