package com.highwayrush.game;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
public class MainActivity extends Activity {
    private WebView game;
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(5894);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); immersive();
        game = new WebView(this);
        game.setBackgroundColor(0xff111329);
        game.getSettings().setJavaScriptEnabled(true);
        game.getSettings().setDomStorageEnabled(true);
        game.getSettings().setAllowFileAccess(false);
        game.getSettings().setAllowContentAccess(false);
        game.setWebViewClient(new WebViewClient());
        setContentView(game);
        game.loadUrl("file:///android_asset/index.html");
    }
    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if(focus) immersive(); }
    @Override protected void onPause() {
        game.evaluateJavascript("if(state==='running')togglePause()",null);
        game.onPause(); super.onPause();
    }
    @Override protected void onResume() { super.onResume(); if(game!=null)game.onResume(); }
    @Override protected void onDestroy() { if(game!=null)game.destroy(); super.onDestroy(); }
}
