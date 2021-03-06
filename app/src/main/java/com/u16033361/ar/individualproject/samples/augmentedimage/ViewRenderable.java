package com.u16033361.ar.individualproject.samples.augmentedimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class ViewRenderable {

    private String title;
    private View display;
    private TextView txtTitle, txtAuthor, txtDescription, txtPageCount;
    private ArrayList<ImageView> imgReview;
    private AugmentedImageDatabaseHelper aidh;
    private ScrollView scrollView;
    private double reviewScore;
    private Bitmap bmpStarFull, bmpStarHalf, bmpStarEmpty, bmpStarNull;

    public ViewRenderable(String bookTitle, Context context) {
        this.title = bookTitle;
        aidh = new AugmentedImageDatabaseHelper(context, null, false);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
        this.display = inflater.inflate(R.layout.ar_display, null, false);
        this.txtPageCount = display.findViewById(R.id.txtPageCount);
        this.txtTitle = display.findViewById(R.id.txtTitle);
        this.txtAuthor = display.findViewById(R.id.txtAuthor);
        this.imgReview = new ArrayList<>();
        this.imgReview.add(display.findViewById(R.id.imgReview1));
        this.imgReview.add(display.findViewById(R.id.imgReview2));
        this.imgReview.add(display.findViewById(R.id.imgReview3));
        this.imgReview.add(display.findViewById(R.id.imgReview4));
        this.imgReview.add(display.findViewById(R.id.imgReview5));
        this.txtDescription = display.findViewById(R.id.txtDescription);
        this.scrollView = display.findViewById(R.id.sv_description);
        bmpStarFull = BitmapFactory.decodeResource(context.getResources(), R.drawable.review_full);
        bmpStarHalf = BitmapFactory.decodeResource(context.getResources(), R.drawable.review_half);
        bmpStarEmpty = BitmapFactory.decodeResource(context.getResources(), R.drawable.review_empty);
        bmpStarNull = BitmapFactory.decodeResource(context.getResources(), R.drawable.review_null);
        this.txtTitle.setText(title);
        this.txtAuthor.setText(aidh.getInfo(title, "author"));
        this.txtDescription.setText(aidh.getInfo(title, "description"));
        this.txtPageCount.setText(aidh.getInfo(title, "pagecount") + " Pages");

        //Handles Review Stars
        this.reviewScore = Double.valueOf(aidh.getInfo(title, "review"));
        if(reviewScore > 0.5) {
            int reviewScoreFloor = (int)reviewScore;
            double floorDiv = reviewScore/reviewScoreFloor;
            int i;
            for (i = 0; i < reviewScoreFloor; i++) {
                imgReview.get(i).setVisibility(View.VISIBLE);
                imgReview.get(i).setImageBitmap(bmpStarFull);
            }
            for (int j = i; j < 5; j++) {
                imgReview.get(j).setVisibility(View.VISIBLE);
                imgReview.get(j).setImageBitmap(bmpStarEmpty);
            }
            if (floorDiv != 1) {
                imgReview.get(i).setImageBitmap(bmpStarHalf);
            }
        }
        else if(reviewScore == -1) {
            imgReview.get(0).setVisibility(View.VISIBLE);
            imgReview.get(0).setImageBitmap(bmpStarNull);
        }

        else if(reviewScore == 0.5 || reviewScore == 0) {
            for(int i = 0; i < 5; i++) {
                imgReview.get(i).setVisibility(View.VISIBLE);
                imgReview.get(i).setImageBitmap(bmpStarEmpty);
            }
            if(reviewScore == 0.5) imgReview.get(0).setImageBitmap(bmpStarHalf);
        }

        if(reviewScore < 2) scrollView.setBackgroundColor(Color.parseColor("#990000"));
        else if(reviewScore > 3) scrollView.setBackgroundColor(Color.parseColor("#009E08"));
    }

    public View getView() { return display; }
}
