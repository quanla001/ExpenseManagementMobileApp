package com.example.expensemanagementstudent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.example.expensemanagementstudent.db.UserDB;


public class RegisterActivity extends AppCompatActivity {
    TextView tvRegister;
    EditText edtUsername, edtEmail, edtAddress, edtPassword;
    Button btnRegister;

    UserDB userDB;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity_layout);

        //nap view cho thong tin
        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtAddress = findViewById(R.id.editTextAddress);
        edtPassword = findViewById(R.id.editTextPassword);
        btnRegister = findViewById(R.id.btnRegis);

        userDB = new UserDB(RegisterActivity.this);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                singupWithSQLite();

            }
        });

        tvRegister = findViewById(R.id.tvSignIn);
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Add touch listener to the parent layout
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ConstraintLayout parentLayout = findViewById(R.id.constraintLayout);
        parentLayout.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard();
            return false;
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void singupWithSQLite() {
        String user = edtUsername.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();

        // Get selected gender from RadioGroup
        RadioGroup radioGroupGender = findViewById(R.id.radioGroupGender);
        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        String gender = "";

        if (selectedGenderId == R.id.radioMale) {
            gender = "Male";
        } else if (selectedGenderId == R.id.radioFemale) {
            gender = "Female";
        }

        // Validate input fields
        if (TextUtils.isEmpty(user)) {
            showMessage("Username is required.");
            return;
        }

        if (userDB.isUsernameExists(user)) {
            showMessage("This username is already taken. Please choose another username.");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            showMessage("Email is required.");
            return;
        }

        if (!isValidEmail(email)) {
            showMessage("Invalid email format. Please provide a valid email address.");
            return;
        }

        if (userDB.isEmailExists(email)) {
            showMessage("This email is already registered. Please use another email.");
            return;
        }

        if (TextUtils.isEmpty(pass)) {
            showMessage("Password is required.");
            return;
        }

        // Validate password with detailed error messages
        if (!validatePassword(pass)) {
            return;
        }

        if (TextUtils.isEmpty(gender)) {
            showMessage("Please select your gender.");
            return;
        }

        if (TextUtils.isEmpty(address)) {
            showMessage("Address is required.");
            return;
        }

        // Add user data to SQLite
        long insert = userDB.addNewAccountUser(user, pass, email, gender, address);
        if (insert == -1) {
            showMessage("Registration failed. Please try again.");
        } else {
            // Save user info in SharedPreferences after successful registration
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", user);
            editor.putString("address", address);
            editor.putString("email", email);  // Optional: Store more info like email
            editor.putBoolean("isLoggedIn", true); // Mark as logged in
            editor.apply();

            showMessage("Registration successful!");
            // Redirect to login screen
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // Helper function to validate password and show specific error messages
    private boolean validatePassword(String password) {
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters long.");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            showMessage("Password must contain at least one uppercase letter.");
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            showMessage("Password must contain at least one lowercase letter.");
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            showMessage("Password must contain at least one number.");
            return false;
        }
        if (!password.matches(".*[@#$%^&+=!].*")) {
            showMessage("Password must contain at least one special character (@#$%^&+=!).");
            return false;
        }
        return true;
    }

    // Helper function to check if email is valid
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Helper function to display messages as Toast
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
}
