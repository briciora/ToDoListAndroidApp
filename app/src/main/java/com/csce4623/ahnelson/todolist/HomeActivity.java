package com.csce4623.ahnelson.todolist;

import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.CheckBox;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.os.Build;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.io.FileWriter;
import android.content.Context;


//Create HomeActivity and implement the OnClick listener
public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    boolean checked = false;

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "dateReminder";
            String description = "reminds the user when a due date has come";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("101", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //register broadcast receiver
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        this.registerReceiver(new ConnectivityBroadcastReceiver(), intentFilter);

        setContentView(R.layout.activity_home);
        initializeComponents();

        createNotificationChannel();

        //create text file to write to if connection is lost
        File file = new File(HomeActivity.this.getFilesDir(), "text");
        if (!file.exists()) {
            file.mkdir();
        }
        try {
            File dbUpdate = new File(file, "sample");
            FileWriter writer = new FileWriter(dbUpdate);
            writer.flush();
            writer.close();
            //Toast.makeText(HomeActivity.this, "Saved your text", Toast.LENGTH_LONG).show();
        } catch (Exception e) { }
    }

    void editNote(int noteIDCurrentPos, String noteTitleCurrentPos, String noteContentCurrentPos, String dateContentCurrentPos) {
        setContentView(R.layout.edit_activity);
        findViewById(R.id.btnSaveChanges).setOnClickListener(this);
        findViewById(R.id.btnDeleteNote).setOnClickListener(this);

        EditText noteTitle = findViewById(R.id.etNoteTitle);
        EditText noteContent = findViewById(R.id.etNoteContent);
        if (!noteContentCurrentPos.isEmpty()) {
            noteTitle.setText(noteTitleCurrentPos);
        }
        if (!noteTitleCurrentPos.isEmpty()) {
            noteContent.setText(noteContentCurrentPos);
        }

        TextView ID = findViewById(R.id.tvID);
        ID.setText(String.valueOf(noteIDCurrentPos));
        EditText date = findViewById(R.id.etDatePicker);
        if(dateContentCurrentPos == null)
        {
            date.setText("");
        }
        else {
            date.setText(dateContentCurrentPos);
        }

        if(checked) {
            CheckBox checkBox = findViewById(R.id.cbCompleted);
            checkBox.setChecked(!checkBox.isChecked());
        }
    }

    public void onCheckboxClicked(View v) {
        checked = ((CheckBox) v).isChecked();
    }

    void saveChanges(View view) {
        TextView ID = findViewById(R.id.tvID);
        int currentID = Integer.parseInt(ID.getText().toString());

        //grab the text from the strings and put into strings
        EditText noteTitle = findViewById(R.id.etNoteTitle);
        String title = noteTitle.getText().toString();
        EditText noteContent = findViewById(R.id.etNoteContent);
        String content = noteContent.getText().toString();
        EditText dateET = findViewById(R.id.etDatePicker);
        String date = dateET.getText().toString();

        if (title.isEmpty() && title.isEmpty()) {
            setContentView(R.layout.activity_home);
            initializeComponents();
            onStart();
            return;
        }
        if (title.isEmpty()) {
            title = "";
        }
        if (content.isEmpty()) {
            content = "";
        }

        String checkedOffline = "false";

        //Create a ContentValues object
        ContentValues myCV = new ContentValues();
        //Put key_value pairs based on the column names, and the values
        myCV.put(ToDoProvider.TODO_TABLE_COL_TITLE, title);
        myCV.put(ToDoProvider.TODO_TABLE_COL_CONTENT, content);
        if(checked)
        {
            myCV.put(ToDoProvider.TODO_TABLE_COL_COMPLETED, true);
            checkedOffline = "true";
        }
        myCV.put(ToDoProvider.TODO_TABLE_COL_DATE, date);
        //Update the note
        int didWork = getContentResolver().update(Uri.parse(ToDoProvider.CONTENT_URI + "/" + currentID), myCV, null, null);
        //If deleted, didWork returns the number of rows deleted (should be 1)
        if (didWork == 1) {
            //If it didWork, then create a Toast Message saying that the note was updated
            Toast.makeText(getApplicationContext(), "Updated Note " + currentID, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "No Note to update!", Toast.LENGTH_LONG).show();
        }

        if(!isNetworkAvailable()) {
            File file = new File(HomeActivity.this.getFilesDir(), "text");
            if (!file.exists()) {
                file.mkdir();
            }
            try {
                File dbUpdate = new File(file, "sample");
                FileWriter writer = new FileWriter(dbUpdate, true);
                writer.write("update" + "\n");
                writer.write(currentID + "\n");
                writer.write(title + "\n");
                writer.write(content + "\n");
                writer.write(date + "\n");
                writer.write(checkedOffline + "\n");
                writer.flush();
                writer.close();
                //Toast.makeText(HomeActivity.this, "Saved your text", Toast.LENGTH_LONG).show();
            }
            catch (Exception e) {
            }
        }

        createNotification(date, title, currentID);

        //back to home
        setContentView(R.layout.activity_home);
        initializeComponents();
        onStart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Find ListView to populate
        final ListView lvItems = (ListView) findViewById(R.id.listOfTasks);

        String[] projection = {
                "_ID AS " + ToDoProvider.TODO_TABLE_COL_ID,
                ToDoProvider.TODO_TABLE_COL_TITLE,
                ToDoProvider.TODO_TABLE_COL_CONTENT,
                ToDoProvider.TODO_TABLE_COL_DATE};
        //Perform a query to get all rows in the DB
        final Cursor toDoCursor = getContentResolver().query(ToDoProvider.CONTENT_URI, projection,
                null, null, null);

        // Setup cursor adapter using cursor from last step
        final ToDoCursorAdapter toDoAdapter = new ToDoCursorAdapter(this, toDoCursor);
        // Attach cursor adapter to the ListView
        lvItems.setAdapter(toDoAdapter);
        toDoAdapter.changeCursor(toDoCursor);

        //be able to click items in LV
        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                toDoCursor.moveToPosition(position);
                //Toast.makeText(getApplicationContext(), toDoCursor.getString(2), Toast.LENGTH_LONG).show();
                editNote(toDoCursor.getInt(0), toDoCursor.getString(1),
                        toDoCursor.getString(2), toDoCursor.getString(3));
            }
        });
    }

    //Set the OnClick Listener for buttons
    void initializeComponents() {
        findViewById(R.id.btnNewNote).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //If new Note, call createNewNote()
            case R.id.btnNewNote:
                createNewNote();
                break;
            //If delete note, call deleteNewestNote()
            case R.id.btnDeleteNote:
                deleteNote();
                break;
            case R.id.btnSave:
                saveNewNote();
                break;
            case R.id.btnSaveChanges:
                saveChanges(v);
                break;
            //This shouldn't happen
            default:
                break;
        }
    }

    void saveNewNote() {
        //grab the text from the strings and put into strings
        EditText noteTitle = findViewById(R.id.etNoteTitle);
        String title = noteTitle.getText().toString();
        EditText noteContent = findViewById(R.id.etNoteContent);
        String content = noteContent.getText().toString();
        EditText noteDate = findViewById(R.id.etDatePicker);
        String date = noteDate.getText().toString();

        if (title.isEmpty() && title.isEmpty()) {
            setContentView(R.layout.activity_home);
            initializeComponents();
            onStart();
            return;
        }
        if (title.isEmpty()) {
            title = "";
        }
        if (content.isEmpty()) {
            content = "";
        }


        //Create a ContentValues object
        ContentValues myCV = new ContentValues();
        //Put key_value pairs based on the column names, and the values
        myCV.put(ToDoProvider.TODO_TABLE_COL_TITLE, title);
        myCV.put(ToDoProvider.TODO_TABLE_COL_CONTENT, content);
        myCV.put(ToDoProvider.TODO_TABLE_COL_DATE, date);
        //Perform the insert function using the ContentProvider
        getContentResolver().insert(ToDoProvider.CONTENT_URI, myCV);

        String[] projection = {
                "_ID AS " + ToDoProvider.TODO_TABLE_COL_ID,
                ToDoProvider.TODO_TABLE_COL_TITLE,
                ToDoProvider.TODO_TABLE_COL_CONTENT,
                ToDoProvider.TODO_TABLE_COL_DATE};
        //Perform a query to get all rows in the DB
        final Cursor toDoCursor = getContentResolver().query(ToDoProvider.CONTENT_URI, projection,
                null, null, null);
        toDoCursor.moveToLast();
        int id = toDoCursor.getPosition();

        //create reminder for data
        createNotification(date, title, id);

        if(!isNetworkAvailable()) {
            File file = new File(HomeActivity.this.getFilesDir(), "text");
            if (!file.exists()) {
                file.mkdir();
            }
            try {
                File dbUpdate = new File(file, "sample");
                FileWriter writer = new FileWriter(dbUpdate, true);
                writer.write("insert \n");
                writer.write(title + "\n");
                writer.write(content + "\n");
                writer.write(date + "\n");
                writer.flush();
                writer.close();
                //Toast.makeText(HomeActivity.this, "Saved your text", Toast.LENGTH_LONG).show();
            }
            catch (Exception e) {
            }
        }
        setContentView(R.layout.activity_home);
        initializeComponents();
        onStart();

    }

    //Create a new note with the title "New Note" and content "Note Content"
    void createNewNote() {
        setContentView(R.layout.note_activity);
        findViewById(R.id.btnSave).setOnClickListener(this);

        //Set the projection for the columns to be returned
        String[] projection = {
                ToDoProvider.TODO_TABLE_COL_ID,
                ToDoProvider.TODO_TABLE_COL_TITLE,
                ToDoProvider.TODO_TABLE_COL_CONTENT,
                ToDoProvider.TODO_TABLE_COL_DATE};
        //Perform a query to get all rows in the DB
        Cursor myCursor = getContentResolver().query(ToDoProvider.CONTENT_URI, projection, null, null, null);
        //Create a toast message which states the number of rows currently in the database
        Toast.makeText(getApplicationContext(), Integer.toString(myCursor.getCount()), Toast.LENGTH_LONG).show();
    }

    //Delete the note selected
    void deleteNote() {
        //get current ID
        TextView ID = findViewById(R.id.tvID);
        int currentID = Integer.parseInt(ID.getText().toString());

        //delete note of ID
        int didWork = getContentResolver().delete(Uri.parse(ToDoProvider.CONTENT_URI + "/" + currentID), null, null);
        //If deleted, didWork returns the number of rows deleted (should be 1)
        if (didWork == 1) {
            //If it didWork, then create a Toast Message saying that the note was deleted
            Toast.makeText(getApplicationContext(), "Deleted Note " + currentID, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "No Note to delete!", Toast.LENGTH_LONG).show();

        }

        if (!isNetworkAvailable()) {
            File file = new File(HomeActivity.this.getFilesDir(), "text");
            if (!file.exists()) {
                file.mkdir();
            }
            try {
                File dbUpdate = new File(file, "sample");
                FileWriter writer = new FileWriter(dbUpdate, true);
                writer.write("delete \n");
                writer.write(currentID + "\n");
                writer.flush();
                writer.close();
                //Toast.makeText(HomeActivity.this, "Saved your text", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
            }

            setContentView(R.layout.activity_home);
            initializeComponents();
            onStart();
        }
    }

    void createNotification(String date, String title, int id)
    {
        if(date == null)
        {
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy hh:mm a");
            Date dateOb = sdf.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateOb);
            NotificationScheduler.setReminder(this, AlarmReceiver.class, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_WEEK), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), title, id);
        }
        catch(java.text.ParseException e){
            e.printStackTrace();
        }

    }

    public void clickBack(View v)
    {
        //back to home
        setContentView(R.layout.activity_home);
        initializeComponents();
        onStart();
    }
}
