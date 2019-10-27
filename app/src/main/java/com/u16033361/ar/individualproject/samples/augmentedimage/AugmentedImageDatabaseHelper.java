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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import static android.graphics.BitmapFactory.decodeStream;

public class AugmentedImageDatabaseHelper extends SQLiteOpenHelper {

    //////////////////////////////////////////////////////////////////////////
    // API STRINGS ////////////////////////////////////////////////////////////////////////////////
    private static final String API_URL = "https://api.nytimes.com/svc/books/v3/lists/current/";
    private final String[] API_URL_LISTS = {"hardcover-fiction", "hardcover-nonfiction",
            "young-adult", "humor",
            "advice-how-to-and-miscellaneous",
            "picture-books", "education"};
    private static final String API_APPENDIX = ".json?api-key=";
    private static final String API_KEY = "nswaoVUNKJ0N6YROAtnuls7nNHBGcs8G";
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // DATABASE STRINGS ///////////////////////////////////////////////////////////////////////////
    public static String DATABASE_NAME = "books_database.db";
    private static final int DATABASE_VERSION = 1;
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
    private static final String CREATE_DATABASE_TABLE_EXPENSES = "CREATE TABLE "
            + DATABASE_TABLE_BOOKS + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_TITLE_COLUMN + " TEXT," + KEY_AUTHOR_COLUMN + " TEXT,"
            + KEY_ISBN_COLUMN + " TEXT," + KEY_DESCRIPTION_COLUMN + " TEXT,"
            + KEY_REVIEW_COLUMN + " INTEGER," + KEY_TYPE_COLUMN + " TEXT,"
            + KEY_PAGE_COUNT_COLUMN + " INTEGER," + KEY_COVER_COLUMN + " BLOB);";
    ///////////////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////
    // BOOK INFO ////////////////////
    private String json;
    private String booktitle;
    private String bookisbn;
    private String bookauthor;
    private String bookdesc;
    private String bookreview;
    private String booktype;
    private int bookpagecount;
    private String bookcoverURLstr;
    private URL bookcoverURL;
    private Bitmap bookcover;
    ///////////////////////////////////

    /////////////////////////////////////////////////////
    // MISC ////////////////////////////////////////////////////////////
    private StringBuilder stringBuilder = new StringBuilder();
    private URL apiurl;
    private static final String TAG = "DATABASE HELPER";
    private AugmentedImageDatabase augmentedImageDatabase;
    private boolean filled = false;
    private boolean dbhasentries = false;
    ////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////
    // CLASS CONSTRUCTOR -> Activates background thread straight off the bat /////
    public AugmentedImageDatabaseHelper(Context context, Session session)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        new RetrieveData().execute(session);
    }
    //////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(SQLiteDatabase db) { db.execSQL(CREATE_DATABASE_TABLE_EXPENSES); }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion) {
        db.execSQL("DROP TABLE IF EXISTS '" + DATABASE_TABLE_BOOKS +"'");
        onCreate(db);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // BACKGROUND THREAD -> Handles all network tasks (API connectivity, Bitmap downloads, etc.) ////////////////////////////////////////
    class RetrieveData extends AsyncTask<Session, Void, Void> {
        JSONObject jsonObject;
        protected void onPreExecute() {
            //TODO: SPLASH LOADING SCREEN!
        }

        protected Void doInBackground(Session... params) {
            if(!databaseHasEntries()) {
                try {
                    stringBuilder.append("{\n\"genres\":[\n");
                    for (int i = 0, len = API_URL_LISTS.length; i < len; ++i) {
                        apiurl = new URL(API_URL + API_URL_LISTS[i] + API_APPENDIX + API_KEY);
                        HttpURLConnection urlConnection = (HttpURLConnection) apiurl.openConnection();
                        try {
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                stringBuilder.append(line).append("\n");
                            }
                            if (i != len - 1) stringBuilder.append(",\n");
                            bufferedReader.close();
                        } finally {
                            urlConnection.disconnect();
                        }
                    }
                    stringBuilder.append("]\n}");
                    json = stringBuilder.toString();
                    jsonObject = new JSONObject(json);
                } catch (Exception e) {
                    Log.e("Error", e.getMessage(), e);
                    return null;
                }
                AddToDatabase(jsonObject);
            }
            setImageDatabase(params[0]);
            return null;
        }

        protected void onPostExecute(AugmentedImageDatabase response) {
            if(response == null) {
                //TODO: Splashscreen showing FAIL
            }
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////
    // SETS ALL IMAGES IN DATABASE INTO IMGDB ////////////////////////////////////////////////
    public void setImageDatabase(Session session) {
        Log.i(TAG, "getImageDatabase() called!");
        augmentedImageDatabase = new AugmentedImageDatabase(session);
        String query = "SELECT * FROM " + DATABASE_TABLE_BOOKS;
        String title;
        String cover;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        while(cursor.moveToNext()) {
            title = cursor.getString(cursor.getColumnIndex(KEY_TITLE_COLUMN));
            cover = cursor.getString(cursor.getColumnIndex(KEY_COVER_COLUMN));
            try {
                bookcoverURL = new URL(cover);
                bookcover = BitmapFactory.decodeStream(bookcoverURL.openStream());
            }
            catch (IOException e) {
                Log.e("Error", e.getMessage(),e);
            }
            try {
            augmentedImageDatabase.addImage(title, bookcover); }
            catch(ImageInsufficientQualityException e) {
                Log.e("Error:", e.getMessage(), e);
            }
            Log.i(TAG, "[!] " + title + " Image Added to imgdb [!]");
            Log.i(TAG, "Cover URL: " + cover);
        }
        cursor.close();
        Log.i(TAG, "################## Imgdb Filled! ##################");
        filled = true;
    }
    //////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////
    // RETURNS IMGDB //////////////////////////////////////////////
    public AugmentedImageDatabase getAugmentedImageDatabase() {
        return augmentedImageDatabase;
    }
    ///////////////////////////////////////////////////////////////

    public boolean getIsFilled() {
        return filled;
    }

    public boolean databaseHasEntries() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor dbcursor = db.rawQuery("SELECT * FROM " + DATABASE_TABLE_BOOKS, null);
        if (dbcursor.moveToFirst()) { dbhasentries = true; }
        else { dbhasentries = false; }
        return dbhasentries;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ADDS ENTRIES INTO SQLITE DATABASE /////////////////////////////////////////////////////
    public void AddToDatabase(JSONObject response) {
        try {
            Log.i(TAG, "Reached AddToDatabase");
            ContentValues newVals = new ContentValues();
            JSONArray genres = response.getJSONArray("genres");
            for (int count = 0; count < genres.length(); count++) {
                JSONObject genreobj = genres.getJSONObject(count);
                JSONObject results = genreobj.getJSONObject("results");
                booktype = results.getString("list_name"); //Get Genre as Str

                JSONArray nytbooks = results.getJSONArray("books");

                for (int i = 0; i < nytbooks.length(); i++) {
                    JSONObject bookobj = nytbooks.getJSONObject(i);
                    booktitle = bookobj.getString("title");
                    bookisbn = bookobj.getString("primary_isbn13");
                    bookauthor = bookobj.getString("author");
                    bookdesc = bookobj.getString("description");
                    bookcoverURLstr = bookobj.getString("book_image");
                    newVals.put(KEY_TYPE_COLUMN, booktype);
                    newVals.put(KEY_TITLE_COLUMN, booktitle);
                    newVals.put(KEY_ISBN_COLUMN, bookisbn);
                    newVals.put(KEY_AUTHOR_COLUMN, bookauthor);
                    newVals.put(KEY_DESCRIPTION_COLUMN, bookdesc);
                    newVals.put(KEY_COVER_COLUMN, bookcoverURLstr);
                    // TODO: bookreview = GoogleApi -> Get Review via ISBN
                    // TODO: bookpagecount = GoogleApi -> Get page count via ISBN
                    SQLiteDatabase db = this.getWritableDatabase();
                    Log.i(TAG, "getWriteableDatabase() called");
                    db.insert(DATABASE_TABLE_BOOKS, null, newVals);
                    Log.i(TAG, "Database theoretically filled once");
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
}
