package com.mot.alexa;

import android.Manifest;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.mot.alexa.utils.Constants;
import com.mot.alexa.utils.Utils;

public class MainActivity extends AppCompatActivity implements
        TutorialDialogFragment.InitAlexaListener {
    private static final int REQUEST_PERMISSION = 0;
    private static final String TAG = "aLexa-Main";
    private static String[] PERMISSIONS_BALI = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};
    View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootView = findViewById(R.id.root);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if((ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                        PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_BALI, REQUEST_PERMISSION);
        } else {
           if(Utils.getBooleanPref(this, Constants.KEY_SHOW_TUTORIAL, true)) {
               showTutorial();
           } else {
               initializeAlexa();
           }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION) {
            if(grantResults != null && grantResults.length == 2) {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    if(Utils.getBooleanPref(this, Constants.KEY_SHOW_TUTORIAL, true)) {
                        showTutorial();
                    } else {
                        initializeAlexa();
                    }
                } else {
                    showSnackbar(R.string.permission_denied);
                }
            } else {
                showSnackbar(R.string.permission_denied);
            }
        }
    }

    private void showSnackbar(int msg) {
        Snackbar sb = Snackbar
                .make(rootView, msg, Snackbar.LENGTH_LONG);
        sb.getView().setBackgroundColor(getResources()
                .getColor(R.color.colorPrimaryDark));
        sb.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Utils.displayOkDialog(this, Utils.getAppVersion(this));
        }

        return super.onOptionsItemSelected(item);
    }

    public void initializeAlexa() {
        Log.d(TAG, "initialize alexa");
        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.fragment);
        if(frag != null && frag instanceof MainActivityFragment) {
            ((MainActivityFragment) frag).initAlexa();
        }
    }

    private void showTutorial() {
        Log.d(TAG, "Show tutorial dialog");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        android.app.Fragment fragment = getFragmentManager().findFragmentByTag("recording");
        if (fragment != null) {
            ft.remove(fragment);
        }
        ft.addToBackStack(null);
        DialogFragment df = TutorialDialogFragment.newInstance();
        df.show(ft, "tutorial");
    }

    @Override
    public void onDialogDismissed() {
        Log.d(TAG, "Tutorial Dialog dismissed");
        initializeAlexa();
    }
}
