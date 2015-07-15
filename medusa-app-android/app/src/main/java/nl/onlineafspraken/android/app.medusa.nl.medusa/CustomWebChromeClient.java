package app.medusa.nl.medusa;

import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;


public class CustomWebChromeClient extends WebChromeClient {
    ProgressBar myProgressBar;
    ImageView mySplash;
    RelativeLayout mySplashLayout;

    public void onProgressChanged(WebView view, int progress) {

        myProgressBar.setProgress(progress);

        if (progress == 100) {
            mySplashLayout.setVisibility(View.GONE);
            myProgressBar.setVisibility(View.GONE);
            mySplash.setVisibility(View.GONE);
        }
    }
}