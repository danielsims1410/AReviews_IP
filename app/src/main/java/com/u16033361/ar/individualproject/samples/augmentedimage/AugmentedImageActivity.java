package com.u16033361.ar.individualproject.samples.augmentedimage;

//ARCore imports
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Frame;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

//Data Containers
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

//Generic
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.Calendar;

public class AugmentedImageActivity extends AppCompatActivity implements BookAdapter.onItemListener{

  private ArFragment arFragment;
  private ImageView fitToScanView, itemDetected;
  private RecyclerView rvBookList;
  private Button btnShowBooks;
  private BookAdapter bookAdapter;
  private AugmentedImageDatabaseHelper databaseHelper;
  private ArrayList<Book> bookCovers;
  private ArrayList<Book> booksList;
  private boolean listShowing;
  private FrameLayout frameLayout;

  //Augmented image and its associated center pose anchor, keyed by the augmented image in
  //the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    frameLayout = findViewById(R.id.fLayout);
    arFragment = (ArFragment)getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    databaseHelper = new AugmentedImageDatabaseHelper(this, null, false);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);
    itemDetected = findViewById(R.id.image_item_detected);
    rvBookList = findViewById(R.id.rvBooks);
    bookCovers = databaseHelper.getBookCovers();
    booksList = databaseHelper.getAllBooks();
    bookAdapter = new BookAdapter(this, bookCovers, this);
    btnShowBooks = findViewById(R.id.btnShowBooks);
    listShowing = false;
    rvBookList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    rvBookList.setAdapter(bookAdapter);
    rvBookList.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener());
    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    if(isUpdateDay()) showUpdateSnackbar();
    showUpdateSnackbar();
  }

  @Override
  public void onItemClick(int position) {
    Intent intent = new Intent(this, UpdateReviewActivity.class);
    intent.putExtra("book", booksList.get(position));
    startActivity(intent);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (augmentedImageMap.isEmpty()) {
      fitToScanView.setVisibility(View.VISIBLE);
    }
  }

  @Override
  protected void onDestroy() {
      super.onDestroy();
  }

  //@param frameTime -> time since last frame.
  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();

    //If there is no frame, just return.
    if(frame == null) return;

    Collection<AugmentedImage> updatedAugmentedImages =
        frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {

          //Image has been detected with insufficient world-mapping data.
        case PAUSED:
          itemDetected.setVisibility(View.VISIBLE);
          break;

          //Image is now being tracked.
        case TRACKING:
          fitToScanView.setVisibility(View.GONE);
          itemDetected.setVisibility(View.GONE);

          // Create an anchor for detected image.
          if (!augmentedImageMap.containsKey(augmentedImage)) {
             AugmentedImageNode node = new AugmentedImageNode(this,
                     new ViewRenderable(augmentedImage.getName(), this).getView());
            node.setImage(augmentedImage);
            augmentedImageMap.put(augmentedImage, node);
            arFragment.getArSceneView().getScene().addChild(node);
          }
          break;

          //Image no longer being tracked.
        case STOPPED:
          augmentedImageMap.remove(augmentedImage);
          fitToScanView.setVisibility(View.VISIBLE);
          break;
      }
    }
  }

  //Switch the visibility of the RecyclerView showing the compatible books.
  public void switchCompatibleBooks(View v) {
    if(listShowing) {
      rvBookList.setVisibility(View.INVISIBLE);
      btnShowBooks.setText("Show");
      listShowing = false;
    }
    else {
      rvBookList.setVisibility(View.VISIBLE);
      btnShowBooks.setText("Hide");
      listShowing = true;
    }
  }

  //Shows the Snackbar announcing the list has been updated! Yay!
  private void showUpdateSnackbar() {
      Snackbar snackbar = Snackbar
              .make(frameLayout, "Book List Updated!", Snackbar.LENGTH_LONG)
              .setAction("VIEW", (View v) -> {
                      rvBookList.setVisibility(View.VISIBLE);
                      btnShowBooks.setText("Hide");
                      listShowing = true;
              });
      snackbar.setActionTextColor(Color.rgb(209, 73, 0));
      snackbar.show();
  }

  //Returns true if the day of the month is 10 or 25.
  private boolean isUpdateDay() {
      boolean isUpdateDay = false;
      Calendar c = Calendar.getInstance();
      int currentDay = c.get(Calendar.DAY_OF_MONTH);
      if(currentDay == 10 || currentDay == 25) isUpdateDay = true;
      return isUpdateDay;
  }
}
