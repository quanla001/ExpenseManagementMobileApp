package com.example.expensemanagementstudent.Fragment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensemanagementstudent.MainActivity;
import com.example.expensemanagementstudent.R;
import com.example.expensemanagementstudent.adapter.TransactionAdapter;
import com.example.expensemanagementstudent.db.ExpenseDB;
import com.example.expensemanagementstudent.model.Transaction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReportFragment extends Fragment {
    private Button exportPdfButton;
    private Button fromDateButton, toDateButton;
    private String fromDate = "";
    private String toDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        // Thiết lập Toolbar
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            // Quay lại MainActivity khi nhấn Back
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        });

        // Khởi tạo các thành phần giao diện
        fromDateButton = view.findViewById(R.id.fromDateButton);
        toDateButton = view.findViewById(R.id.toDateButton);
        exportPdfButton = view.findViewById(R.id.exportPdfButton); // Nút xuất PDF
        RecyclerView recyclerView = view.findViewById(R.id.costsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Gán sự kiện cho nút chọn ngày
        fromDateButton.setOnClickListener(v -> showDatePicker(fromDateButton));
        toDateButton.setOnClickListener(v -> showDatePicker(toDateButton));
        // Xử lý sự kiện cho nút EXPORT PDF
        exportPdfButton.setOnClickListener(v -> exportTransactionsToPDF());
        return view;
    }

    @SuppressLint("Range")
    private void loadTransactions(int userId, String startDate, String endDate) {
        ExpenseDB expenseDB = new ExpenseDB(getContext());
        Cursor cursor = expenseDB.getTransactionsByDateRange(userId, startDate, endDate);

        List<Transaction> transactions = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                // Lấy dữ liệu từ cursor
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                String description = cursor.getString(cursor.getColumnIndex("description"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                int categoryId = cursor.getInt(cursor.getColumnIndex("category_id"));
                int type = cursor.getInt(cursor.getColumnIndex("type")); // 0: income, 1: expense

                // Lấy tên danh mục
                Cursor categoryCursor = expenseDB.getCategoryById(categoryId);
                String categoryName = "";
                if (categoryCursor != null && categoryCursor.moveToFirst()) {
                    categoryName = categoryCursor.getString(categoryCursor.getColumnIndex("name"));
                }

                // Định dạng số tiền
                String amountText = String.format("%,.2f", amount);
                int textColor = (type == 0) ? getResources().getColor(R.color.red) : getResources().getColor(R.color.green);
                String formattedAmount = (type == 0 ? "-" : "+") + amountText;

                // Thêm giao dịch vào danh sách
                transactions.add(new Transaction(id, formattedAmount, description, date, categoryName, textColor, type));

                if (categoryCursor != null) {
                    categoryCursor.close();
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Cập nhật RecyclerView
        updateRecyclerView(transactions);
    }



    private void updateRecyclerView(List<Transaction> transactions) {
        RecyclerView recyclerView = requireView().findViewById(R.id.costsRecyclerView);
        TextView noDataTextView = requireView().findViewById(R.id.noDataTextView);

        if (transactions.isEmpty()) {
            // Hiển thị thông báo khi không có dữ liệu
            noDataTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // Hiển thị RecyclerView khi có dữ liệu
            noDataTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            // Cập nhật Adapter với danh sách giao dịch
            TransactionAdapter adapter = new TransactionAdapter(transactions);
            recyclerView.setAdapter(adapter);
        }
    }


    /**
     * Displays a DatePickerDialog and updates the text of the provided button.
     *
     * @param button The button to update with the selected date.
     */
    private void showDatePicker(Button button) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Định dạng ngày là dd-MM-yyyy
                    String displayDate = String.format("%02d-%02d-%04d", selectedDay, selectedMonth + 1, selectedYear);

                    if (button.getId() == R.id.fromDateButton) {
                        fromDate = displayDate; // Gán giá trị ngày bắt đầu
                    } else if (button.getId() == R.id.toDateButton) {
                        toDate = displayDate; // Gán giá trị ngày kết thúc
                    }

                    // Hiển thị ngày lên nút
                    button.setText(displayDate);

                    // Gọi loadTransactions nếu cả hai ngày đã được chọn
                    if (!fromDate.isEmpty() && !toDate.isEmpty()) {
                        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                        int userId = sharedPreferences.getInt("userId", -1); // -1 nếu không tìm thấy userId
                        loadTransactions(userId, fromDate, toDate); // Thay 1 bằng userId thực tế
                    }
                },
                year, month, day);

        datePickerDialog.show();
    }
    private void exportTransactionsToPDF() {
        RecyclerView recyclerView = requireView().findViewById(R.id.costsRecyclerView);
        TransactionAdapter adapter = (TransactionAdapter) recyclerView.getAdapter();

        if (adapter == null || adapter.getItemCount() == 0) {
            Toast.makeText(getContext(), "No transactions to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Transaction> transactions = adapter.getTransactions();

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();

        int pageWidth = 595; // A4 chiều ngang (px)
        int pageHeight = 842; // A4 chiều dọc (px)
        int margin = 20;
        int currentY = margin + 40;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Vẽ tiêu đề
        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        canvas.drawText("Transaction Report", margin, currentY, paint);
        currentY += 40;

        paint.setTextSize(14);
        paint.setFakeBoldText(false);

        // Vẽ cột tiêu đề
        canvas.drawText("Date", margin, currentY, paint);
        canvas.drawText("Description", margin + 100, currentY, paint);
        canvas.drawText("Amount", pageWidth - margin - 100, currentY, paint);
        currentY += 30;

        // Vẽ từng giao dịch
        for (Transaction transaction : transactions) {
            if (currentY + 30 > pageHeight - margin) {
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                currentY = margin + 40;
            }

            canvas.drawText(transaction.getDate(), margin, currentY, paint);
            canvas.drawText(transaction.getDescription(), margin + 100, currentY, paint);
            canvas.drawText(transaction.getFormattedAmount(), pageWidth - margin - 100, currentY, paint);
            currentY += 30;
        }

        pdfDocument.finishPage(page);

        // Lưu PDF vào bộ nhớ
        File pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TransactionReport.pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(getContext(), "PDF exported successfully: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Mở file PDF sau khi lưu
            openPdfFile(pdfFile);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error exporting PDF.", Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
        }
    }

    /**
     * Mở file PDF bằng ứng dụng xem PDF mặc định.
     *
     * @param pdfFile File PDF cần mở.
     */
    private void openPdfFile(File pdfFile) {
        if (pdfFile.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    pdfFile
            ), "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "No application found to open PDF.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "PDF file not found.", Toast.LENGTH_SHORT).show();
        }
    }


}