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

public class AugmentedImageDatabaseHelper extends SQLiteOpenHelper {

    //////////////////////////////////////////////////////////////////////////
    // API STRINGS ////////////////////////////////////////////////////////////////////////////////
    private static final String NYT_API_URL = "https://api.nytimes.com/svc/books/v3/lists/current/";
    private final String[] NYT_API_URL_LISTS = {"hardcover-fiction", "hardcover-nonfiction",
            "young-adult", "humor",
            "advice-how-to-and-miscellaneous",
            "picture-books", "education"};
    private static final String NYT_API_APPENDIX = ".json?api-key=";
    private static final String NYT_API_KEY = "nswaoVUNKJ0N6YROAtnuls7nNHBGcs8G";

    private static final String GOOGLE_API_URL = "https://www.googleapis.com/books/v1/volumes?q=";
    private static final String GOOGLE_API_APPENDIX = "&key=";
    private static final String GOOGLE_API_KEY = "AIzaSyCzHJ0nqPFUyrJqxC7vFU_IhUKTvKDcXIM";
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
    private String bookreview = "-1"; //Not always available
    private String booktype;
    private String bookpagecount = "-1"; //Not always available
    private String bookcoverURLstr;
    private URL bookcoverURL;
    private Bitmap bookcover;
    ///////////////////////////////////
    //TODO: Local variables pls x

    /////////////////////////////////////////////////////
    // MISC ////////////////////////////////////////////////////////////
    private StringBuilder stringBuilderNYT = new StringBuilder();
    private StringBuilder stringBuilderGoogle = new StringBuilder();
    private URL nytapiurl;
    private URL googleapiurl;
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
        if (run) new RetrieveData().execute(session);
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
        JSONObject jsonObjectNYT;
        protected void onPreExecute() {
            //TODO: SPLASH LOADING SCREEN!
        }

        protected Void doInBackground(Session... params) {
            if(!databaseHasEntries()) {
                try {
                    stringBuilderNYT.append("{\n\"genres\":[\n");
                    for (int i = 0, len = NYT_API_URL_LISTS.length; i < len; ++i) {
                        nytapiurl = new URL(NYT_API_URL + NYT_API_URL_LISTS[i] + NYT_API_APPENDIX + NYT_API_KEY);
                        HttpURLConnection urlConnection = (HttpURLConnection) nytapiurl.openConnection();
                        try {
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                stringBuilderNYT.append(line).append("\n");
                            }
                            if (i != len - 1) stringBuilderNYT.append(",\n");
                            bufferedReader.close();
                        } finally {
                            urlConnection.disconnect();
                        }
                    }
                    stringBuilderNYT.append("]\n}");
                    json = stringBuilderNYT.toString();
                    jsonObjectNYT = new JSONObject(json);
                } catch (Exception e) {
                    Log.e("Error", e.getMessage(), e);
                    return null;
                }
                AddToDatabase(jsonObjectNYT);
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
                Log.e("Error:", e.getMessage(),e);
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
    public AugmentedImageDatabase getAugmentedImageDatabase() { return augmentedImageDatabase; }
    ///////////////////////////////////////////////////////////////

    public boolean getIsFilled() { return filled; }

    private boolean databaseHasEntries() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor dbcursor = db.rawQuery("SELECT * FROM " + DATABASE_TABLE_BOOKS, null);
        dbhasentries = dbcursor.moveToFirst();
        dbcursor.close();
        return dbhasentries;
    }

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

    ///////////////////////////////////////////////////////////////////////////
    // ADDS ENTRIES INTO SQLITE DATABASE /////////////////////////////////////////////////////
    private void AddToDatabase(JSONObject response) {
        try {
            ContentValues newVals = new ContentValues();
            JSONArray genres = response.getJSONArray("genres");
            JSONObject jsonObjectGoogle;
            for (int count = 0; count < genres.length(); count++) {
                JSONObject genreobj = genres.getJSONObject(count);
                JSONObject results = genreobj.getJSONObject("results");
                booktype = results.getString("list_name"); //Get Genre as Str

                JSONArray nytbooks = results.getJSONArray("books");

                for (int i = 0; i < nytbooks.length(); i++) {
                    JSONObject bookobj = nytbooks.getJSONObject(i);
                    booktitle = bookobj.getString("title");
                    Log.i(TAG, "[!] Book Title: " + booktitle.replaceAll(" ", "+") + "[!]");
                    bookisbn = bookobj.getString("primary_isbn13");
                    bookauthor = bookobj.getString("author");
                    bookdesc = bookobj.getString("description");
                    bookcoverURLstr = bookobj.getString("book_image");
                    googleapiurl = new URL(GOOGLE_API_URL + booktitle.replaceAll(" ","+") + "+" + bookauthor.replaceAll(" ","+") + GOOGLE_API_APPENDIX + GOOGLE_API_KEY);
                    Log.i(TAG,"[!] APIURL: " + googleapiurl.toString());
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
                    int googleItemsLength = googleItems.length();
                    int index = 0;
                    boolean resultFound = true;
                    JSONObject result = googleItems.getJSONObject(index);

                    while(!result.getJSONObject("volumeInfo").getString("title").equalsIgnoreCase(booktitle)) {
                        try { result = googleItems.getJSONObject(++index); }
                        catch(Exception e) { resultFound = false; break; }
                        Log.i(TAG,"IN WHILE LOOP");
                    }

                    if(resultFound) {
                        JSONObject volumeInfo = result.getJSONObject("volumeInfo");
                        Log.i(TAG, "Title from GGL: " + volumeInfo.getString("title"));
                        if (volumeInfo.has("pageCount")) bookpagecount = volumeInfo.getString("pageCount");
                        if (volumeInfo.has("averageRating")) bookreview = volumeInfo.getString("averageRating");
                    }
                    newVals.put(KEY_TYPE_COLUMN, booktype);
                    newVals.put(KEY_TITLE_COLUMN, booktitle);
                    newVals.put(KEY_ISBN_COLUMN, bookisbn);
                    newVals.put(KEY_AUTHOR_COLUMN, bookauthor);
                    newVals.put(KEY_DESCRIPTION_COLUMN, bookdesc);
                    newVals.put(KEY_COVER_COLUMN, bookcoverURLstr);
                    newVals.put(KEY_REVIEW_COLUMN, bookreview);
                    newVals.put(KEY_PAGE_COUNT_COLUMN, bookpagecount);
                    SQLiteDatabase db = this.getWritableDatabase();
                    db.insert(DATABASE_TABLE_BOOKS, null, newVals);
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
}
