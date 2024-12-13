package com.example.expensemanagementstudent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.expensemanagementstudent.db.UserDB;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText edtUsername, edtPassword;
    Button btnLogin;
    TextView tvForgotPassword, tvSignUp;
    UserDB userDB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_layout);

        // Khởi tạo UserDB
        userDB = new UserDB(this);

        // Ánh xạ các thành phần giao diện
        edtUsername = findViewById(R.id.editTextUsername);
        edtPassword = findViewById(R.id.editTextPasswordLogin);
        btnLogin = findViewById(R.id.buttonGetStarted);
        tvForgotPassword = findViewById(R.id.textViewForgotPassword);
        tvSignUp = findViewById(R.id.textViewSignUp);

        // Lấy SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        // Hiển thị username nếu đã đăng nhập trước đó
        boolean showUsernameOnce = sharedPreferences.getBoolean("showUsernameOnce", false);
        if (showUsernameOnce) {
            String savedUsername = sharedPreferences.getString("username", "");
            edtUsername.setText(savedUsername);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("showUsernameOnce");
            editor.apply();
        }

        // Xóa mật khẩu khi vào màn hình đăng nhập
        edtPassword.setText("");

        // Xử lý sự kiện nhấn nút "Get Started"
        btnLogin.setOnClickListener(view -> {
            String input = edtUsername.getText().toString().trim(); // Có thể là username hoặc email
            String password = edtPassword.getText().toString().trim();

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter complete information!", Toast.LENGTH_SHORT).show();
            } else {
                String loginResult = userDB.checkLogin(input, password); // Dùng phương thức checkLogin mới
                switch (loginResult) {
                    case "USER_NOT_FOUND":
                        Toast.makeText(LoginActivity.this, "User does not exist!", Toast.LENGTH_SHORT).show();
                        break;

                    case "WRONG_PASSWORD":
                        Toast.makeText(LoginActivity.this, "Wrong password!", Toast.LENGTH_SHORT).show();
                        break;

                    case "LOGIN_SUCCESS":
                        Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                        int userId = userDB.getUserId(input); // Lấy userId bằng input (username hoặc email)

                        // Nếu đăng nhập bằng email, lấy username tương ứng từ cơ sở dữ liệu
                        String username;
                        if (input.contains("@")) { // Nếu là email
                            username = userDB.getUsernameByEmail(input);  // Phương thức mới để lấy username từ email
                        } else { // Nếu là username
                            username = input; // Lưu trực tiếp input nếu là username
                        }

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("isLoggedIn", true);
                        editor.putString("username", username);  // Lưu username vào SharedPreferences
                        editor.putInt("userId", userId);
                        editor.putBoolean("showUsernameOnce", true);
                        editor.apply();

                        // Chuyển đến màn hình chính
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        break;

                    default:
                        Toast.makeText(LoginActivity.this, "Unknown error!!!", Toast.LENGTH_SHORT).show();
                }
            }
        });



        // Xử lý sự kiện nhấn "Sign Up"
        tvSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Xử lý sự kiện nhấn "Forgot Password"
        tvForgotPassword.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Ẩn bàn phím khi chạm vào layout bên ngoài
        ConstraintLayout parentLayout = findViewById(R.id.constraintLayout);
        parentLayout.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard();
            return false;
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

}