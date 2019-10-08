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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
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
  private LayoutInflater inflater;

  // View used as a renderable
  private static CompletableFuture<ViewRenderable> infoBox;

  public AugmentedImageNode(Context context, String imagename, View view) {
    // Build the renderable
    if (infoBox == null) {
      infoBox =
          ViewRenderable
                  .builder()
                  .setView(context, view)
                  .build();
    }
  }

  //@SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void setImage(AugmentedImage image) {
    this.image = image;

    // If viewrenderable hasn't loaded -> repeat until it is
    if (!infoBox.isDone()) {
      CompletableFuture.allOf(infoBox)
          .thenAccept((Void aVoid) -> setImage(image))
          .exceptionally(throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
              });
    }
    // Set the anchor to centre of image
    setAnchor(image.createAnchor(image.getCenterPose()));

    // Create the node
    Vector3 localPosition = new Vector3();
    Node cornerNode;

    // Set the renderable at position declared relative to centre of image
    localPosition
            .set(0.0f, 0.0f, -0.6f * image.getExtentZ());
    cornerNode = new Node();
    cornerNode.setParent(this);
    cornerNode.setLocalPosition(localPosition);
    cornerNode.setLocalRotation(new Quaternion(90f, 0f,0f,-90f));
    cornerNode.setRenderable(infoBox.getNow(null));
  }

  public AugmentedImage getImage() {
    return image;
  }
}
