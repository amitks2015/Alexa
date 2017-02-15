package com.mot.alexa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LoginWebViewActivity extends AppCompatActivity {
    private static final String TAG = "LoginWebViewActivity";

    WebView mWebView;

    private static final int RESULT_LOGIN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mWebView = (WebView) findViewById(R.id.webview);

        mWebView.setWebViewClient(mWebViewClient);

        // Get the intent that started this activity
        Intent intent = getIntent();
        Uri data = intent.getData();

        if(data != null){
            mWebView.loadUrl(data.toString());
        }else{
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RESULT_LOGIN){
            LoginWebViewActivity.this.finish();
        }
    }

    WebViewClient mWebViewClient = new WebViewClient(){
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if(BuildConfig.DEBUG){
                Log.i(TAG, url);
            }
            if(url.startsWith("http") || url.startsWith("https")){
                return super.shouldOverrideUrlLoading(view, url);
            }

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivityForResult(i, RESULT_LOGIN);

            return true;
        }


    };

}
