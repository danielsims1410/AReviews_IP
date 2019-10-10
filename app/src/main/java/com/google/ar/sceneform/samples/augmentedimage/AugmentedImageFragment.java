/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.sceneform.samples.augmentedimage;

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
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extend the ArFragment to customize the ARCore session configuration to include Augmented Images.
 */
public class AugmentedImageFragment extends ArFragment {
  private static final String TAG = "AugmentedImageFragment";

  /// Image filenames in assets ///////////////////
  private static final String DEFAULT_IMAGE_NAME = "default.jpg";
  private static final String FLAG_IMAGE_NAME = "Logotest.jpg";
  private static final String COCO_POPS_PACKAGING = "coco_pops_packaging.jpg";
  private static final String DANI_DEX = "dani_dex.jpg";
  private static final String WHITE_COCO_POPS_PACKAGING = "white_coco_pops_packaging.jpg";
  /////////////////////////////////////////////////
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
    if (!setupAugmentedImageDatabase(config, session)) {
      SnackbarHelper
              .getInstance()
              .showError(getActivity(), "[!] Database Setup Failed [!]");
    }
    return config;
  }

  private boolean setupAugmentedImageDatabase(Config config, Session session) {
    //AugmentedImageDatabase augmentedImageDatabase;
    AugmentedImageDatabaseHelper databaseHelper = new AugmentedImageDatabaseHelper(this.getContext());
    AssetManager assetManager = getContext() != null ? getContext().getAssets() : null;
    if (assetManager == null) {
      Log.e(TAG, "[!] Cannot Initialise Database [!]");
      return false;
    }

    //Convert images->bitmap
    /*Bitmap augmentedImageBitmap = loadAugmentedImageBitmap(assetManager, DEFAULT_IMAGE_NAME);
    if (augmentedImageBitmap == null) { return false; }
    Bitmap logoBitmap = loadAugmentedImageBitmap(assetManager, FLAG_IMAGE_NAME);
    if (logoBitmap == null) { return false; }
    Bitmap cocoPopsBitmap = loadAugmentedImageBitmap(assetManager, COCO_POPS_PACKAGING);
    if(cocoPopsBitmap == null) { return false; }
    Bitmap daniDexBitmap = loadAugmentedImageBitmap(assetManager, DANI_DEX);
    if(daniDexBitmap == null) { return false; }
    Bitmap whiteCocoPopsBitmap = loadAugmentedImageBitmap(assetManager, WHITE_COCO_POPS_PACKAGING);
    if(whiteCocoPopsBitmap == null) { return false; }*/

    //Add images to database (one at a time)
    // TODO create pre-generated imgdb - will have several adverts/packaging
      //TODO so this ain't ideal long-term
    /*augmentedImageDatabase = new AugmentedImageDatabase(session);
    augmentedImageDatabase.addImage(DEFAULT_IMAGE_NAME, augmentedImageBitmap);
    augmentedImageDatabase.addImage(FLAG_IMAGE_NAME, logoBitmap);
    augmentedImageDatabase.addImage(COCO_POPS_PACKAGING, cocoPopsBitmap);
    augmentedImageDatabase.addImage(DANI_DEX, daniDexBitmap);
    augmentedImageDatabase.addImage(WHITE_COCO_POPS_PACKAGING, whiteCocoPopsBitmap);*/
    config.setAugmentedImageDatabase(databaseHelper.getImageDatabase(session));
    return true;
  }

  private Bitmap loadAugmentedImageBitmap(AssetManager assetManager, String filename) {
    try (InputStream is = assetManager.open(filename)) {
      return BitmapFactory.decodeStream(is);
    } catch (IOException e) {
      Log.e(TAG, "IO exception loading augmented image bitmap.", e);
    }
    return null;
  }
}
