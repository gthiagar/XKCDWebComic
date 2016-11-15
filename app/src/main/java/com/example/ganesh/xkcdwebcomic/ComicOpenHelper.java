package com.example.ganesh.xkcdwebcomic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Log;
import android.view.ViewDebug;
import android.widget.ImageView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Ganesh on 10/11/2016.
 */
public class ComicOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "comic1.db";
    private static final String DATABASE_PATH = "/data/data/com.example.ganesh.xkcdwebcomic/database";
    private static final String TABLE_NAME = "comic1";
    private static final String IMAGE_COLUMN = "image";
    private static final String ID_COLUMN = "_id";
    private static final String TITLE_COLUMN = "title";
    private static final String COMIC_TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + "(" +
                    ID_COLUMN + " INTEGER, " +
                    TITLE_COLUMN + " TEXT," +
                    IMAGE_COLUMN + " BLOB)";
    private SQLiteDatabase db;
    private static Context context = null;
    ComicOpenHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        // onCreate is called after getWritableDatabase() is called
        db = getWritableDatabase();
    }
    private static final String COMIC_TABLE_DELETE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    private static final String COMIC_QUERY_DATA = "SELECT * FROM " + TABLE_NAME ;

    public void createDataBase() throws IOException{
        boolean doesDataBaseExist  = checkDataBase();
        if(!doesDataBaseExist){
            try{
                copyDataBase();
            }
            catch(IOException ioException){
                throw new Error("ERROR In Copying Database");
            }
        }
    }

    // Checks if the database exists
    private boolean checkDataBase(){
        return new File(DATABASE_PATH + DATABASE_NAME).exists();
    }

    // Copies database from assets folder
    private void copyDataBase() throws IOException{
        InputStream inputStream = context.getAssets().open(DATABASE_NAME);
        OutputStream outputStream = new FileOutputStream(DATABASE_PATH + DATABASE_NAME);

        // Copy to file
        byte[] buffer = new byte[200000000];
        int nRead;
        while((nRead = inputStream.read(buffer)) != -1){
            outputStream.write(buffer, 0, nRead);
        }

        // Close streams
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        System.out.println("Constructor");
        db.execSQL(COMIC_TABLE_CREATE);
        FetchComicNumTask fetchComicNumTask = new FetchComicNumTask();
        try {
            // Get the max comic number from api
            int maxComicNum = 200;//fetchComicNumTask.execute().get().intValue();
            for (int i = 1; i <= maxComicNum; i++) {
                // Get the title and images and store it in a database
                FetchTitleTask fetchTitleTask = new FetchTitleTask();
                FetchImageURLTask fetchImageURLTask = new FetchImageURLTask();
                String comicNum = new Integer(i).toString();
                String imageURLString = fetchImageURLTask.execute(comicNum).get();
                FetchImageTask fetchImageTask = new FetchImageTask(imageURLString);

                byte [] base64Result = fetchImageTask.execute().get();
                // Get Data from api
                ContentValues contentValues = new ContentValues();
                contentValues.put(ID_COLUMN, comicNum);
                contentValues.put(TITLE_COLUMN, fetchTitleTask.execute(comicNum).get());
                contentValues.put(IMAGE_COLUMN, base64Result);
                // Insert into database
                db.insert(TABLE_NAME, null, contentValues);
            }
        }
        catch(Exception e){

        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // Delete old one and make new one
        db.execSQL(COMIC_TABLE_DELETE);
        onCreate(db);
    }

    public Cursor getData(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(COMIC_QUERY_DATA, null);
        return cursor;
    }

    class FetchImageTask extends AsyncTask<Void, Void, byte[]>{

        private final String LOG_TAG = FetchImageTask.class.getSimpleName();
        private String imageURLString;

        public FetchImageTask(String str){
            imageURLString = str;
        }

        @Override
        protected byte [] doInBackground(Void... voids) {

            BufferedReader imageReader = null;
            HttpURLConnection imageConnection = null;
            InputStream imageStream = null;
            byte [] base64Array = new byte[16384];
            //Bitmap bitmap = null;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try{
                URL imageURL = new URL(imageURLString);
                imageConnection = (HttpURLConnection) imageURL.openConnection();
                imageConnection.setRequestMethod("GET");
                imageConnection.connect();
                imageStream = imageConnection.getInputStream();
                if(imageStream == null){
                    return null;
                }

                int readBytes;
                while((readBytes = imageStream.read(base64Array, 0, base64Array.length)) != -1){
                    output.write(base64Array, 0, readBytes);
                }
                output.flush();
            }
            catch(Exception e){
                Log.e(LOG_TAG, "ERROR: " + e.getMessage());
            }
            finally{
                if (imageConnection != null) {
                    imageConnection.disconnect();
                }
                if (imageReader != null) {
                    try {
                        imageReader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return output.toByteArray(); //Base64.encode(output.toByteArray(), 0);
        }


        @Override
        protected void onPostExecute(byte[] bytes) {
            /*if(imageViewWeakReference != null && bitmap != null) {
                final ImageView imageView = imageViewWeakReference.get();
                if(imageView != null){
                    imageView.setImageBitmap(bitmap);
                }
            }*/
        }
    }

    class FetchImageURLTask extends AsyncTask<String, Void, String> {

        private final String LOG_TAG = FetchImageURLTask.class.getSimpleName();

        @Override
        protected String doInBackground(String... strings) {
            String comicNum = strings[0];
            final String urlBase = "http://xkcd.com/" + comicNum + "/info.0.json";
            HttpURLConnection httpURLConnection = null;
            StringBuffer buffer = null;
            BufferedReader reader = null;
            InputStream inputStream = null;
            String imageURLString = null;
            try {
                URL url = new URL(urlBase);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();
                inputStream = httpURLConnection.getInputStream();
                buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                String jsonString = buffer.toString();
                JSONObject jsonComic = new JSONObject(jsonString);
                imageURLString = jsonComic.getString("img");
            } catch (Exception e) {
                Log.e(LOG_TAG, "ERROR: " + e.getMessage());
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return imageURLString;
        }
    }


    class FetchTitleTask extends AsyncTask<String, Void, String> {

        private final String LOG_TAG = FetchTitleTask.class.getSimpleName();

        @Override
        protected String doInBackground(String... strings) {
            String comicNum = strings[0];
            final String urlBase = "http://xkcd.com/" + comicNum + "/info.0.json";
            HttpURLConnection httpURLConnection = null;
            StringBuffer buffer = null;
            BufferedReader reader = null;
            InputStream inputStream = null;
            String titleString = null;
            try {
                URL url = new URL(urlBase);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();
                inputStream = httpURLConnection.getInputStream();
                buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                String jsonString = buffer.toString();
                JSONObject jsonComic = new JSONObject(jsonString);
                titleString = jsonComic.getString("title");
            } catch (Exception e) {
                Log.e(LOG_TAG, "ERROR: " + e.getMessage());
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return titleString;
        }
    }


    class FetchComicNumTask extends AsyncTask<Void, Void, Integer> {

        private final String LOG_TAG = FetchComicNumTask.class.getSimpleName();

        @Override
        protected Integer doInBackground(Void... voids) {
            final String urlBase = "http://xkcd.com/info.0.json";
            HttpURLConnection httpURLConnection = null;
            StringBuffer buffer = null;
            BufferedReader reader = null;
            InputStream inputStream = null;
            Integer comicNum = null;
            try {
                URL url = new URL(urlBase);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();
                inputStream = httpURLConnection.getInputStream();
                buffer = new StringBuffer();
                if (inputStream == null) {
                    return comicNum;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return comicNum;
                }
                String jsonString = buffer.toString();
                JSONObject jsonComic = new JSONObject(jsonString);
                comicNum = jsonComic.getInt("num");
            } catch (Exception e) {
                Log.e(LOG_TAG, "ERROR: " + e.getMessage());
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return comicNum;
        }
    }

}
