package com.example.expensemanagementstudent.Fragment;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.example.expensemanagementstudent.TransactionHistoryActivity;
import com.example.expensemanagementstudent.CategoryActivity;
import com.example.expensemanagementstudent.R;
import com.example.expensemanagementstudent.db.DatabaseHelper;
import com.example.expensemanagementstudent.db.ExpenseDB;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class OverviewFragment extends Fragment {

    private TextView monthYearDisplay, tvIncome, tvExpense, totalBalanceTextView;
    private Calendar calendar;
    private LinearLayout transactionListContainer;
    private ExpenseDB expenseDB;

    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String LAST_NOTIFICATION_TIME = "lastNotificationTime";
    private static final String LAST_BALANCE = "lastBalance";
    private static final long NOTIFICATION_COOLDOWN = 10 * 60 * 1000; // 10 minutes in milliseconds
    private boolean notificationsEnabled = true;

    public OverviewFragment() {
        // Required empty public constructor
    }

    public static OverviewFragment newInstance(String param1, String param2) {
        OverviewFragment fragment = new OverviewFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            getArguments().getString("param1");
            getArguments().getString("param2");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_overview, container, false);

        // Initialize the database
        expenseDB = new ExpenseDB(requireContext());

        // Initialize views
        totalBalanceTextView = rootView.findViewById(R.id.total_balance);
        tvIncome = rootView.findViewById(R.id.tvIncome);
        tvExpense = rootView.findViewById(R.id.tvExpense);
        transactionListContainer = rootView.findViewById(R.id.transaction_list_container);

        TextView greetingText = rootView.findViewById(R.id.greetingText);
        TextView seeAllButton = rootView.findViewById(R.id.see_all_button);
        LinearLayout btnAddCategory = rootView.findViewById(R.id.btnAddCategory);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "Username");

        greetingText.setText("Hi " + username + ",");

        monthYearDisplay = rootView.findViewById(R.id.month_year_display);
        ImageView previousMonth = rootView.findViewById(R.id.previous_month);
        ImageView nextMonth = rootView.findViewById(R.id.next_month);

        calendar = Calendar.getInstance();
        updateMonthYearDisplay();

        previousMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonthYearDisplay();
            updateIncomeAndExpense();
        });

        nextMonth.setOnClickListener(v -> {
            Calendar currentDate = Calendar.getInstance(); // Get the current date
            if (calendar.get(Calendar.YEAR) < currentDate.get(Calendar.YEAR) ||
                    (calendar.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) < currentDate.get(Calendar.MONTH))) {
                // Allow adding a month only if it's not the current month or future
                calendar.add(Calendar.MONTH, 1);
                updateMonthYearDisplay();
                updateIncomeAndExpense();
            }
        });


        seeAllButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TransactionHistoryActivity.class);
            startActivity(intent);
        });

        btnAddCategory.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CategoryActivity.class);
            startActivity(intent);
        });

        // Initialize notification icon
        ImageView notificationIcon = rootView.findViewById(R.id.notificationIcon);

        // Load the current notification state
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        notificationsEnabled = prefs.getBoolean("notificationsEnabled", true);

        // Set the initial icon state
        updateNotificationIcon(notificationIcon);

        // Handle notification icon click
        notificationIcon.setOnClickListener(v -> {
            notificationsEnabled = !notificationsEnabled; // Toggle the state
            saveNotificationState(notificationsEnabled);  // Save the new state
            updateNotificationIcon(notificationIcon);     // Update the icon
            showNotificationToggleMessage();              // Show a message
        });

        // Load transactions dynamically into the container
        loadTransactions();
        updateIncomeAndExpense(); // Update income, expense, and balance values

        return rootView;
    }


    private void updateMonthYearDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.ENGLISH);
        String formattedDate = dateFormat.format(calendar.getTime());
        monthYearDisplay.setText(formattedDate);
    }

    private void updateIncomeAndExpense() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        int userId = sharedPreferences.getInt("userId", -1);

        // Fetch month and year
        String month = String.format(Locale.ENGLISH, "%02d", calendar.get(Calendar.MONTH) + 1);
        String year = String.valueOf(calendar.get(Calendar.YEAR));

        // Fetch total income and expense
        Cursor cursorIncome = expenseDB.getTotalByType(userId, month, year, 1); // 1: Income
        Cursor cursorExpense = expenseDB.getTotalByType(userId, month, year, 0); // 0: Expense

        float totalIncome = 0;
        float totalExpense = 0;

        if (cursorIncome != null && cursorIncome.moveToFirst()) {
            totalIncome = cursorIncome.getFloat(0);
            cursorIncome.close();
        }

        if (cursorExpense != null && cursorExpense.moveToFirst()) {
            totalExpense = cursorExpense.getFloat(0);
            cursorExpense.close();
        }

        // Update TextViews
        tvIncome.setText(String.format("$%,.0f", totalIncome));
        tvExpense.setText(String.format("$%,.0f", totalExpense));

        // Update total balance
        double totalBalance = totalIncome - totalExpense;
        totalBalanceTextView.setText(String.format("$%,.0f", totalBalance));

        // Check for income threshold
        checkIncomeThreshold(totalIncome, totalBalance);
    }
    // Check income and balance thresholds
    private void checkIncomeThreshold(float totalIncome, double totalBalance) {
        // Fetch the notification state
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notificationsEnabled", true); // Default: enabled
        long lastNotificationTime = prefs.getLong(LAST_NOTIFICATION_TIME, 0);
        double lastNotifiedBalance = Double.longBitsToDouble(prefs.getLong(LAST_BALANCE, Double.doubleToLongBits(0)));

        // If notifications are disabled, skip
        if (!notificationsEnabled) {
            return;
        }

        // Current time
        long currentTime = System.currentTimeMillis();

        // Calculate threshold
        float veryLowBalanceThreshold = totalIncome * 0.2f;

        // Check cooldown and balance change
        if (currentTime - lastNotificationTime < NOTIFICATION_COOLDOWN && totalBalance == lastNotifiedBalance) {
            return; // Skip notification if within cooldown and no balance change
        }

        // Trigger notification for very low balance
        if (totalBalance < veryLowBalanceThreshold && totalBalance >= 0) {
            sendNotification("Critical: Very Low Balance", "Your balance is below 20% of your income. Immediate action is required.");
            saveNotificationState(currentTime, totalBalance);
            return;
        }

        // Trigger notification for negative balance
        if (totalBalance < 0) {
            sendNotification("Critical: Negative Balance", "Your balance is negative. Please review your expenses immediately.");
            saveNotificationState(currentTime, totalBalance);
        }
    }

    // Update the notification icon based on the current state
    private void updateNotificationIcon(ImageView notificationIcon) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notificationsEnabled", true); // Default: enabled
        if (notificationsEnabled) {
            notificationIcon.setImageResource(R.drawable.bell); // Replace with your "bell" icon
        } else {
            notificationIcon.setImageResource(R.drawable.bell_off); // Replace with your "bell_off" icon
        }
    }

    // Save the notification state in SharedPreferences
    private void saveNotificationState(boolean enabled) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("notificationsEnabled", enabled);
        editor.apply();
    }

    // Show a message when notifications are toggled
    private void showNotificationToggleMessage() {
        String message = notificationsEnabled ? "Notifications Enabled" : "Notifications Turned Off";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Save the state of the last notification
    private void saveNotificationState(long currentTime, double totalBalance) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_NOTIFICATION_TIME, currentTime);
        editor.putLong(LAST_BALANCE, Double.doubleToLongBits(totalBalance));
        editor.apply();
    }

    // Toggle the notification state
    private void toggleNotifications(ImageView notificationIcon) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notificationsEnabled", true);
        notificationsEnabled = !notificationsEnabled; // Toggle state
        saveNotificationState(notificationsEnabled);
        updateNotificationIcon(notificationIcon);
        showNotificationToggleMessage();
    }


    @Override
    public void onResume() {
        super.onResume();
        loadTransactions(); // Reload the transactions when the fragment is resumed
        updateIncomeAndExpense();
    }

    private void loadTransactions() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        int userId = sharedPreferences.getInt("userId", -1);
        if (userId == -1) {
            Toast.makeText(getContext(), "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        transactionListContainer.removeAllViews(); // Clear previous transactions
        Cursor cursor = expenseDB.getTransactionsByUserId(userId);
        int limit = 2; // Show only two newest transactions
        int count = 0;

        if (cursor != null) {
            while (cursor.moveToNext() && count < limit) {
                String category = cursor.getString(cursor.getColumnIndex("category_name"));
                String description = cursor.getString(cursor.getColumnIndex(DatabaseHelper.DESCRIPTION_COL));
                double amount = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.AMOUNT_COL));
                int type = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.TYPE_COL));
                String date = cursor.getString(cursor.getColumnIndex(DatabaseHelper.DATE_COL));

                addTransactionItem(category, description, amount, type, date);
                count++;
            }
            cursor.close();
        }
    }

    private void addTransactionItem(String category, String description, double amount, int type, String date) {
        View transactionItem = LayoutInflater.from(getContext()).inflate(R.layout.transaction_item_overview, transactionListContainer, false);

        TextView categoryView = transactionItem.findViewById(R.id.transaction_category);
        TextView descriptionView = transactionItem.findViewById(R.id.transaction_description);
        TextView amountView = transactionItem.findViewById(R.id.transaction_amount);
        TextView dateView = transactionItem.findViewById(R.id.transaction_date);

        View editButton = transactionItem.findViewById(R.id.btn_edit_transaction);
        View deleteButton = transactionItem.findViewById(R.id.btn_delete_transaction);
        editButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);

        categoryView.setText(category);
        descriptionView.setText(description);
        dateView.setText(date);

        if (type == 1) { // Income
            amountView.setText(String.format("+ %, .0f $", amount));
            amountView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else { // Expense
            amountView.setText(String.format("- %, .0f $", amount));
            amountView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        transactionListContainer.addView(transactionItem);
    }

    private void sendNotification(String title, String message) {
        // Check for Notification Permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
                return; // Exit to wait for permission response
            }
        }

        // Create Notification Channel (for Android 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "income_notification_channel",
                    "Income Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = requireActivity().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "income_notification_channel")
                .setSmallIcon(R.drawable.ic_notification) // Replace with your app's notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        notificationManager.notify(1, builder.build());
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, send the notification
                sendNotification("Permission Granted", "You can now receive notifications.");
            } else {
                // Permission denied
                Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
