package com.yi.cmsdashboard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.Button;
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
    private TextView tvStatus;
    private TextView tvErrorLogs;
    private Button btnRefresh;
    private Handler autoRefreshHandler;
    private Runnable refreshRunnable;
    private OkHttpClient client;
    
    private final String GITHUB_TOKEN = "YOUR_CMS_TOKEN";
    private final String OWNER = "YOUR_GITHUB_ID";
    private final String REPO = "YOUR_REPO_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvErrorLogs = findViewById(R.id.tvErrorLogs);
        btnRefresh = findViewById(R.id.btnRefresh);
        client = new OkHttpClient();

        btnRefresh.setOnClickListener(v -> fetchCMSData());

        autoRefreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchCMSData();
                autoRefreshHandler.postDelayed(this, 300000);
            }
        };
        autoRefreshHandler.post(refreshRunnable);
    }

    private void fetchCMSData() {
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/contents/status.txt";
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "token " + GITHUB_TOKEN)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvErrorLogs.setText("Network/API Error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonData);
                        String encodedContent = jsonObject.getString("content");
                        byte[] decodedBytes = Base64.decode(encodedContent, Base64.DEFAULT);
                        String statusText = new String(decodedBytes);

                        runOnUiThread(() -> {
                            tvStatus.setText(statusText);
                            tvErrorLogs.setText("Sync Successful.");
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> tvErrorLogs.setText("Data Parsing Error: " + e.getMessage()));
                    }
                } else {
                    runOnUiThread(() -> tvErrorLogs.setText("GitHub API Blocked / Error Code: " + response.code()));
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoRefreshHandler.removeCallbacks(refreshRunnable);
    }
}
