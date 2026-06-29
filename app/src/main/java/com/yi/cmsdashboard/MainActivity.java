package com.yi.cmsdashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final String GITHUB_TOKEN = "YOUR_CMS_TOKEN";
    private final String OWNER = "YOUR_GITHUB_ID";
    private final String REPO = "YOUR_REPO_NAME";

    private TableLayout statusTable;
    private TextView errorLogView;
    private OkHttpClient client;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTable = findViewById(R.id.statusTable);
        errorLogView = findViewById(R.id.errorLogView);
        Button refreshBtn = findViewById(R.id.refreshBtn);

        client = new OkHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());

        refreshBtn.setOnClickListener(v -> fetchData());

        // Auto-refresh every 5 mins
        Runnable autoRefresh = new Runnable() {
            @Override
            public void run() {
                fetchData();
                mainHandler.postDelayed(this, 300000);
            }
        };
        mainHandler.post(autoRefresh);
    }

    private void fetchData() {
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/contents/status.txt";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "token " + GITHUB_TOKEN)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateErrorLog("Network Error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonData);
                        String base64Content = jsonObject.getString("content");
                        String decodedText = new String(Base64.decode(base64Content, Base64.DEFAULT));
                        
                        updateTableUI(decodedText);
                        updateErrorLog("Sync Successful.");
                    } catch (Exception e) {
                        updateErrorLog("Parsing Error: " + e.getMessage());
                    }
                } else {
                    updateErrorLog("GitHub API Error: " + response.code());
                }
            }
        });
    }

    private void updateTableUI(String data) {
        mainHandler.post(() -> {
            // Keep header row (index 0), remove the rest to avoid duplicate data on refresh
            int count = statusTable.getChildCount();
            for (int i = count - 1; i > 0; i--) {
                statusTable.removeViewAt(i);
            }

            String[] lines = data.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("Bot_ID")) continue;

                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    TableRow row = new TableRow(MainActivity.this);
                    row.setPadding(8, 12, 8, 12);

                    row.addView(createCell(parts[0].trim())); // Bot ID
                    row.addView(createCell(parts[1].trim())); // Status
                    row.addView(createCell(parts[2].trim())); // Priority
                    row.addView(createCell(parts[3].trim())); // Platform

                    statusTable.addView(row);
                }
            }
        });
    }

    private TextView createCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(4, 4, 4, 4);
        return tv;
    }

    private void updateErrorLog(String msg) {
        mainHandler.post(() -> {
            if (errorLogView != null) {
                errorLogView.setText(msg);
            }
        });
    }
}
