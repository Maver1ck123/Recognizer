package com.example.recognizer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;

public class HistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private ArrayAdapter<String> historyAdapter;
    private ArrayList<String> searchHistory;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyListView = findViewById(R.id.historyListView);
        sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        searchHistory = getSearchHistory(); // Fetch initial history
        historyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, searchHistory);
        historyListView.setAdapter(historyAdapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.wp));
        }

        if (getIntent().hasExtra("newEntry")) {
            String newEntry = getIntent().getStringExtra("newEntry");
            addToSearchHistory(newEntry);
        }


        // Set up the "Clear History" button
        Button clearHistoryButton = findViewById(R.id.clearHistoryButton);
        clearHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearSearchHistory();
            }
        });
    }

    private ArrayList<String> getSearchHistory() {
        // Retrieve the search history from SharedPreferences
        String historyString = sharedPreferences.getString("searchHistory", "");

        // Split the historyString and filter out empty entries
        String[] historyArray = historyString.split(",");
        ArrayList<String> historyList = new ArrayList<>();
        for (String entry : historyArray) {
            if (!entry.trim().isEmpty()) {
                historyList.add(entry);
            }
        }

        return historyList;
    }


    public void addToSearchHistory(String entry) {
        // Check if the new entry is not empty
        if (!TextUtils.isEmpty(entry.trim())) {
            // Add the new entry to the search history
            searchHistory.add(entry);

            // Save the updated search history to SharedPreferences
            saveSearchHistory();

            // Notify the adapter that the data set has changed
            historyAdapter.notifyDataSetChanged();
        }
    }

    private void saveSearchHistory() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String historyString = TextUtils.join(",", searchHistory);
        editor.putString("searchHistory", historyString);
        editor.apply();
    }

    private void clearSearchHistory() {
        // Clear the search history
        searchHistory.clear();

        // Save the updated search history to SharedPreferences
        saveSearchHistory();

        // Notify the adapter that the data set has changed
        historyAdapter.notifyDataSetChanged();
    }
}
