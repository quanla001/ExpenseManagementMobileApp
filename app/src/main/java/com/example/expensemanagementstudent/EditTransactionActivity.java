package com.example.expensemanagementstudent;

import android.app.DatePickerDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensemanagementstudent.db.CategoryDB;
import com.example.expensemanagementstudent.db.DatabaseHelper;
import com.example.expensemanagementstudent.db.ExpenseDB;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class EditTransactionActivity extends AppCompatActivity {

    private TextInputEditText inputEditAmount, inputEditDescription, inputEditDate;
    private Spinner editCategorySpinner;
    private Button btnSaveChanges;
    private int transactionId, transactionType;
    private ExpenseDB expenseDB;
    private CategoryDB categoryDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_transaction);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());
        // Initialize database helpers
        expenseDB = new ExpenseDB(this);
        categoryDB = new CategoryDB(this);

        // Initialize UI components
        inputEditAmount = findViewById(R.id.input_edit_amount);
        inputEditDescription = findViewById(R.id.input_edit_notes);
        inputEditDate = findViewById(R.id.input_edit_date);
        editCategorySpinner = findViewById(R.id.edit_category_spinner);
        btnSaveChanges = findViewById(R.id.btn_save_changes);

        // Get transactionId and type from Intent
        transactionId = getIntent().getIntExtra("transactionId", -1);
        transactionType = getIntent().getIntExtra("transactionType", -1);

        if (transactionId == -1 || transactionType == -1) {
            Toast.makeText(this, "Invalid transaction data", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Load transaction data into fields
        loadTransactionData(transactionId, transactionType);

        // Date picker for editing the date
        inputEditDate.setOnClickListener(v -> showDatePicker());

        // Save button click listener
        btnSaveChanges.setOnClickListener(v -> saveTransactionChanges());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    inputEditDate.setText(formatDate(calendar));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Restrict future dates
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePicker.show();
    }


    private String formatDate(Calendar calendar) {
        return android.text.format.DateFormat.format("dd-MM-yyyy", calendar).toString();
    }

    /**
     * Load transaction data into fields for editing.
     */
    private void loadTransactionData(int transactionId, int type) {
        Cursor cursor = expenseDB.getTransactionById(transactionId);
        if (cursor != null && cursor.moveToFirst()) {
            double amount = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.AMOUNT_COL));
            String description = cursor.getString(cursor.getColumnIndex(DatabaseHelper.DESCRIPTION_COL));
            String date = cursor.getString(cursor.getColumnIndex(DatabaseHelper.DATE_COL));
            int categoryId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.EXPENSE_CATEGORY_ID_COL));

            // Pre-fill fields
            inputEditAmount.setText(String.format(Locale.US, "%,.2f", amount));
            inputEditDescription.setText(description);
            inputEditDate.setText(date);

            // Update spinner categories based on transaction type
            updateCategorySpinner(type);

            // Match the category ID to the category name
            ArrayList<String> categories = categoryDB.getCategoryNamesByType(type);
            String categoryName = categoryDB.getCategoryNameById(categoryId);

            // Log for debugging
            Log.d("EditTransaction", "Category Name: " + categoryName + ", Categories: " + categories);

            // Set the correct category in the spinner
            if (categories.contains(categoryName)) {
                int position = categories.indexOf(categoryName);
                editCategorySpinner.setSelection(position);
            } else {
                Log.e("EditTransaction", "Category mismatch. ID: " + categoryId + ", Name: " + categoryName);
            }
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Update category spinner based on transaction type.
     */
    private void updateCategorySpinner(int type) {
        // Reverse the type: 0 (income) -> 1 (expense), and 1 (expense) -> 0 (income)
        int reversedType = (type == 0) ? 1 : 0;

        // Get category names based on the reversed type
        ArrayList<String> categoryNames = categoryDB.getCategoryNamesByType(reversedType);

        // Set up the spinner adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editCategorySpinner.setAdapter(adapter);
    }


    /**
     * Save changes made to the transaction.
     */
    private void saveTransactionChanges() {
        String amountText = inputEditAmount.getText().toString().trim().replace(",", ""); // Remove commas
        String description = inputEditDescription.getText().toString().trim();
        String date = inputEditDate.getText().toString().trim();
        String categoryName = editCategorySpinner.getSelectedItem().toString();

        if (amountText.isEmpty() || description.isEmpty() || date.isEmpty() || categoryName.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountText); // Parse cleaned-up amount
            int categoryId = categoryDB.getCategoryId(categoryName);

            boolean isUpdated = expenseDB.updateTransaction(transactionId, amount, description, date, categoryId, transactionType);
            if (isUpdated) {
                Toast.makeText(this, "Transaction updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error updating transaction", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount entered", Toast.LENGTH_SHORT).show();
        }
    }


}
