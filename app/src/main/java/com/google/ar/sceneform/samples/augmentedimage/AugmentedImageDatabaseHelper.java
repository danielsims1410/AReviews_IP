package com.google.ar.sceneform.samples.augmentedimage;

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

    // API Strings //////////
    private static final String API_URL = "https://api.nytimes.com/svc/books/v3/lists/current/";
    private final String[] API_URL_LISTS = {"hardcover-fiction", "hardcover-nonfiction",
            "young-adult", "humor",
            "advice-how-to-and-miscellaneous",
            "picture-books", "education"};
    private static final String API_APPENDIX = ".json?api-key=";
    private static final String API_KEY = "nswaoVUNKJ0N6YROAtnuls7nNHBGcs8G";
    /////////////////////////

    // Database Strings //////////
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
    /////////////////////////

    // Book info //////////
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
    /////////////////////////

    // Misc //////////
    private StringBuilder stringBuilder = new StringBuilder();
    URL apiurl;

    /////////////////////////

    public AugmentedImageDatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        RetrieveData RD = new RetrieveData();
        RD.execute();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DATABASE_TABLE_EXPENSES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion) {
        db.execSQL("DROP TABLE IF EXISTS '" + DATABASE_TABLE_BOOKS +"'");
        onCreate(db);
    }

    class RetrieveData extends AsyncTask<Void, Void, JSONObject> {
        JSONObject jsonObject;

        protected void onPreExecute() {
            //TODO: SPLASH LOADING SCREEN!
        }

        protected JSONObject doInBackground(Void... urls) {
            try {
                for(int i = 0, len = API_URL_LISTS.length; i < len; ++i) {
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
                return jsonObject;
            }
            catch(Exception e) {
                Log.e("Error", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(JSONObject response) {
            if(response == null) {
                //TODO Splashscreen showing FAIL
            }
            try {
                ContentValues newVals = new ContentValues();
                JSONArray genres = response.getJSONArray("genres");
                for (int count = 0; count < genres.length(); count++) {
                    JSONObject genreobj = genres.getJSONObject(count);
                    JSONObject results = genreobj.getJSONObject("results");
                    booktype = results.getString("list_name"); //GET GENRE STRING
                    // txtResultsNYT.append("No. Results: " + genreobj.getInt("num_results") + "\n"); //GET NO. RESULTS
                    JSONArray nytbooks = results.getJSONArray("books");

                    for (int i = 0; i < nytbooks.length(); i++) {
                        JSONObject bookobj = nytbooks.getJSONObject(i);
                        booktitle = bookobj.getString("title");
                        newVals.put(KEY_TITLE_COLUMN, booktitle);
                        bookisbn = bookobj.getString("primary_isbn13");
                        newVals.put(KEY_ISBN_COLUMN, bookisbn);
                        bookauthor = bookobj.getString("author");
                        newVals.put(KEY_AUTHOR_COLUMN, bookauthor);
                        bookdesc = bookobj.getString("description");
                        newVals.put(KEY_DESCRIPTION_COLUMN, bookdesc);
                        bookcoverURLstr = bookobj.getString("book_image");
                        newVals.put(KEY_COVER_COLUMN, bookcoverURLstr);
                        // TODO: bookreview = GoogleApi -> Get Review via ISBN
                        // TODO: bookpagecount = GoogleApi -> Get page count via ISBN
                    }
                    SQLiteDatabase db = getWritableDatabase();
                    db.insert(DATABASE_TABLE_BOOKS, null, newVals);
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            catch (Exception e) {
                Log.e("Error", e.getMessage(), e);
            }
        }
    }

    public AugmentedImageDatabase getImageDatabase(Session session) {
        AugmentedImageDatabase augmentedImageDatabase = new AugmentedImageDatabase(session);
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
            augmentedImageDatabase.addImage(title, bookcover); //TODO: URL to Bitmap!
        }
        cursor.close();
        return augmentedImageDatabase;
    }
}
