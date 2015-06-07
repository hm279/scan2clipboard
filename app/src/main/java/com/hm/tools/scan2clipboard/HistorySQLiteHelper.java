package com.hm.tools.scan2clipboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by hm on 15-5-30.
 */
public class HistorySQLiteHelper extends SQLiteOpenHelper {
    private static final String HISTORY_DB = "history.db";
    private static final int VERSION = 1;

    private static final String mHistoryTableName = "history";
    public static final String column_text = "text";

    private static final String CREATE_TABLE ="CREATE TABLE IF NOT EXISTS " + mHistoryTableName + "("
            + column_text + ");";
    private static final String query_all = "select rowid, * from " + mHistoryTableName
            + " ORDER BY rowid DESC";

    private static HistorySQLiteHelper helper;
    public static HistorySQLiteHelper getInstance(Context context) {
        if (helper == null ) {
            helper = new HistorySQLiteHelper(context.getApplicationContext());
        }
        return helper;
    }

    public HistorySQLiteHelper(Context context) {
        super(context, HISTORY_DB, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + mHistoryTableName);
        onCreate(db);
    }

    public ArrayList<Long> insert(ArrayList<String> strings) {
        ArrayList<Long> longs = new ArrayList<>();
        SQLiteDatabase database = getWritableDatabase();
        for (String text : strings) {
            if (text == null || text.length() < 1) {
                longs.add((long) -1);
            } else {
                ContentValues values = new ContentValues();
                values.put(column_text, text);
                long id = database.insert(mHistoryTableName, null, values);
                longs.add(id);
            }
        }
        return longs;
    }

    public Cursor query() {
        return getReadableDatabase().rawQuery(query_all, null, null);
    }

    public int delete(long rowid) {
        SQLiteDatabase database = getWritableDatabase();
        if (rowid == -1) {
            //delete add records
            return database.delete(mHistoryTableName, null, null);
        } else {
            return database.delete(mHistoryTableName, "rowid=?", new String[]{Long.toString(rowid)});
        }
    }

    public long update(long rowid, String text) {
        ContentValues values = new ContentValues();
        values.put(column_text, text);
        return getWritableDatabase()
                .update(mHistoryTableName, values, "rowid=?", new String[]{Long.toString(rowid)});
    }

}
