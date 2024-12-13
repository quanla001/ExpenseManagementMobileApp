package com.example.expensemanagementstudent;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensemanagementstudent.db.CategoryDB;
import com.example.expensemanagementstudent.db.DatabaseHelper;
import com.example.expensemanagementstudent.db.ExpenseDB;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;

public class TransactionActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ExpenseDB expenseDB;
    private CategoryDB categoryDB;
    private Spinner categorySpinner;
    private ArrayAdapter<String> adapter;
    private MaterialButton btnExpense, btnIncome;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        dbHelper = new DatabaseHelper(this);
        categoryDB = new CategoryDB(this);
        expenseDB = new ExpenseDB(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextInputEditText inputAmount = findViewById(R.id.input_amount);
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.transaction_type_toggle);
        Button btnSubmit = findViewById(R.id.btn_submit);
        TextView amountErrorMessage = findViewById(R.id.amount_error_message);
        categorySpinner = findViewById(R.id.category_spinner);




        btnBack.setOnClickListener(v -> onBackPressed());
        toggleGroup.check(R.id.btn_expense);
        btnSubmit.setText("Add Expense");

        // Initialize toggle buttons for styling
        btnExpense = findViewById(R.id.btn_expense);
        btnIncome = findViewById(R.id.btn_income);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_expense) {
                    btnSubmit.setText("Add Expense");
                    updateCategorySpinner(1);  // Only show categories for expense
                    updateToggleButtonStyles(R.id.btn_expense);
                } else if (checkedId == R.id.btn_income) {
                    btnSubmit.setText("Add Income");
                    updateCategorySpinner(0);  // Only show categories for income
                    updateToggleButtonStyles(R.id.btn_income);
                }
            }
        });

        ArrayList<String> categoryNames = categoryDB.getCategoryNamesByType(1);
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // Format amount input
        inputAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    inputAmount.removeTextChangedListener(this);

                    try {
                        String input = s.toString().replaceAll("[,.\\s]", ""); // Remove existing formatting
                        if (!input.isEmpty()) {
                            long parsedAmount = Long.parseLong(input); // Parse the number

                            if (parsedAmount > 1_000_000_000) { // Check for max limit
                                amountErrorMessage.setText("Maximum amount is 999,999,999.99");
                                amountErrorMessage.setVisibility(View.VISIBLE);
                            } else {
                                amountErrorMessage.setVisibility(View.GONE);
                            }

                            current = String.format("%,d", parsedAmount).replace(",", "."); // Add formatting
                            inputAmount.setText(current);
                            inputAmount.setSelection(current.length()); // Move cursor to end
                        } else {
                            amountErrorMessage.setVisibility(View.GONE);
                        }
                    } catch (NumberFormatException e) {
                        amountErrorMessage.setVisibility(View.GONE);
                    }

                    inputAmount.addTextChangedListener(this);
                }
            }
        });

        // Other initialization code
        TextInputEditText inputDate = findViewById(R.id.input_date);
        TextInputEditText inputNotes = findViewById(R.id.input_notes);

        // Set default date to today
        Calendar calendar = Calendar.getInstance();
        inputDate.setText(formatDate(calendar));

        // Date picker for transaction date
        inputDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        inputDate.setText(formatDate(calendar));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            // Restrict future dates
            datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePicker.show();
        });


        btnSubmit.setOnClickListener(v -> {
            // Remove formatting and validate amount input
            String amountText = inputAmount.getText().toString().replaceAll("[,.\\s]", "").trim();
            if (amountText.isEmpty()) {
                amountErrorMessage.setText("Amount is required");
                amountErrorMessage.setVisibility(View.VISIBLE);
                return;
            }

            try {
                long amount = Long.parseLong(amountText);
                if (amount > 1_000_000_000) {
                    amountErrorMessage.setText("Maximum amount is 999,999,999.99");
                    amountErrorMessage.setVisibility(View.VISIBLE);
                    return;
                }

                // Clear the error message if input is valid
                amountErrorMessage.setVisibility(View.GONE);

                // Validate category selection
                String category = categorySpinner.getSelectedItem().toString();
                if (category.isEmpty() || category.equals("Select Category")) {
                    Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validate date input
                String date = inputDate.getText().toString();
                if (date.isEmpty()) {
                    Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Notes are optional but can be trimmed
                String notes = inputNotes.getText().toString().trim();

                // Determine transaction type (1 = Income, 0 = Expense)
                int type = (toggleGroup.getCheckedButtonId() == R.id.btn_income) ? 1 : 0;

                // Retrieve the logged-in user's ID from SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                int userId = sharedPreferences.getInt("userId", -1);
                if (userId == -1) {
                    Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Save the transaction to the database
                long transactionId = expenseDB.addTransaction(type, amount, notes, date, userId, categoryDB.getCategoryId(category));
                if (transactionId != -1) {
                    // Notify the user of success
                    String successMessage = (type == 1) ? "Income saved successfully" : "Expense saved successfully";
                    Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();

                    // Set result to indicate success and refresh data in the previous activity/fragment
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("transactionAdded", true);
                    setResult(RESULT_OK, resultIntent);

                    // Close the activity
                    finish();
                } else {
                    Toast.makeText(this, "Error saving transaction", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                // Handle invalid amount input
                amountErrorMessage.setText("Invalid amount entered");
                amountErrorMessage.setVisibility(View.VISIBLE);
            }
        });


    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Check if the touch event is outside the currently focused view
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view instanceof TextInputEditText) {
                // Clear focus and hide keyboard
                view.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    // Helper method to format the date
    private String formatDate(Calendar calendar) {
        return android.text.format.DateFormat.format("dd-MM-yyyy", calendar).toString();
    }

    // Update Category Spinner
    private void updateCategorySpinner(int type) {
        ArrayList<String> categoryNames = categoryDB.getCategoryNamesByType(type);
        adapter.clear();
        adapter.addAll(categoryNames);
        adapter.notifyDataSetChanged();
    }

    // Update Toggle Button Styles
    private void updateToggleButtonStyles(int checkedId) {
        int selectedColor = getResources().getColor(R.color.toggle_selected);
        int selectedTextColor = getResources().getColor(R.color.toggle_text_selected);
        int unselectedColor = getResources().getColor(R.color.toggle_unselected);
        int unselectedTextColor = getResources().getColor(R.color.toggle_text_unselected);

        if (checkedId == R.id.btn_expense) {
            btnExpense.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
            btnExpense.setTextColor(ColorStateList.valueOf(selectedTextColor));

            btnIncome.setBackgroundTintList(ColorStateList.valueOf(unselectedColor));
            btnIncome.setTextColor(ColorStateList.valueOf(unselectedTextColor));
        } else if (checkedId == R.id.btn_income) {
            btnIncome.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
            btnIncome.setTextColor(ColorStateList.valueOf(selectedTextColor));

            btnExpense.setBackgroundTintList(ColorStateList.valueOf(unselectedColor));
            btnExpense.setTextColor(ColorStateList.valueOf(unselectedTextColor));
        }
    }


}