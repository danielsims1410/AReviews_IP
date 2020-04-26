package com.u16033361.ar.individualproject.samples.augmentedimage;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class UpdateReviewActivity extends AppCompatActivity {
    private static final String TAG = "Update Book Activity";

    private Book book;
    private TextView txtTitleLabel;
    private ArrayList<Button> starButtons;
    private AugmentedImageDatabaseHelper databaseHelper;
    private double reviewScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_review);

        //If an extra's been passed in from the last activity...
        if(getIntent().hasExtra("book")) {
            book = getIntent().getParcelableExtra("book");
            Log.d(TAG, "Book Passed: " + book.getBookTitle());
        }

        //Create's a DatabaseHelper without running the network code.
        databaseHelper = new AugmentedImageDatabaseHelper(this, null, false);

        //Gets the existing review score.
        reviewScore = Double.valueOf(databaseHelper.getInfo(book.getBookTitle(), "review"));

        //Identify Views
        starButtons = new ArrayList<>();
        starButtons.add(findViewById(R.id.btnStar1));
        starButtons.add(findViewById(R.id.btnStar2));
        starButtons.add(findViewById(R.id.btnStar3));
        starButtons.add(findViewById(R.id.btnStar4));
        starButtons.add(findViewById(R.id.btnStar5));
        txtTitleLabel = findViewById(R.id.txtUpdateHeaderTitle);
        setUI();
    }

    //Sets the UI to its initial state.
    public void setUI() {
        txtTitleLabel.setText(capitaliseTitle(book.getBookTitle()));
        int index;
        for(index = 0; index < book.getBookReview(); index++) {
            starButtons.get(index).setBackgroundResource(R.drawable.review_full);
        }
        if(reviewScore % book.getBookReview() != 0) {
            starButtons.get(index).setBackgroundResource(R.drawable.review_half_vertical);
        }
    }

    //Generic function to handle the user's button pressing.
    public void buttonHandler(View view) {
        switch(view.getId()) {
            case(R.id.btnStar5):
                setStarButtons(5);
                reviewScore = 5;
                break;
            case(R.id.btnStar4):
                setStarButtons(4);
                reviewScore = 4;
                break;
            case(R.id.btnStar3):
                setStarButtons(3);
                reviewScore = 3;
                break;
            case(R.id.btnStar2):
                setStarButtons(2);
                reviewScore = 2;
                break;
            case(R.id.btnStar1):
                setStarButtons(1);
                reviewScore = 1;
                break;
            case(R.id.btnSaveUpdate):
                Toast toast = Toast.makeText(this, "Updating Score...",
                        Toast.LENGTH_LONG);
                toast.show();
                databaseHelper.updateReview(book.getBookID(), (int)reviewScore);
                Intent intent = new Intent(this, AugmentedImageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                break;
        }
    }

    //Fills in vertical stars to reflect user's choice.
    private void setStarButtons(int score) {
        int index;
        for(index = 0; index < score; index++) {
            starButtons.get(index).setBackgroundResource(R.drawable.review_full);
        }
        for(int i = index; i < 5; i++) {
            starButtons.get(i).setBackgroundResource(R.drawable.review_empty);
        }
    }

    //Just for vanity and aesthetic, this.
    //Capitalises every word in a given string.
    private static String capitaliseTitle(String title) {
        String[] wordsArr = title.split(" ");
        String capitaliseWord = "";
        for(String word : wordsArr) {
            String firstLetter = word.substring(0,1);
            String postFirst = word.substring(1);
            capitaliseWord += firstLetter.toUpperCase() + postFirst.toLowerCase() + " ";
        }
        return capitaliseWord.trim();
    }
}
