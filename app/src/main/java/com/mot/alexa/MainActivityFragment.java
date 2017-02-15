package com.mot.alexa;

import android.app.ProgressDialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Message;
import android.speech.SpeechRecognizer;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mot.alexa.utils.Constants;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private static final String TAG = "aLexa";
    private ImageView mAlexaMic;
    private RelativeLayout mParentView;
    private CoordinatorLayout mSnackBarParent;
    private Snackbar mSnackbar;

    //Alexa Voice Service
    private AlexaVoiceService mAVS;
    private AnimationDrawable mAlexaAnimation;
    private VoiceServiceStateHandler mVoiceStateHandler;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mParentView = (RelativeLayout) rootView.findViewById(R.id.root_view);
        mSnackBarParent = (CoordinatorLayout) rootView.findViewById(R.id.coordinatorLayout);
        mAlexaMic = (ImageView) rootView.findViewById(R.id.alexa_mic);
        mAlexaMic.setBackgroundResource(R.drawable.alexa_animation);
        mAlexaMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Mic clicked Start Listening");
                //Start animation when VoiceQ gets detected
                //mSpeech.startListening(recognizerIntent);
                mVoiceStateHandler.sendEmptyMessage(Constants.STATE_LISTENING);
                if(mAVS != null)
                mAVS.startListening();
                //playMusic();
            }
        });
        return rootView;
    }

    public void initAlexa() {
        mVoiceStateHandler = new VoiceServiceStateHandler();
        mAVS = AlexaVoiceService.getInstance(getActivity(), mVoiceStateHandler);
    }
    /**
     * Handle the response from voice services and update UI
     */
    public class VoiceServiceStateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case Constants.STATE_IDLE:
                    Log.d(TAG,"VS STATE IDLE");
                    stopMicAnimation();
                    dismissSnackBar();
                    break;
                case Constants.STATE_LISTENING:
                    Log.d(TAG,"VS STATE LISTENING");
                    startMicAnimation();
                    break;
                case Constants.STATE_PROCESSING:
                    Log.d(TAG,"VS STATE PROCESSING");
                    stopMicAnimation();
                    displaySnackBar(getString(R.string.processing));
                    break;
                case Constants.STATE_INITIALIZING:
                    Log.d(TAG,"VS STATE INITIALIZING");
                    displaySnackBar(getString(R.string.initializing));
                    break;
                case Constants.STATE_SPEAKING:
                    Log.d(TAG,"VS STATE SPEAKING");
                    dismissSnackBar();
                    break;
                case Constants.STATE_ERROR:
                    break;
            }
        }
    }

    private void displaySnackBar(String msg) {
        mSnackbar = Snackbar.make(mSnackBarParent, msg, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.getView().setBackgroundColor(getResources()
                .getColor(R.color.colorPrimaryDark));
        mSnackbar.show();
    }

    private void startMicAnimation() {
        mAlexaAnimation = (AnimationDrawable) mAlexaMic.getBackground();
        if(mAlexaAnimation != null) {
            mAlexaAnimation.start();
        }
    }

    private void stopMicAnimation() {
        if(mAlexaAnimation != null && mAlexaAnimation.isRunning()) {
            mAlexaAnimation.stop();
            mAlexaAnimation = null;
            mAlexaMic.setBackground(null);
            mAlexaMic.setBackgroundResource(R.drawable.alexa_animation);
        }
    }

    private void dismissSnackBar() {
        if(mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        dismissSnackBar();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        if(mAVS!= null) {
            mAVS.stopAVS();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }
}
