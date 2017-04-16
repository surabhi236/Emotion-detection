package com.google.facetracker;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.util.List;

public class EmotionDetection extends Activity implements Detector.ImageListener, CameraDetector.CameraEventListener
{

    final String LOG_TAG = "CameraDetectorDemo";

    Button startSDKButton;
    TextView smileTextView;
    TextView angerTextView;
    TextView fearTextView;
    TextView surpriseTextView;
    TextView contemptTextView;
    TextView disgustTextView;
    TextView focusTextView;
    TextView sadnessTextView;

    SurfaceView cameraPreview;

    boolean isSDKStarted;

    RelativeLayout mainLayout;

    CameraDetector detector;

    int previewWidth = 0;
    int previewHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_detection);

        smileTextView = (TextView) findViewById(R.id.smile_textview);
        contemptTextView = (TextView) findViewById(R.id.contempt_textview);
        fearTextView = (TextView) findViewById(R.id.fear_textview);
        angerTextView = (TextView) findViewById(R.id.anger_textview);
        disgustTextView = (TextView) findViewById(R.id.disgust_textview);
        focusTextView = (TextView) findViewById(R.id.focus_textview);
        sadnessTextView = (TextView) findViewById(R.id.sadness_textview);

        isSDKStarted = getIntent().getExtras().getBoolean("sdkStarted");

        startSDKButton = (Button) findViewById(R.id.sdk_start_button);
        startSDKButton.setText("stop analysis");
        startSDKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSDKStarted) {
                    isSDKStarted = false;
                    stopDetector();

                    startSDKButton.setText("start analysis");
                } else {
                    isSDKStarted = true;
                    startDetector();
                    startSDKButton.setText("stop analysis");
                }
            }
        });


        //We create a custom SurfaceView that resizes itself to match the aspect ratio of the incoming camera frames
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        cameraPreview = new SurfaceView(this) {
            @Override
            public void onMeasure(int widthSpec, int heightSpec) {
                int measureWidth = MeasureSpec.getSize(widthSpec);
                int measureHeight = MeasureSpec.getSize(heightSpec);
                int width;
                int height;
                if (previewHeight == 0 || previewWidth == 0) {
                    width = measureWidth;
                    height = measureHeight;
                } else {
                    float viewAspectRatio = (float)measureWidth/measureHeight;
                    float cameraPreviewAspectRatio = (float) previewWidth/previewHeight;

                    if (cameraPreviewAspectRatio > viewAspectRatio) {
                        width = measureWidth;
                        height =(int) (measureWidth / cameraPreviewAspectRatio);
                    } else {
                        width = (int) (measureHeight * cameraPreviewAspectRatio);
                        height = measureHeight;
                    }
                }
                setMeasuredDimension(width,height);
            }
        };
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE);
        cameraPreview.setLayoutParams(params);
        mainLayout.addView(cameraPreview,0);
        cameraPreview.setVisibility(View.VISIBLE);

        detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, cameraPreview);
        detector.setDetectSmile(true);
        detector.setDetectAnger(true);

        detector.setDetectDisgust(true);
        detector.setDetectFear(true);
        detector.setDetectSadness(true);
        detector.setDetectAttention(true);
        detector.setDetectContempt(true);
        detector.setImageListener(this);
        detector.setOnCameraEventListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSDKStarted) {
            startDetector();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDetector();
    }

    void startDetector() {
        if (!detector.isRunning()) {
            detector.start();
        }
    }

    void stopDetector() {
        if (detector.isRunning()) {
            detector.stop();
        }
    }

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v) {
        if (list == null)
            return;
        if (list.size() == 0) {
            smileTextView.setText("no face detected");
           angerTextView.setText("");
            fearTextView.setText("");
            disgustTextView.setText("");
            contemptTextView.setText("");
            focusTextView.setText("");
            sadnessTextView.setText("");
        } else {
            Face face = list.get(0);
            smileTextView.setText(String.format("SMILE\n%.2f",face.expressions.getSmile()));

            angerTextView.setText(String.format("ANGER\n%.2f",face.emotions.getAnger()));
            sadnessTextView.setText(String.format("SADNESS\n%.2f",face.emotions.getSadness()));
            fearTextView.setText(String.format("FEAR\n%.2f",face.emotions.getFear()));
            disgustTextView.setText(String.format("DISGUST\n%.2f",face.emotions.getDisgust()));
            contemptTextView.setText(String.format("CONTEMPT\n%.2f",face.emotions.getContempt()));

            focusTextView.setText(String.format("FOCUS\n%.2f",face.expressions.getAttention()));
        }
    }
    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onCameraSizeSelected(int width, int height, Frame.ROTATE rotate) {
        if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW) {
            previewWidth = height;
            previewHeight = width;
        } else {
            previewHeight = height;
            previewWidth = width;
        }
        cameraPreview.requestLayout();
    }
}



