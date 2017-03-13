package com.example.ganesh.xkcdwebcomic;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String DATABASE_NAME = "comic8";
    private ComicOpenHelper sqLiteOpenHelper;
    private SQLiteDatabase sqLiteDatabase;
    private String query;
    private Cursor cursor;
    private final ArrayList<String> listOfIds  = new ArrayList<String>();;
    private final ArrayList<byte []> listOfImages = new ArrayList<byte []>();
    private ArrayAdapter<String> adapter;
    int lower = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Open Data base and query everything
            ComicOpenHelper sqLiteOpenHelper = new ComicOpenHelper(MainActivity.this);
            // Try to create database
            sqLiteOpenHelper.createDataBase();
            SQLiteDatabase sqLiteDatabase = sqLiteOpenHelper.getWritableDatabase();
            String query = "SELECT * FROM " + DATABASE_NAME; // + ";";
            Cursor cursor = sqLiteDatabase.rawQuery(query + " WHERE _id<=" + "'" + new Integer(lower).toString() + "'" , null);
            if(cursor == null){
                System.out.println("Cursor is NULL");
            }
            // Put everything into an adapter and attach that to the view
            while(cursor.moveToNext()){
                String body = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                body += " " + cursor.getString(cursor.getColumnIndexOrThrow("title"));
                listOfIds.add(body);
                byte [] imageString = cursor.getBlob(cursor.getColumnIndexOrThrow("image"));
                listOfImages.add(imageString);
            }
            adapter = new ArrayAdapter<String>(this,  R.layout.activity_listview, listOfIds);
            ListView listView = (ListView) findViewById(R.id.test);

            listView.setAdapter(adapter);
            listView.setClickable(true);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                    Intent intent = new Intent(MainActivity.this, ItemActivity.class);
                    intent.putExtra("image", listOfImages.get(position));
                    startActivity(intent);
                }
            });


            listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                boolean loadingFlag = false;
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll (AbsListView absListView, int first, int visible, int total) {
                    final int lastItem = first + visible;
                    System.out.println("lastItem" + lastItem);
                    System.out.println("TOTAL" + total);
                    if(lastItem == total  && total != 0 && !loadingFlag){
                        loadingFlag = true;
                        loadingFlag = addNewItems();
                    }

                }
            });
        }
        catch(Exception e){

        }
    }

    // Return false and pass it to the loadingFlag
    public boolean addNewItems(){
        System.out.println("ADD ITEMS");
        sqLiteOpenHelper = new ComicOpenHelper(MainActivity.this);
        sqLiteDatabase = sqLiteOpenHelper.getWritableDatabase();
        query = "SELECT * FROM " + DATABASE_NAME;
        Cursor cursor = sqLiteDatabase.
                rawQuery(query + " WHERE _id>" + "'" + new Integer(lower).toString() + "' " + "AND _id<=" + "'" + new Integer(lower+20).toString() + "'", null);
        System.out.println("ADD ITEMS");
        lower += 20;
        while(cursor.moveToNext()){
            String body = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
            body += " " + cursor.getString(cursor.getColumnIndexOrThrow("title"));
            listOfIds.add(body);
            byte [] imageString = cursor.getBlob(cursor.getColumnIndexOrThrow("image"));
            listOfImages.add(imageString);
            //adapter.add(body);
        }
        adapter.notifyDataSetChanged();
        return false;
    }


}
