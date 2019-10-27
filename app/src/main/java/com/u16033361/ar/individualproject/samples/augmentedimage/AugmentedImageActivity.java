package com.u16033361.ar.individualproject.samples.augmentedimage;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AugmentedImageActivity extends AppCompatActivity {

  private ArFragment arFragment;
  private ImageView fitToScanView, itemDetected;
  private TextView textView;
  private View viewTest;
  private CheckBox checkBox;

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    viewTest = inflater.inflate(R.layout.drawable_test, null, false);
    textView = viewTest.findViewById(R.id.image_desc);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);
    itemDetected = findViewById(R.id.image_item_detected);
    checkBox = viewTest.findViewById(R.id.checkbox_test);
    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (augmentedImageMap.isEmpty()) {
      fitToScanView.setVisibility(View.VISIBLE);
    }
  }

  //@param frameTime -> time since last frame.
  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();

    // If there is no frame, just return. Fuck it.
    if (frame == null) {
      return;
    }

    Collection<AugmentedImage> updatedAugmentedImages =
        frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          itemDetected.setVisibility(View.VISIBLE);
          break;

        case TRACKING:
          fitToScanView.setVisibility(View.GONE);
          itemDetected.setVisibility(View.GONE);

          // Create an anchor for detected image
          if (!augmentedImageMap.containsKey(augmentedImage)) {
             AugmentedImageNode node = new AugmentedImageNode(this, augmentedImage.getName(), viewTest);
            node.setImage(augmentedImage);
            augmentedImageMap.put(augmentedImage, node);
            arFragment.getArSceneView().getScene().addChild(node);
          }
          break;

        case STOPPED:
          augmentedImageMap.remove(augmentedImage);
          break;
      }
    }
  }

  //TODO Connect to db and receive info here
  //DEFUNCT atm
  private void changePopUpInfo(String imageName) {
      switch(imageName) {
          case "default.jpg":
              textView.setText("We're destroying this!");
              break;
          case "Logotest.jpg":
              textView.setText("I'm lovin' it!");
              break;
          case "coco_pops_packaging.jpg":
              textView.setText("Tesco Coco Snaps > This");
              break;
          case "dani_dex.jpg":
              textView.setText("I love you x");
              textView.setTextSize(40);
              checkBox.setVisibility(View.GONE);
              viewTest.setBackgroundColor(Color.rgb(99,00,00));
              break;
          case "white_coco_pops_packaging.jpg":
              textView.setText("Tick if you prefer Coco Snaps: ");
              break;
          default:
              textView.setText("Something went wrong!");
              break;
      }
  }
}
