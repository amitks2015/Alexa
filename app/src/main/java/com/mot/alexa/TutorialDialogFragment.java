package com.mot.alexa;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.mot.alexa.utils.Constants;
import com.mot.alexa.utils.Utils;

/**
 * Created by asingh on 05-01-2016.
 */
public class TutorialDialogFragment extends DialogFragment implements View.OnClickListener {
    private CheckBox cb;
    boolean isChecked = false;

    static TutorialDialogFragment newInstance() {
        return new TutorialDialogFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container
            , Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tutorial_layout, container, false);
        getDialog().setTitle(getString(R.string.app_name));
        cb = (CheckBox) rootView.findViewById(R.id.do_not_show);
        cb.setOnClickListener(this);
        Button okButton = (Button) rootView.findViewById(R.id.ok);
        okButton.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.ok:
                if(isChecked) {
                    Utils.setBooleanPref(getActivity(), Constants.KEY_SHOW_TUTORIAL, !isChecked);
                }
                if(mCallback != null) {
                    mCallback.onDialogDismissed();
                }
                dismiss();
                break;
            case R.id.do_not_show:
                isChecked = ((CheckBox)v).isChecked();
                break;
        }
    }

    InitAlexaListener mCallback;

    // Container Activity must implement this interface
    public interface InitAlexaListener {
        public void onDialogDismissed();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (InitAlexaListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }
}
