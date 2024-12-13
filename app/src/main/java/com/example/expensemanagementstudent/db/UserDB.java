package com.example.expensemanagementstudent.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserDB {

    private SQLiteDatabase db;

    public UserDB(Context context) {
        DatabaseHelper helper = new DatabaseHelper(context);
        db = helper.getWritableDatabase();
    }

    // Method to get userId by username or email
    @SuppressLint("Range")
    public int getUserId(String input) {
        int userId = -1; // Default value if user not found
        Cursor cursor = null;

        try {
            // Kiểm tra thông tin qua username hoặc email
            cursor = db.query(DatabaseHelper.USER_TABLE,
                    new String[]{DatabaseHelper.USER_ID_COL}, // Column to retrieve
                    DatabaseHelper.USERNAME_COL + " = ? OR " + DatabaseHelper.EMAIL_COL + " = ?",
                    new String[]{input, input},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                userId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.USER_ID_COL));
            }
        } finally {
            if (cursor != null) {
                cursor.close(); // Ensure cursor is closed to avoid memory leaks
            }
        }
        return userId;
    }

    // Method to update user details
    public boolean updateUserDetails(long userId, String email, String address) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.EMAIL_COL, email);
        values.put(DatabaseHelper.ADDRESS_COL, address);

        int rowsAffected = db.update(DatabaseHelper.USER_TABLE, values,
                DatabaseHelper.USER_ID_COL + " = ?",
                new String[]{String.valueOf(userId)});
        return rowsAffected > 0;
    }

    // Method to add new account user
    public long addNewAccountUser(String username, String password, String email, String gender, String address) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.USERNAME_COL, username);
        values.put(DatabaseHelper.PASS_COL, password);
        values.put(DatabaseHelper.EMAIL_COL, email);
        values.put(DatabaseHelper.GENDER_COL, gender);
        values.put(DatabaseHelper.ADDRESS_COL, address);

        return db.insert(DatabaseHelper.USER_TABLE, null, values);
    }

    // Method to check login (check both username and email)
    public String checkLogin(String input, String password) {
        // Câu truy vấn SQL, kiểm tra cả username và email
        String query = "SELECT " + DatabaseHelper.PASS_COL +
                " FROM " + DatabaseHelper.USER_TABLE +
                " WHERE " + DatabaseHelper.USERNAME_COL + " = ? OR " + DatabaseHelper.EMAIL_COL + " = ?";

        // Sử dụng rawQuery để chạy câu truy vấn
        Cursor cursor = db.rawQuery(query, new String[]{input, input});

        if (cursor != null && cursor.moveToFirst()) {
            // Lấy mật khẩu từ kết quả truy vấn
            String storedPassword = cursor.getString(0);
            cursor.close();

            if (storedPassword.equals(password)) {
                return "LOGIN_SUCCESS";
            } else {
                return "WRONG_PASSWORD";
            }
        } else {
            if (cursor != null) cursor.close();
            return "USER_NOT_FOUND";
        }
    }

    // Method to check if email exists
    public boolean isEmailExists(String email) {
        String query = "SELECT * FROM " + DatabaseHelper.USER_TABLE + " WHERE " + DatabaseHelper.EMAIL_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Method to check if username exists
    public boolean isUsernameExists(String username) {
        String query = "SELECT * FROM " + DatabaseHelper.USER_TABLE + " WHERE " + DatabaseHelper.USERNAME_COL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Method to reset password by email
    public boolean resetPassword(String email, String newPassword) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.PASS_COL, newPassword);

        int rowsAffected = db.update(DatabaseHelper.USER_TABLE, values,
                DatabaseHelper.EMAIL_COL + " = ?",
                new String[]{email});
        return rowsAffected > 0;
    }
    // Lấy username từ email
    @SuppressLint("Range")
    public String getUsernameByEmail(String email) {
        String username = null;
        Cursor cursor = null;

        try {
            cursor = db.query(DatabaseHelper.USER_TABLE,
                    new String[]{DatabaseHelper.USERNAME_COL},  // Chỉ lấy cột username
                    DatabaseHelper.EMAIL_COL + " = ?",
                    new String[]{email},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                username = cursor.getString(cursor.getColumnIndex(DatabaseHelper.USERNAME_COL));
            }
        } finally {
            if (cursor != null) {
                cursor.close(); // Đảm bảo đóng cursor để tránh rò rỉ bộ nhớ
            }
        }

        return username;
    }

}