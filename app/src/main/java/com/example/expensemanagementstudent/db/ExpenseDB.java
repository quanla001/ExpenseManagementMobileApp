package com.example.expensemanagementstudent.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

public class ExpenseDB {

    private SQLiteDatabase db;
    private DatabaseHelper dbHelper; // Add dbHelper as a field

    public ExpenseDB(Context context) {
        dbHelper = new DatabaseHelper(context); // Initialize dbHelper
        db = dbHelper.getWritableDatabase();
    }

    // Provide access to DatabaseHelper's getReadableDatabase
    public SQLiteDatabase getReadableDatabase() {
        return dbHelper.getReadableDatabase(); // Use the initialized dbHelper
    }

    // Get expenses by category and month
    public Cursor getExpenseByCategoryAndMonth(int userId, String month, String year) {
        String query = "SELECT c.name, SUM(e.amount) " +
                "FROM expenses e " +
                "INNER JOIN categories c ON e.category_id = c._id " +
                "WHERE e.user_id = ? AND e.type = 0 " + // Thêm điều kiện lọc e.type = 1
                "AND strftime('%m', substr(e.date, 7, 4) || '-' || substr(e.date, 4, 2) || '-' || substr(e.date, 1, 2)) = ? " +
                "AND strftime('%Y', substr(e.date, 7, 4) || '-' || substr(e.date, 4, 2) || '-' || substr(e.date, 1, 2)) = ? " +
                "GROUP BY c.name";
        return db.rawQuery(query, new String[]{String.valueOf(userId), month, year});
    }
    public Cursor getTransactionsByDateRange(int userId, String startDate, String endDate) {
        String query = "SELECT * FROM expenses " +
                "WHERE user_id = ? " +
                "AND date BETWEEN ? AND ?";
        return db.rawQuery(query, new String[]{String.valueOf(userId), startDate, endDate});
    }



    public Cursor getTotalByType(int userId, String month, String year, int type) {
        String query = "SELECT SUM(e.amount) " +
                "FROM expenses e " +
                "WHERE e.user_id = ? AND e.type = ? AND strftime('%m', substr(e.date, 7, 4) || '-' || substr(e.date, 4, 2) || '-' || substr(e.date, 1, 2)) = ? " +
                "AND strftime('%Y', substr(e.date, 7, 4) || '-' || substr(e.date, 4, 2) || '-' || substr(e.date, 1, 2)) = ?";
        return db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(type), month, year});
    }

    @SuppressLint("Range")
    public void logExpensesForMonth(int userId, String month) {
        String query = "SELECT * " +
                "FROM expenses " +
                "WHERE user_id = ? AND strftime('%m', date) = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), month});

        if (cursor.moveToFirst()) {
            do {
                Log.d("ExpenseDB", "ID: " + cursor.getInt(cursor.getColumnIndex("id")) +
                        ", Amount: " + cursor.getFloat(cursor.getColumnIndex("amount")) +
                        ", Date: " + cursor.getString(cursor.getColumnIndex("date")) +
                        ", Category ID: " + cursor.getInt(cursor.getColumnIndex("category_id")));
            } while (cursor.moveToNext());
        } else {
            Log.d("ExpenseDB", "No expenses found for the month: " + month);
        }
        cursor.close();
    }
    public Cursor getCategoryById(int categoryId) {
        String query = "SELECT name FROM categories WHERE " + DatabaseHelper.CATEGORY_ID_COL + " = ?";
        return db.rawQuery(query, new String[]{String.valueOf(categoryId)});
    }



    // Add a transaction (income or expense)
    public long addTransaction(int type, double amount, String description, String date, int userId, int categoryId) {
        ContentValues values = new ContentValues();

        // Set column values
        values.put(DatabaseHelper.TYPE_COL, type); // Type (0: income, 1: expense)
        values.put(DatabaseHelper.AMOUNT_COL, amount); // Amount
        values.put(DatabaseHelper.DESCRIPTION_COL, description); // Description
        values.put(DatabaseHelper.DATE_COL, date); // Date
        values.put(DatabaseHelper.EXPENSE_USER_ID_COL, userId); // User ID
        values.put(DatabaseHelper.EXPENSE_CATEGORY_ID_COL, categoryId); // Category ID

        // Insert into the "expenses" table
        return db.insert(DatabaseHelper.EXPENSE_TABLE, null, values);
    }


    public Cursor getTransactionsByUserId(int userId) {
        String query = "SELECT e.*, c.name AS category_name " +
                "FROM expenses e " +
                "INNER JOIN categories c ON e.category_id = c._id " +
                "WHERE e.user_id = ? " +
                "ORDER BY e.date DESC";
        return db.rawQuery(query, new String[]{String.valueOf(userId)});
    }

    public Cursor getFilteredTransactionsForUser(int userId, int type, int categoryId) {
        StringBuilder query = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();

        query.append("SELECT e.*, c.name AS category_name ")
                .append("FROM ").append(DatabaseHelper.EXPENSE_TABLE).append(" e ")
                .append("INNER JOIN ").append(DatabaseHelper.CATEGORY_TABLE).append(" c ")
                .append("ON e.").append(DatabaseHelper.EXPENSE_CATEGORY_ID_COL).append(" = c.").append(DatabaseHelper.CATEGORY_ID_COL)
                .append(" WHERE e.").append(DatabaseHelper.EXPENSE_USER_ID_COL).append(" = ?");

        args.add(String.valueOf(userId));

        // Add condition for type if specified
        if (type != -1) {
            query.append(" AND e.").append(DatabaseHelper.TYPE_COL).append(" = ?");
            args.add(String.valueOf(type));
        }

        // Add condition for categoryId if specified
        if (categoryId != -1) {
            query.append(" AND e.").append(DatabaseHelper.EXPENSE_CATEGORY_ID_COL).append(" = ?");
            args.add(String.valueOf(categoryId));
        }

        query.append(" ORDER BY e.").append(DatabaseHelper.DATE_COL).append(" DESC");

        return db.rawQuery(query.toString(), args.toArray(new String[0]));
    }

    public Cursor getTransactionsForUser(int userId) {
        String query = "SELECT e.*, c.name AS category_name " +
                "FROM " + DatabaseHelper.EXPENSE_TABLE + " e " +
                "INNER JOIN " + DatabaseHelper.CATEGORY_TABLE + " c " +
                "ON e." + DatabaseHelper.EXPENSE_CATEGORY_ID_COL + " = c." + DatabaseHelper.CATEGORY_ID_COL +
                " WHERE e." + DatabaseHelper.EXPENSE_USER_ID_COL + " = ?" +
                " ORDER BY e." + DatabaseHelper.DATE_COL + " DESC";
        return db.rawQuery(query, new String[]{String.valueOf(userId)});
    }
    // Update Transaction
    public boolean updateTransaction(int transactionId, double amount, String description, String date, int categoryId, int type) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.AMOUNT_COL, amount);
        values.put(DatabaseHelper.DESCRIPTION_COL, description);
        values.put(DatabaseHelper.DATE_COL, date);
        values.put(DatabaseHelper.EXPENSE_CATEGORY_ID_COL, categoryId);
        values.put(DatabaseHelper.TYPE_COL, type);

        return db.update(
                DatabaseHelper.EXPENSE_TABLE,
                values,
                DatabaseHelper.EXPENSE_ID_COL + " = ?",
                new String[]{String.valueOf(transactionId)}
        ) > 0;
    }
    public Cursor getTransactionById(int transactionId) {
        String query = "SELECT * FROM " + DatabaseHelper.EXPENSE_TABLE +
                " WHERE " + DatabaseHelper.EXPENSE_ID_COL + " = ?";
        return db.rawQuery(query, new String[]{String.valueOf(transactionId)});
    }
    public boolean deleteTransaction(int transactionId) {
        int rowsDeleted = db.delete(DatabaseHelper.EXPENSE_TABLE,
                DatabaseHelper.EXPENSE_ID_COL + " = ?",
                new String[]{String.valueOf(transactionId)});
        return rowsDeleted > 0; // Return true if a row was deleted
    }

}
