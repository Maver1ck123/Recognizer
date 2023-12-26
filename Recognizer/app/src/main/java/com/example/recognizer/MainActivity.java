package com.example.recognizer;

import android.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.os.Build;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private CameraSource cameraSource;
    private SurfaceView cameraPreview;
    private TextView resultTextView;
    private Button saveContactButton;

    private String ph, email, site;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;


    public void selectImage(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 456); // Use any unique request code
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 456 && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            Intent imageRecognitionIntent = new Intent(this, ImageRecognitionActivity.class);
            imageRecognitionIntent.putExtra("imageUri", selectedImageUri.toString());
            startActivity(imageRecognitionIntent);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = findViewById(R.id.cameraPreview);
        resultTextView = findViewById(R.id.resultTextView);
        saveContactButton = findViewById(R.id.saveContactButton);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.wp));
        }


        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setAutoFocusEnabled(true)
                    .setRequestedPreviewSize(1920, 1080)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    try {
                        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            cameraSource.start(cameraPreview.getHolder());
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA}, 123);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                    cameraSource.stop();
                }
            });

            saveContactButton.setOnClickListener(view -> saveContact(getPhoneNumber(), getEmail(), getWebsite()));

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    SparseArray<TextBlock> items = detections.getDetectedItems();
                    StringBuilder phoneNumberBuilder = new StringBuilder();
                    StringBuilder emailBuilder = new StringBuilder();
                    StringBuilder websiteBuilder = new StringBuilder();

                    for (int i = 0; i < items.size(); ++i) {
                        TextBlock item = items.valueAt(i);
                        String text = item.getValue().trim();

                        // Use regex to identify phone numbers
                        Pattern phonePattern = Pattern.compile("\\b\\d{10}\\b");
                        Matcher phoneMatcher = phonePattern.matcher(text);
                        while (phoneMatcher.find()) {
                            phoneNumberBuilder.append(phoneMatcher.group()).append("\n");
                        }

                        // Use regex to identify emails
                        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
                        Matcher emailMatcher = emailPattern.matcher(text);
                        while (emailMatcher.find()) {
                            emailBuilder.append(emailMatcher.group()).append("\n");
                        }

                        // Use regex to identify website links
                        Pattern websitePattern = Pattern.compile("\\b(?:https?|ftp):\\/\\/(?:www\\.)?[\\w-]+(?:\\.[a-z]{2,})+(?:\\/[^\\s]*)?\\b");
                        Matcher websiteMatcher = websitePattern.matcher(text);
                        while (websiteMatcher.find()) {
                            websiteBuilder.append(websiteMatcher.group()).append("\n");
                        }
                    }

                    ph = phoneNumberBuilder.toString();
                    email = emailBuilder.toString();
                    site = websiteBuilder.toString();

                    // Display the identified information
                    runOnUiThread(() -> {
                        resultTextView.setText(
                                "Phone Numbers:\n" + phoneNumberBuilder.toString() +
                                        "\n\nEmails:\n" + emailBuilder.toString() +
                                        "\n\nWebsites:\n" + websiteBuilder.toString());

                    });
                }
            });
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, android.R.string.ok, android.R.string.cancel);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item ->

        {
            if(item.getItemId() == R.id.menu_history)
                openHistoryActivity();

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


    }

    private void openHistoryActivity() {
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        startActivity(historyIntent);
    }


    private String getPhoneNumber() {
        return ph;
    }

    private String getEmail() {
        return email;
    }

    private String getWebsite() {
        return site;
    }


    private void saveContact(String phoneNumber, String email, String website) {
        try {
            ContentResolver contentResolver = getContentResolver();

            ContentValues contactValues = new ContentValues();
            Uri rawContactUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contactValues);
            long rawContactId = Long.parseLong(rawContactUri.getLastPathSegment());

            if (!phoneNumber.isEmpty()) {
                ContentValues phoneValues = new ContentValues();
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID, rawContactId);
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues);
            }

            // Add email
            if (!email.isEmpty()) {
                ContentValues emailValues = new ContentValues();
                emailValues.put(ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID, rawContactId);
                emailValues.put(ContactsContract.CommonDataKinds.Email.ADDRESS, email);
                emailValues.put(ContactsContract.CommonDataKinds.Email.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                contentResolver.insert(ContactsContract.Data.CONTENT_URI, emailValues);
            }

// Add website
            if (!website.isEmpty()) {
                ContentValues websiteValues = new ContentValues();
                websiteValues.put(ContactsContract.CommonDataKinds.Website.RAW_CONTACT_ID, rawContactId);
                websiteValues.put(ContactsContract.CommonDataKinds.Website.URL, website);
                websiteValues.put(ContactsContract.CommonDataKinds.Website.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
                contentResolver.insert(ContactsContract.Data.CONTENT_URI, websiteValues);
            }

            // Show a toast message indicating that the contact is saved
            if (!website.isEmpty() || !email.isEmpty() || !phoneNumber.isEmpty()) {
                Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
                sendToHistoryActivity(phoneNumber + "\n" + email + "\n" + website);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error saving contact", e);
            e.printStackTrace();
        }
    }

    private void sendToHistoryActivity(String entry) {
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        historyIntent.putExtra("newEntry", entry);
        startActivity(historyIntent);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 123 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                try {
                    cameraSource.start(cameraPreview.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}
