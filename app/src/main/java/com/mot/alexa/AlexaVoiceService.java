package com.mot.alexa;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.knowles.alexalibrary.AlexaManager;
import com.knowles.alexalibrary.audioplayer.AlexaAudioPlayer;
import com.knowles.alexalibrary.callbacks.AsyncCallback;
import com.knowles.alexalibrary.interfaces.AvsItem;
import com.knowles.alexalibrary.interfaces.AvsResponse;
import com.knowles.alexalibrary.interfaces.audioplayer.AvsPlayContentItem;
import com.knowles.alexalibrary.interfaces.audioplayer.AvsPlayRemoteItem;
import com.knowles.alexalibrary.interfaces.errors.AvsResponseException;
import com.knowles.alexalibrary.interfaces.playbackcontrol.AvsMediaNextCommandItem;
import com.knowles.alexalibrary.interfaces.playbackcontrol.AvsMediaPauseCommandItem;
import com.knowles.alexalibrary.interfaces.playbackcontrol.AvsMediaPlayCommandItem;
import com.knowles.alexalibrary.interfaces.playbackcontrol.AvsMediaPreviousCommandItem;
import com.knowles.alexalibrary.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.knowles.alexalibrary.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.knowles.alexalibrary.interfaces.playbackcontrol.AvsStopItem;
import com.knowles.alexalibrary.interfaces.speaker.AvsAdjustVolumeItem;
import com.knowles.alexalibrary.interfaces.speaker.AvsSetMuteItem;
import com.knowles.alexalibrary.interfaces.speaker.AvsSetVolumeItem;
import com.knowles.alexalibrary.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.knowles.alexalibrary.interfaces.speechsynthesizer.AvsSpeakItem;
import com.knowles.alexalibrary.requestbody.DataRequestBody;
import com.knowles.speechutils.AudioPauser;
import com.knowles.speechutils.RawAudioRecorder;
import com.mot.alexa.utils.Constants;
import com.mot.alexa.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okio.BufferedSink;

/**
 * Created by asingh on 9/1/2016.
 */
public class AlexaVoiceService {
    private static final String TAG = "AlexaVS";

    private final static String PRODUCT_ID = "aLexa";

    private Context mContext;
    private static AlexaVoiceService sInstance;
    private AlexaManager alexaManager;
    private AlexaAudioPlayer audioPlayer;
    private List<AvsItem> avsQueue = new ArrayList<>();
    private long startTime = 0;
    private RawAudioRecorder mRecorder;
    private AudioPauser mAudioPauser;
    private boolean alexaInitialized;

    private Handler mUIHandler;

    private AlexaVoiceService(Context context, Handler handler) {
        mContext = context;
        mUIHandler = handler;
        initAlexaAndroid();
    }

    public static AlexaVoiceService getInstance(Context context, Handler handler) {
        if (sInstance == null) {
            sInstance = new AlexaVoiceService(context, handler);
        }
        return sInstance;
    }

    private void initAlexaAndroid() {
        if(!Utils.isNetworkAvailable(mContext)) {
            setState(Constants.STATE_ERROR);
            alexaInitialized = false;
            return;
        }
        setState(Constants.STATE_INITIALIZING);
        //get our AlexaManager instance for convenience
        alexaManager = AlexaManager.getInstance(mContext, PRODUCT_ID);
        //instantiate our audio player
        audioPlayer = AlexaAudioPlayer.getInstance(mContext);
        //Remove the current item and check for more items once we've finished playing
        audioPlayer.addCallback(alexaAudioPlayerCallback);
        //open our down channel
        alexaManager.sendOpenDownchannelDirective();
        //synchronize our device
        alexaManager.sendSynchronizeStateEvent(requestCallback);
        mAudioPauser = new AudioPauser(mContext, false);
        alexaInitialized = true;
    }

    public void logout() {
        //alexaManager.
    }

    //Listen for user input live
    public void startListening() {
        if (mRecorder == null) {
            mRecorder = new RawAudioRecorder(16000);
        }
        setState(Constants.STATE_LISTENING);
        mRecorder.start();
        alexaManager.sendAudioRequest(requestBody, requestCallback);
    }

    //Send pre recorded file to alexa
    public void sendAudioDataToAVS(byte[] audioBytes) throws Exception {
        if(!alexaInitialized) {
            throw new Exception();
        }
        setState(Constants.STATE_PROCESSING);
        alexaManager.sendAudioRequest(audioBytes, requestCallback);
    }

    public void stopAVS() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        if (audioPlayer != null) {
            //remove callback to avoid memory leaks
            audioPlayer.stop();
            audioPlayer.removeCallback(alexaAudioPlayerCallback);
            audioPlayer.release();
        }
        sInstance = null;
        alexaInitialized = false;
        mUIHandler = null;
    }

    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private AlexaAudioPlayer.Callback alexaAudioPlayerCallback = new AlexaAudioPlayer.Callback() {

        private boolean almostDoneFired = false;

        @Override
        public void playerPrepared(AvsItem pendingItem) {
            almostDoneFired = false;
            Log.d(TAG, "playing the response");
            mAudioPauser.pause();
            setState(Constants.STATE_SPEAKING);
            sendPlaybackStartedEvent(pendingItem);
        }

        @Override
        public void playerProgress(AvsItem item, long offsetInMilliseconds, float percent) {
            Log.i(TAG, "Player percent: " + percent);
            if (percent > .5f && !almostDoneFired) {
                almostDoneFired = true;
                //sendPlaybackNearlyFinishedEvent(item, offsetInMilliseconds);
            }
        }

        @Override
        public void itemComplete(AvsItem completedItem) {
            mAudioPauser.resume();
            setState(Constants.STATE_IDLE);
            Log.d(TAG, "play complete");
            almostDoneFired = false;
            sendPlaybackFinishedEvent(completedItem);
            avsQueue.remove(completedItem);
            checkQueue();
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            setState(Constants.STATE_ERROR);
            sendPlaybackNearlyFinishedEvent(item, 500);
            itemComplete(item);
            return false;
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            e.printStackTrace();
            setState(Constants.STATE_ERROR);
            sendPlaybackNearlyFinishedEvent(item, 500);
            itemComplete(item);
        }
    };

    /**
     * Send an event back to Alexa that we're nearly done with our current playback event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackNearlyFinishedEvent(AvsItem item, long offsetInMilliseconds) {
        if (item != null) {
            alexaManager.sendPlaybackNearlyFinishedEvent(item, offsetInMilliseconds, requestCallback);
            Log.i(TAG, "Sending PlaybackNearlyFinishedEvent");
        }
    }

    /**
     * Send an event back to Alexa that we're starting a speech event
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackStartedEvent(AvsItem item) {
        alexaManager.sendPlaybackStartedEvent(item, null);
        Log.i(TAG, "Sending SpeechStartedEvent");
    }

    /**
     * Send an event back to Alexa that we're done with our current speech event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackFinishedEvent(AvsItem item) {
        if (item != null) {
            alexaManager.sendPlaybackFinishedEvent(item, requestCallback);
            Log.i(TAG, "Sending SpeechFinishedEvent");
        }
    }

    //async callback for commands sent to Alexa Voice
    private AsyncCallback<AvsResponse, Exception> requestCallback = new AsyncCallback<AvsResponse, Exception>() {
        @Override
        public void start() {
            Log.i(TAG, "Event Start");
        }

        @Override
        public void success(AvsResponse result) {
            Log.i(TAG, "Event Success");
            handleResponse(result);
        }

        @Override
        public void failure(Exception error) {
            error.printStackTrace();
            Log.i(TAG, "Event Error");
            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            }
            if (audioPlayer != null) {
                //remove callback to avoid memory leaks
                audioPlayer.stop();
                audioPlayer.removeCallback(alexaAudioPlayerCallback);
                audioPlayer.release();
            }
            initAlexaAndroid();
        }

        @Override
        public void complete() {
            Log.i(TAG, "Event Complete");
            setState(Constants.STATE_IDLE);
            long totalTime = System.currentTimeMillis() - startTime;
            Log.i(TAG, "Total request time: " + totalTime + " miliseconds");
        }
    };

    /**
     * Handle the response sent back from Alexa's parsing of the Intent, these can be any of the AvsItem types (play, speak, stop, clear, listen)
     *
     * @param response a List<AvsItem> returned from the mAlexaManager.sendTextRequest() call in sendVoiceToAlexa()
     */
    private void handleResponse(AvsResponse response) {
        boolean checkAfter = (avsQueue.size() == 0);
        if (response != null) {
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
            //from doing that
            for (int i = response.size() - 1; i >= 0; i--) {
                if (response.get(i) instanceof AvsReplaceAllItem ||
                        response.get(i) instanceof AvsReplaceEnqueuedItem) {
                    //clear our queue
                    avsQueue.clear();
                    //remove item
                    response.remove(i);
                }
            }
            Log.i(TAG, "Adding " + response.size() + " items to our queue");
            avsQueue.addAll(response);
        }
        if (checkAfter) {
            checkQueue();
        }
    }

    /**
     * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
     * next item in our list.
     * <p>
     * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
     * the new items are added to the list, it should have no function here.
     */
    private void checkQueue() {

        //if we're out of things, hang up the phone and move on
        if (avsQueue.size() == 0) {
            //setState(Constants.STATE_IDLE);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    long totalTime = System.currentTimeMillis() - startTime;
                    Log.i(TAG, "Total interaction time: " + totalTime + " miliseconds");
                }
            });
            return;
        }

        final AvsItem current = avsQueue.get(0);

        Log.i(TAG, "Item type " + current.getClass().getName());

        if (current instanceof AvsPlayRemoteItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayRemoteItem) current);
            }
        } else if (current instanceof AvsPlayContentItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayContentItem) current);
            }
        } else if (current instanceof AvsSpeakItem) {
            //play a sound file
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsSpeakItem) current);
            }
            //setState(STATE_SPEAKING);
        } else if (current instanceof AvsStopItem) {
            //stop our play
            audioPlayer.stop();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceAllItem) {
            //clear all items
            //mAvsItemQueue.clear();
            audioPlayer.stop();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceEnqueuedItem) {
            //clear all items
            //mAvsItemQueue.clear();
            avsQueue.remove(current);
        } else if (current instanceof AvsExpectSpeechItem) {

            //listen for user input
            audioPlayer.stop();
            avsQueue.clear();
            //startListening();
        } else if (current instanceof AvsSetVolumeItem) {
            //set our volume
            //setVolume(((AvsSetVolumeItem) current).getVolume());
            avsQueue.remove(current);
        } else if (current instanceof AvsAdjustVolumeItem) {
            //adjust the volume
            //adjustVolume(((AvsAdjustVolumeItem) current).getAdjustment());
            avsQueue.remove(current);
        } else if (current instanceof AvsSetMuteItem) {
            //mute/unmute the device
            //setMute(((AvsSetMuteItem) current).isMute());
            avsQueue.remove(current);
        } else if (current instanceof AvsMediaPlayCommandItem) {
            //fake a hardware "play" press
            //sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PLAY);
            Log.i(TAG, "Media play command issued");
            avsQueue.remove(current);
        } else if (current instanceof AvsMediaPauseCommandItem) {
            //fake a hardware "pause" press
            //sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PAUSE);
            Log.i(TAG, "Media pause command issued");
            avsQueue.remove(current);
        } else if (current instanceof AvsMediaNextCommandItem) {
            //fake a hardware "next" press
            //sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_NEXT);
            Log.i(TAG, "Media next command issued");
            avsQueue.remove(current);
        } else if (current instanceof AvsMediaPreviousCommandItem) {
            //fake a hardware "previous" press
            //sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            Log.i(TAG, "Media previous command issued");
            avsQueue.remove(current);
        } else if (current instanceof AvsResponseException) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(mContext)
                            .setTitle("Error")
                            .setMessage(((AvsResponseException) current).getDirective()
                                    .getPayload().getCode() + ": " +
                                    ((AvsResponseException) current).getDirective()
                                            .getPayload().getDescription())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            });

            avsQueue.remove(current);
            checkQueue();
        }
    }

    private void setState(final int state) {
        if(mUIHandler == null) return;
        switch (state) {
            case (Constants.STATE_LISTENING):
                mUIHandler.sendEmptyMessage(Constants.STATE_LISTENING);
                break;
            case (Constants.STATE_PROCESSING):
                mUIHandler.sendEmptyMessage(Constants.STATE_PROCESSING);
                break;
            case (Constants.STATE_SPEAKING):
                mUIHandler.sendEmptyMessage(Constants.STATE_SPEAKING);
                break;
            case (Constants.STATE_IDLE):
                mUIHandler.sendEmptyMessage(Constants.STATE_IDLE);
                break;
            case (Constants.STATE_ERROR):
                mUIHandler.sendEmptyMessage(Constants.STATE_ERROR);
                break;
            case (Constants.STATE_INITIALIZING):
                mUIHandler.sendEmptyMessage(Constants.STATE_INITIALIZING);
                //statePrompting();
                break;
            default:
                //stateNone();
                break;
        }
    }

    private DataRequestBody requestBody = new DataRequestBody() {
        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            while (mRecorder != null && !mRecorder.isPausing()) {
                if (mRecorder != null) {
                    if (sink != null && mRecorder != null) {
                        sink.write(mRecorder.consumeRecording());
                    }
                }

                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stopListening();
        }

    };

    private void stopListening() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        setState(Constants.STATE_PROCESSING);
        //mAlexaAnimation.stop();
    }
}
