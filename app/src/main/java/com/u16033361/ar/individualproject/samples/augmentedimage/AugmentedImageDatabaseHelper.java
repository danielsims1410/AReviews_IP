package com.u16033361.ar.individualproject.samples.augmentedimage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.ImageInsufficientQualityException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

public class AugmentedImageDatabaseHelper extends SQLiteOpenHelper {

    //////////////////////////////////////////////////////////////////////////
    // API STRINGS ////////////////////////////////////////////////////////////////////////////////
    private static final String NYT_API_URL = "https://api.nytimes.com/svc/books/v3/lists/current/";
    private final String[] NYT_API_URL_LISTS = {"hardcover-fiction", "hardcover-nonfiction",
            "young-adult", "humor",
            "advice-how-to-and-miscellaneous",
            "picture-books", "education"};
    private static final String NYT_API_APPENDIX = ".json?api-key=";
    private final String NYT_API_KEY;

    private static final String GOOGLE_API_URL = "https://www.googleapis.com/books/v1/volumes?q=";
    private static final String GOOGLE_API_APPENDIX = "&key=";
    private final String GOOGLE_API_KEY;
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // DATABASE STRINGS ///////////////////////////////////////////////////////////////////////////
    public static String DATABASE_NAME = "books_database.db";
    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_TABLE_BOOKS = "books";
    private static final String KEY_ID = "_id";
    private static final String KEY_TITLE_COLUMN = "TITLE_COLUMN";
    private static final String KEY_AUTHOR_COLUMN = "AUTHOR_COLUMN";
    private static final String KEY_ISBN_COLUMN = "ISBN_COLUMN";
    private static final String KEY_DESCRIPTION_COLUMN = "DESCRIPTION_COLUMN";
    private static final String KEY_REVIEW_COLUMN = "REVIEW_COLUMN";
    private static final String KEY_TYPE_COLUMN = "TYPE_COLUMN";
    private static final String KEY_PAGE_COUNT_COLUMN = "PAGE_COUNT_COLUMN";
    private static final String KEY_COVER_COLUMN = "COVER_COLUMN";
    private static final String KEY_NEW_ENTRY_COLUMN = "NEW_ENTRY_COLUMN";
    private static final String KEY_DAY_ADDED_COLUMN = "DAY_COLUMN";

    //Create Books Table
    private static final String CREATE_DATABASE_TABLE_BOOKS = "CREATE TABLE "
            + DATABASE_TABLE_BOOKS + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_TITLE_COLUMN + " TEXT," + KEY_AUTHOR_COLUMN + " TEXT,"
            + KEY_ISBN_COLUMN + " TEXT," + KEY_DESCRIPTION_COLUMN + " TEXT,"
            + KEY_REVIEW_COLUMN + " INTEGER," + KEY_TYPE_COLUMN + " TEXT,"
            + KEY_PAGE_COUNT_COLUMN + " INTEGER," + KEY_COVER_COLUMN + " BLOB,"
            + KEY_NEW_ENTRY_COLUMN + " INTEGER, " + KEY_DAY_ADDED_COLUMN + " INTEGER);";
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////
    // BOOK INFO ////////////////////
    private String json;
    private String booktitle;
    private String bookisbn;
    private String bookauthor;
    private String bookdesc;
    private String bookreview = "-1"; //Not always available.
    private String bookpagecount = "-1"; //Not always available.
    private String booktype;
    private String bookcoverURLstr;
    private String booknewentry;
    private URL bookcoverURL;
    Calendar cal = Calendar.getInstance();
    private String currentDay;
    private Bitmap bookcover;
    private byte[] bookcoverbytearr;
    ///////////////////////////////////

    /////////////////////////////////////////////////////
    // MISC ////////////////////////////////////////////////////////////
    private StringBuilder stringBuilderNYT = new StringBuilder(),
            stringBuilderGoogle = new StringBuilder();
    private URL nytapiurl, googleapiurl;
    private static final String TAG = "DATABASE HELPER";
    private AugmentedImageDatabase augmentedImageDatabase;
    private boolean filled = false;
    private boolean dbhasentries;
    ////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////
    // CLASS CONSTRUCTOR -> Activates background thread straight off the bat /////
    public AugmentedImageDatabaseHelper(Context context, Session session, Boolean run)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        NYT_API_KEY = context.getString(R.string.nyt_api_key);
        GOOGLE_API_KEY = context.getString(R.string.google_books_api_key);

        this.currentDay = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        if(run) {
            if(recreateDatabase()) {
                new RetrieveData().execute(session);
            } else {
                setImageDatabase(session);
            }
        }
    }
    //////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DATABASE_TABLE_BOOKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion) {
        db.execSQL("DROP TABLE IF EXISTS '" + DATABASE_TABLE_BOOKS + "'");
        onCreate(db);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // BACKGROUND THREAD -> Handles all network tasks (API connectivity, Bitmap downloads, etc.) ////////////////////////////////////////
    class RetrieveData extends AsyncTask<Session, Void, Void> {
        JSONObject jsonObjectNYT;
        protected Void doInBackground(Session... params) {
            try {
                //Starts building the JSON.
                stringBuilderNYT.append("{\n\"genres\":[\n");

                //For every chosen genre.
                for (int i = 0, len = NYT_API_URL_LISTS.length; i < len; ++i) {
                    nytapiurl = new URL(NYT_API_URL + NYT_API_URL_LISTS[i] + NYT_API_APPENDIX + NYT_API_KEY);
                    HttpURLConnection urlConnection = (HttpURLConnection)nytapiurl.openConnection();
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            //Adds the next line from the API's result.
                            stringBuilderNYT.append(line).append("\n");
                        }
                        if (i != len - 1) stringBuilderNYT.append(",\n");
                        bufferedReader.close();
                    } finally {
                        urlConnection.disconnect();
                    }
                }
                    //Closes off JSON array and object.
                    stringBuilderNYT.append("]\n}");
                    json = stringBuilderNYT.toString();
                    jsonObjectNYT = new JSONObject(json);
                } catch (Exception e) {
                    Log.e("Error", e.getMessage(), e);
                    return null;
                }
            //Adds the JSON object to the device's database.
            addToDatabase(jsonObjectNYT);

            //Adds the images from the database into the image database.
            setImageDatabase(params[0]);
            return null;
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // ADDS ENTRIES INTO SQLITE DATABASE /////////////////////////////////////////////////////
    public void addToDatabase(JSONObject response) {
        Bitmap tempbookcover;
        try {
            ContentValues newVals = new ContentValues();
            JSONArray genres = response.getJSONArray("genres");
            JSONObject jsonObjectGoogle;
            SQLiteDatabase db = this.getWritableDatabase();

            //If it has entries already, it needs replacing. Delete it all.
            if(databaseHasEntries()) {
                db.execSQL("DELETE FROM '" + DATABASE_TABLE_BOOKS + "'");
                Log.d(TAG, "ALL ENTRIES DELETED HOPEFULLY MAYBE");
            }

            //Loop through every genre in the JSON.
            for (int count = 0; count < genres.length(); count++) {
                JSONObject genreobj = genres.getJSONObject(count);
                JSONObject results = genreobj.getJSONObject("results");
                booktype = results.getString("list_name"); //Get Genre as Str

                JSONArray nytbooks = results.getJSONArray("books");

                //Loop through every book in the JSON array.
                for (int i = 0; i < nytbooks.length(); i++) {
                    JSONObject bookobj = nytbooks.getJSONObject(i);

                    //Get title, ISBN, author, description and URL for the book cover.
                    booktitle = bookobj.getString("title");
                    Log.i(TAG, "[!] Book Title: " + booktitle.replaceAll(" ", "+") + "[!]");
                    bookisbn = bookobj.getString("primary_isbn13");
                    bookauthor = bookobj.getString("author");
                    bookdesc = bookobj.getString("description");
                    bookcoverURLstr = bookobj.getString("book_image");

                    //Books that haven't been on the list for long are considered new.
                    int weeksonlist = bookobj.getInt("weeks_on_list");
                    if(weeksonlist < 4 && weeksonlist > 0) booknewentry = "1";
                    else booknewentry = "0";

                    //Get the book cover -> Bitmap and Byte[].
                    bookcoverURL = new URL(bookcoverURLstr);
                    tempbookcover = BitmapFactory.decodeStream(bookcoverURL.openStream());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    tempbookcover.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                    bookcoverbytearr = stream.toByteArray();

                    //Queries the Google Books API next to get further info.
                    //Page count and Avg. Review Score.
                    googleapiurl = new URL(GOOGLE_API_URL + booktitle.replaceAll(" ","+") + "+" + bookauthor.replaceAll(" ","+") + GOOGLE_API_APPENDIX + GOOGLE_API_KEY);
                    HttpURLConnection urlConnection = (HttpURLConnection) googleapiurl.openConnection();
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            stringBuilderGoogle.append(line).append("\n");
                        }
                    } catch(Exception e) {
                        Log.e("Error", e.getMessage(), e);
                    } finally {
                        urlConnection.disconnect();
                    }
                    jsonObjectGoogle = new JSONObject(stringBuilderGoogle.toString());
                    JSONArray googleItems = jsonObjectGoogle.getJSONArray("items");
                    int index = 0;

                    //An exact match (or a match at all) isn't always found, so have to check.
                    boolean resultFound = true;
                    JSONObject result = googleItems.getJSONObject(index);

                    //Scrolls through all results until a perfect match is found.
                    while(!result.getJSONObject("volumeInfo").getString("title").equalsIgnoreCase(booktitle)) {
                        try { result = googleItems.getJSONObject(++index); }
                        catch(Exception e) { resultFound = false; break; }
                        Log.i(TAG,"IN WHILE LOOP");
                    }

                    //Yay, a result! Add it!
                    if(resultFound) {
                        JSONObject volumeInfo = result.getJSONObject("volumeInfo");
                        if (volumeInfo.has("pageCount")) bookpagecount = volumeInfo.getString("pageCount");
                        if (volumeInfo.has("averageRating")) bookreview = volumeInfo.getString("averageRating");
                    }

                    //All the information above into a ContentsValue and shove into the database.
                    newVals.put(KEY_TYPE_COLUMN, booktype);
                    newVals.put(KEY_TITLE_COLUMN, booktitle);
                    newVals.put(KEY_ISBN_COLUMN, bookisbn);
                    newVals.put(KEY_AUTHOR_COLUMN, bookauthor);
                    newVals.put(KEY_DESCRIPTION_COLUMN, bookdesc);
                    newVals.put(KEY_COVER_COLUMN, bookcoverbytearr);
                    newVals.put(KEY_REVIEW_COLUMN, bookreview);
                    newVals.put(KEY_PAGE_COUNT_COLUMN, bookpagecount);
                    newVals.put(KEY_NEW_ENTRY_COLUMN, booknewentry);
                    newVals.put(KEY_DAY_ADDED_COLUMN, currentDay);
                    db.insert(DATABASE_TABLE_BOOKS, null, newVals);

                    //Reset the stringbuilder.
                    stringBuilderGoogle.setLength(0);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            Log.e("Error", e.getMessage(), e);
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////
    // SETS ALL IMAGES IN DATABASE INTO IMGDB ////////////////////////////////////////////////
    public void setImageDatabase(Session session) {

        Log.i(TAG, "[!] Filling Image Database [!]");
        augmentedImageDatabase = new AugmentedImageDatabase(session);

        String query = "SELECT * FROM " + DATABASE_TABLE_BOOKS;
        String title;
        byte[] cover;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        //Going through every entry in the Books table.
        while(cursor.moveToNext()) {

            //Image Database takes two params - a name (Str) and a bitmap.
            title = cursor.getString(cursor.getColumnIndex(KEY_TITLE_COLUMN));
            cover = cursor.getBlob(cursor.getColumnIndex(KEY_COVER_COLUMN));

            //(Try to) Decode the Byte[] into a Bitmap.
            try {
                bookcover = BitmapFactory.decodeByteArray(cover, 0, cover.length);
            } catch (Exception e) {
                Log.e("Error:", e.getMessage(),e);
            }

            //(Try to) Add the image into the Image Database.
            //If image quality is too, it's rejected.
            try {
            augmentedImageDatabase.addImage(title, bookcover);
            } catch(ImageInsufficientQualityException e) {
                Log.e("Error:", e.getMessage(), e);
            }
        }
        cursor.close();
        Log.i(TAG, "[!] Image Database Filled [!]");
        filled = true;
    }
    //////////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////////////////////////////
    // GETTERS //////////////////////////////////////////////
    //Returns image database
    public AugmentedImageDatabase getAugmentedImageDatabase() { return augmentedImageDatabase; }

    //Returns whether the database has been filled or not.
    public boolean getIsFilled() { return filled; }

    //Get specified information from database.
    public String getInfo(String title, String query) {
        String column = "";
        SQLiteDatabase db = this.getReadableDatabase();
        switch(query) {
            case("author"): column = KEY_AUTHOR_COLUMN; break;
            case("description"): column = KEY_DESCRIPTION_COLUMN; break;
            case("review"): column = KEY_REVIEW_COLUMN; break;
            case("pagecount"): column = KEY_PAGE_COUNT_COLUMN; break;
        }
        Cursor dbcursor = db.rawQuery("SELECT " + column + " FROM " +
                DATABASE_TABLE_BOOKS + " WHERE " + KEY_TITLE_COLUMN + " = " + "\"" + title + "\"", null);
        dbcursor.moveToFirst();
        String info = dbcursor.getString(dbcursor.getColumnIndex(column));
        dbcursor.close();
        return info;
    }

    //Get all book covers.
    public ArrayList<Book> getBookCovers() {
        SQLiteDatabase database = this.getReadableDatabase();
        ArrayList<Book> bookCovers = new ArrayList<>();
        String query = "SELECT " + KEY_COVER_COLUMN  + ", " + KEY_NEW_ENTRY_COLUMN + " FROM " + DATABASE_TABLE_BOOKS;
        Cursor cursor = database.rawQuery(query, null);
        while(cursor.moveToNext()) {
            Book book = new Book();
            book.setImageByte(cursor.getBlob(cursor.getColumnIndex(KEY_COVER_COLUMN)));
            book.setNewEntry(cursor.getInt(cursor.getColumnIndex(KEY_NEW_ENTRY_COLUMN)));
            bookCovers.add(book);
        }
        cursor.close();
        return bookCovers;
    }

    //Get all the books in all their glory (and their attributes).
    public ArrayList<Book> getAllBooks() {
        SQLiteDatabase database = this.getReadableDatabase();
        ArrayList<Book> books = new ArrayList<>();
        String query = "SELECT * FROM " + DATABASE_TABLE_BOOKS;
        Cursor cursor = database.rawQuery(query, null);
        while(cursor.moveToNext()) {
            Book newBook = new Book();
            newBook.setBookID(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
            newBook.setBookGenre(cursor.getString(cursor.getColumnIndex(KEY_TYPE_COLUMN)));
            newBook.setBookTitle(cursor.getString(cursor.getColumnIndex(KEY_TITLE_COLUMN)));
            newBook.setBookISBN(cursor.getString(cursor.getColumnIndex(KEY_ISBN_COLUMN)));
            newBook.setBookAuthor(cursor.getString(cursor.getColumnIndex(KEY_AUTHOR_COLUMN)));
            newBook.setBookDesc(cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION_COLUMN)));
            newBook.setBookReview(cursor.getInt(cursor.getColumnIndex(KEY_REVIEW_COLUMN)));
            newBook.setBookPages(cursor.getInt(cursor.getColumnIndex(KEY_PAGE_COUNT_COLUMN)));
            newBook.setBookDayAdded(cursor.getInt(cursor.getColumnIndex(KEY_DAY_ADDED_COLUMN)));
            newBook.setImageByte(cursor.getBlob(cursor.getColumnIndex(KEY_COVER_COLUMN)));
            newBook.setNewEntry(cursor.getInt(cursor.getColumnIndex(KEY_NEW_ENTRY_COLUMN)));
            books.add(newBook);
        }
        cursor.close();
        return books;
    }

    //Get the day the database was last updated.
    //To avoid the app being updated every single time it's opened on the 10th or 25th.
    private int getDayLastAdded() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor dbcursor = db.rawQuery("SELECT " + KEY_DAY_ADDED_COLUMN + " FROM " + DATABASE_TABLE_BOOKS, null);
        dbcursor.moveToFirst();
        int dayLastAdded = dbcursor.getInt(dbcursor.getColumnIndex(KEY_DAY_ADDED_COLUMN));
        dbcursor.close();
        return dayLastAdded;
    }
    ///////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////
    // DATABASE UPDATE ///////////////////////////////////////////////////////
    //Updates the review score to a new, given value.
    public void updateReview(int bookID, int newReviewScore) {
        SQLiteDatabase database = this.getWritableDatabase();
        String query = "UPDATE " + DATABASE_TABLE_BOOKS + " SET " + KEY_REVIEW_COLUMN + " = "
                + newReviewScore + " WHERE " + KEY_ID + " = " + bookID;
        database.execSQL(query);
    }
    ///////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////
    // BOOLEAN METHODS ///////////////////////////////////////////////////////
    //Checks whether database has entries or not.
    private boolean databaseHasEntries() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor dbcursor = db.rawQuery("SELECT * FROM " + DATABASE_TABLE_BOOKS, null);

        //Only need to check the first entry, if it's there then the db has entries.
        dbhasentries = dbcursor.moveToFirst();
        dbcursor.close();
        return dbhasentries;
    }

    /* Checks conditions to see whether database should be recreated or not.
       One of Two Conditions must be met:
            • It's the 10th or 25th day of the month.
            • Subsequently, the database was last updated on the other day to the current.
                OR
            • The database is empty.
     */
    private boolean recreateDatabase() {
        boolean recreate = false;
        if(databaseHasEntries()) {
            if (currentDay.equals("10") || currentDay.equals("25")) {
                if (Integer.parseInt(currentDay) != getDayLastAdded()) {
                    recreate = true;
                }
            }
        }
        else recreate = true;
        return recreate;
    }
    ///////////////////////////////////////////////////////////////
}
