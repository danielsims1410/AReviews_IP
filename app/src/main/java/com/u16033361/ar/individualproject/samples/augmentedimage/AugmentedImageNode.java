package com.u16033361.ar.individualproject.samples.augmentedimage;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage image;

  // View used as a renderable
  private static CompletableFuture<ViewRenderable> interfaceRender;

  public AugmentedImageNode(Context context, View view) {
    // Build the renderable
      interfaceRender =
          ViewRenderable
                  .builder()
                  .setView(context, view)
                  .build();
  }

 public void setImage(AugmentedImage image) {
    this.image = image;

    if (!interfaceRender.isDone()) {
      CompletableFuture.allOf(interfaceRender)
          .thenAccept((Void aVoid) -> setImage(image))
          .exceptionally(throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
              });
    }
    // Set the anchor to centre of image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    // Create the node.
    Vector3 localPosition = new Vector3();
    Node node;

    // Set the renderable at position declared relative to centre of image.
    localPosition
            .set(0.0f, 0.0f, -0.6f * image.getExtentZ());
     node = new Node();
     node.setParent(this);
     node.setLocalPosition(localPosition);
     node.setLocalRotation(new Quaternion(90f, 0f,0f,-90f));
     node.setRenderable(interfaceRender.getNow(null));
  }
}
