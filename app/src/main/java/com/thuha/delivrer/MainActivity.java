package com.thuha.delivrer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.Api;

public class MainActivity<Rider> extends AppCompatActivity {
    private Button nRider, nClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nRider = findViewById(R.id.rider);
        nClient = findViewById(R.id.client);
        nRider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RiderLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
            });
        nClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ClientLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }
}
