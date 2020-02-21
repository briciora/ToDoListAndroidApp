package com.csce4623.ahnelson.todolist;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class ConnectivityBroadcastReceiver extends BroadcastReceiver {

    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;


    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public void insertNewNote(final Context context, String title, String content, String date)
    {
        //Create a ContentValues object
        ContentValues myCV = new ContentValues();
        //Put key_value pairs based on the column names, and the values
        myCV.put(ToDoProvider.TODO_TABLE_COL_TITLE, title);
        myCV.put(ToDoProvider.TODO_TABLE_COL_CONTENT, content);
        myCV.put(ToDoProvider.TODO_TABLE_COL_DATE, date);
        //Perform the insert function using the ContentProvider
        context.getContentResolver().insert(ToDoProvider.CONTENT_URI, myCV);
    }

    public void updateNewNote(final Context context, String id, String title, String content, String date, String checked)
    {
        int currentID = Integer.parseInt(id);
        //Create a ContentValues object
        ContentValues myCV = new ContentValues();
        //Put key_value pairs based on the column names, and the values
        myCV.put(ToDoProvider.TODO_TABLE_COL_TITLE, title);
        myCV.put(ToDoProvider.TODO_TABLE_COL_CONTENT, content);
        myCV.put(ToDoProvider.TODO_TABLE_COL_DATE, date);
        if(checked.equals("true"))
        {
            myCV.put(ToDoProvider.TODO_TABLE_COL_COMPLETED, true);
        }
        else
        {
            myCV.put(ToDoProvider.TODO_TABLE_COL_COMPLETED, false);
        }
        //Perform the insert function using the ContentProvider
        context.getContentResolver().update(Uri.parse(ToDoProvider.CONTENT_URI + "/" + currentID), myCV, null, null);
    }

    public void deleteNote(final Context context, String id)
    {
        int currentID = Integer.parseInt(id);
        //delete from Content Provider
        context.getContentResolver().delete(Uri.parse(ToDoProvider.CONTENT_URI + "/" + currentID), null, null);
    }

    public void updateDB(final Context context) {
        File fileEvents = new File(context.getFilesDir() + "/text/sample");
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileEvents));
            String line = br.readLine();
            while (line != null) {
                switch (line) {
                    case "insert":
                        String title = br.readLine();
                        String content = br.readLine();
                        String date = br.readLine();
                        insertNewNote(context, title, content, date);
                        break;
                    case "update":
                        String idU = br.readLine();
                        String titleU = br.readLine();
                        String contentU = br.readLine();
                        String dateU = br.readLine();
                        String checked = br.readLine();
                        updateNewNote(context, idU, titleU, contentU, dateU, checked);
                        break;
                    case "delete":
                        String id = br.readLine();
                        deleteNote(context, id);
                        break;
                    default:
                        break;
                }
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
        }
    }

    public void eraseFileContents(Context context)
    {
        File file = new File(context.getFilesDir(), "text");
        if (!file.exists()) {
            file.mkdir();
        }
        try {
            File dbUpdate = new File(file, "sample");
            FileWriter writer = new FileWriter(dbUpdate, false);
            writer.write("");
            writer.flush();
            writer.close();
        }
        catch (Exception e) {
        }
    }


    @Override
    public void onReceive(final Context context, final Intent intent) {

        int status = getConnectivityStatus(context);

        if(status == 0) {
            Toast.makeText(context, "You are not connected!", Toast.LENGTH_LONG).show();
        }

        if(status == 1) {
            Toast.makeText(context, "You have WIFI connection!", Toast.LENGTH_LONG).show();
            updateDB(context);
            eraseFileContents(context);
        }

        if(status == 2) {
            Toast.makeText(context, "You have mobile connection!", Toast.LENGTH_LONG).show();
            updateDB(context);
            eraseFileContents(context);
        }

    }
}
