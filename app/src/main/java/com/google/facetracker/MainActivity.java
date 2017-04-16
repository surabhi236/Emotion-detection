package com.google.facetracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button realTimeDetectionOptionButton;
    Button imageDetectionOptionButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        realTimeDetectionOptionButton = (Button) findViewById(R.id.realTimeDetectionOptionButton);
        imageDetectionOptionButton = (Button) findViewById(R.id.imageDetectionOptionButton);

        realTimeDetectionOptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FaceDetectionActivity.class);
                startActivity(intent);
            }
        });

        imageDetectionOptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(getApplicationContext(), ImageDetection.class);
                startActivity(gallery);
            }
        });

    }

}
