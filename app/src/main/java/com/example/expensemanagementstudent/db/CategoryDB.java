package com.example.expensemanagementstudent.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class CategoryDB {
    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;

    public CategoryDB(Context context) {
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public Cursor getCategoryByName(String categoryName) {
        return db.query(
                DatabaseHelper.CATEGORY_TABLE,
                null,
                DatabaseHelper.CATEGORY_NAME_COL + " = ?",
                new String[]{categoryName},
                null,
                null,
                null
        );
    }

    @SuppressLint("Range")
    public ArrayList<String> getCategoryNamesByType(int type) {
        ArrayList<String> categoryNames = new ArrayList<>();
        Cursor cursor = db.query(
                DatabaseHelper.CATEGORY_TABLE,
                new String[]{DatabaseHelper.CATEGORY_NAME_COL},
                DatabaseHelper.CATEGORY_TYPE_COL + " = ?",
                new String[]{String.valueOf(type)},
                null,
                null,
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                categoryNames.add(cursor.getString(cursor.getColumnIndex(DatabaseHelper.CATEGORY_NAME_COL)));
            }
            cursor.close();
        }
        return categoryNames;
    }

    public long addCategory(String name, String icon, int type) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.CATEGORY_NAME_COL, name);
        values.put(DatabaseHelper.CATEGORY_ICON_COL, icon);
        values.put(DatabaseHelper.CATEGORY_TYPE_COL, type);

        return db.insert(DatabaseHelper.CATEGORY_TABLE, null, values);
    }

    public Cursor getAllCategories() {
        return db.query(
                DatabaseHelper.CATEGORY_TABLE,
                new String[]{DatabaseHelper.CATEGORY_ID_COL, DatabaseHelper.CATEGORY_NAME_COL, DatabaseHelper.CATEGORY_ICON_COL, DatabaseHelper.CATEGORY_TYPE_COL},
                null,
                null,
                null,
                null,
                null
        );
    }

    @SuppressLint("Range")
    public ArrayList<String> getCategoryNames() {
        ArrayList<String> categoryNames = new ArrayList<>();
        Cursor cursor = getAllCategories();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                categoryNames.add(cursor.getString(cursor.getColumnIndex(DatabaseHelper.CATEGORY_NAME_COL)));
            }
            cursor.close();
        }

        return categoryNames;
    }

    public int getCategoryId(String categoryName) {
        Cursor cursor = db.query(
                DatabaseHelper.CATEGORY_TABLE,
                new String[]{DatabaseHelper.CATEGORY_ID_COL},
                DatabaseHelper.CATEGORY_NAME_COL + " = ?",
                new String[]{categoryName},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range")
            int categoryId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.CATEGORY_ID_COL));
            cursor.close();
            return categoryId;
        }

        if (cursor != null) {
            cursor.close();
        }
        return -1;
    }

    public boolean updateCategory(long id, String name, String icon, int type) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.CATEGORY_NAME_COL, name);
        values.put(DatabaseHelper.CATEGORY_ICON_COL, icon);
        values.put(DatabaseHelper.CATEGORY_TYPE_COL, type);

        return db.update(
                DatabaseHelper.CATEGORY_TABLE,
                values,
                DatabaseHelper.CATEGORY_ID_COL + " = ?",
                new String[]{String.valueOf(id)}
        ) > 0;
    }

    public boolean deleteCategory(long id) {
        return db.delete(
                DatabaseHelper.CATEGORY_TABLE,
                DatabaseHelper.CATEGORY_ID_COL + " = ?",
                new String[]{String.valueOf(id)}
        ) > 0;
    }

    public String getCategoryNameById(int categoryId) {
        String categoryName = "";
        Cursor cursor = db.query(
                DatabaseHelper.CATEGORY_TABLE,
                new String[]{DatabaseHelper.CATEGORY_NAME_COL},
                DatabaseHelper.CATEGORY_ID_COL + " = ?",
                new String[]{String.valueOf(categoryId)},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            categoryName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CATEGORY_NAME_COL));
            cursor.close();
        }
        return categoryName;
    }

}
