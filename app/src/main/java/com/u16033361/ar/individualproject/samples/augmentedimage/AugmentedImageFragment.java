package com.u16033361.ar.individualproject.samples.augmentedimage;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;

public class AugmentedImageFragment extends ArFragment {
  private static final String TAG = "AugmentedImageFragment";
  private static final double MIN_OPENGL_VERSION = 3.0;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    //Check for AR compatibility first - be pretty useless without it
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      SnackbarHelper
          .getInstance()
          .showError(getActivity(), "[!] Sceneform requires Android N or later [!]");
    }

    String openGlVersionString =
        ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later");
      SnackbarHelper.getInstance()
          .showError(getActivity(), "[!] Sceneform requires OpenGL ES 3.0 or later [!]");
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    View view = super.onCreateView(inflater, container, savedInstanceState);

    //PlaneDiscovery used for Sceneform - only using augmented images here so bye bye
      //Absolutely love that Google's own IDE doesn't recognise Sceneform as a word
    getPlaneDiscoveryController().hide();
    getPlaneDiscoveryController().setInstructionView(null);
    getArSceneView().getPlaneRenderer().setEnabled(false);
    return view;
  }

  @Override
  protected Config getSessionConfiguration(Session session) {
    Config config = super.getSessionConfiguration(session);
    config.setFocusMode(Config.FocusMode.AUTO);
    if (!setupAugmentedImageDatabase(config, session)) {
      SnackbarHelper
              .getInstance()
              .showError(getActivity(), "[!] Database Setup Failed [!]");
    }
    return config;
  }

  private boolean setupAugmentedImageDatabase(Config config, Session session) {
    //AugmentedImageDatabase augmentedImageDatabase;
    AugmentedImageDatabaseHelper databaseHelper =
            new AugmentedImageDatabaseHelper(this.getContext(), session, true);
    AssetManager assetManager = getContext() != null ? getContext().getAssets() : null;
    if (assetManager == null) {
      Log.e(TAG, "[!] Cannot Initialise Database [!]");
      return false;
    }

    //TODO: Must be a better way of doing this
    //TODO: SPLASH SCREEN GOD DAMN IT
    while(!databaseHelper.getIsFilled()) {} //Wait for database to be filled!
    config.setAugmentedImageDatabase(databaseHelper.getAugmentedImageDatabase());
    return true;
  }
}
