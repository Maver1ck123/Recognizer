package com.example.recognizer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageRecognitionActivity extends AppCompatActivity {

    private ImageView selectedImageView;
    private TextView recognizedInfoTextView;
    private Button saveContactButton;
    private ImageButton closeButton;

    private Bitmap bitmap;

    private String ph, email, site;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_recognition);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.wp));
        }

        selectedImageView = findViewById(R.id.selectedImageView);
        recognizedInfoTextView = findViewById(R.id.recognizedInfoTextView);
        saveContactButton = findViewById(R.id.saveContactButton);
        closeButton = findViewById(R.id.closeButton);

        String imageUriString = getIntent().getStringExtra("imageUri");
        Uri imageUri = Uri.parse(imageUriString);
        selectedImageView.setImageURI(imageUri);

        performTextRecognition(imageUri);

        saveContactButton.setOnClickListener(view -> saveContact(getPhoneNumberFromText(), getEmailFromText(), getWebsiteFromText()));

        closeButton.setOnClickListener(view -> finish());
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        try {
            ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                Bitmap imageBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                return imageBitmap;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void performTextRecognition(Uri imageUri) {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
            return;
        }

        Bitmap bitmap = getBitmapFromUri(imageUri);

        Frame imageFrame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<TextBlock> items = textRecognizer.detect(imageFrame);
        StringBuilder phoneNumberBuilder = new StringBuilder();
        StringBuilder emailBuilder = new StringBuilder();
        StringBuilder websiteBuilder = new StringBuilder();

        StringBuilder recognizedTextBuilder = new StringBuilder();

        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            String text = item.getValue().trim();

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

            recognizedTextBuilder.append(text).append("\n");
        }

        ph=phoneNumberBuilder.toString();
        email=emailBuilder.toString();
        site=websiteBuilder.toString();

        // Update the UI with the recognized information
        runOnUiThread(() -> {
            recognizedInfoTextView.setText(
                    "Phone Numbers:\n" + phoneNumberBuilder.toString() +
                            "\n\nEmails:\n" + emailBuilder.toString() +
                            "\n\nWebsites:\n" + websiteBuilder.toString());
        });
    }

    // Add methods to extract phone numbers, emails, and websites using regex or other methods
    private String getPhoneNumberFromText() {
        // Implement extraction logic
        return ph;
    }

    private String getEmailFromText() {
        // Implement extraction logic
        return email;
    }

    private String getWebsiteFromText() {
        // Implement extraction logic
        return site;
    }

    private void saveContact(String phoneNumber, String email, String website) {
        try {
            ContentResolver contentResolver = getContentResolver();

            ContentValues contactValues = new ContentValues();
            Uri rawContactUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contactValues);
            long rawContactId = Long.parseLong(rawContactUri.getLastPathSegment());

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                ContentValues phoneValues = new ContentValues();
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID, rawContactId);
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues);
            }

            // Add email
            if (email != null && !email.isEmpty()) {
                ContentValues emailValues = new ContentValues();
                emailValues.put(ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID, rawContactId);
                emailValues.put(ContactsContract.CommonDataKinds.Email.ADDRESS, email);
                emailValues.put(ContactsContract.CommonDataKinds.Email.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                contentResolver.insert(ContactsContract.Data.CONTENT_URI, emailValues);
            }

            // Add website
            if (website != null && !website.isEmpty()) {
                ContentValues websiteValues = new ContentValues();
                websiteValues.put(ContactsContract.CommonDataKinds.Website.RAW_CONTACT_ID, rawContactId);
                websiteValues.put(ContactsContract.CommonDataKinds.Website.URL, website);
                websiteValues.put(ContactsContract.CommonDataKinds.Website.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
                contentResolver.insert(ContactsContract.Data.CONTENT_URI, websiteValues);
            }

            // Show a toast message indicating that the contact is saved
            if (website != null || email != null || phoneNumber != null) {
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
        sendBroadcast(historyIntent);
    }

}