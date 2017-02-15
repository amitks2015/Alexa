package com.mot.alexa.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.mot.alexa.R;

/**
 * Created by asingh on 9/7/2016.
 */
public class Utils {
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void displayOkDialog(final Context ctx, String msg) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
        dialog.setTitle(R.string.app_name);
        dialog.setMessage(msg);
        dialog.setNeutralButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }

    public static String getAppVersion(Context context) {
        String version = "App Version: ";
        PackageManager manager = context.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(
                    context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if(info != null) {
            version = version + info.versionName;
        }
        return version;
    }

    public static boolean getBooleanPref(Context context, String name, boolean def) {
        SharedPreferences prefs =
                context.getSharedPreferences(Constants.APP_PREF, Context.MODE_PRIVATE);
        return prefs.getBoolean(name, def);
    }

    public static void setBooleanPref(Context context, String name, boolean value) {
        SharedPreferences prefs =
                context.getSharedPreferences(Constants.APP_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(name, value);
        ed.apply();
    }
}
