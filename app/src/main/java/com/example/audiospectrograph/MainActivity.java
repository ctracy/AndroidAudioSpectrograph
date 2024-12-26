package com.example.audiospectrograph;

// Android-specific imports - very different from traditional Java
import android.Manifest;                // Android's permission system
import android.content.pm.PackageManager; // For checking/requesting permissions
import android.os.Bundle;               // For passing data between components
import android.widget.Button;           // Android UI component (not Swing/AWT)
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;               // Base class for UI components
// AndroidX is the modern support library for backward compatibility
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/*
Key Android concepts different from traditional Java:

Activity Lifecycle:
-Activities have a complex lifecycle (onCreate, onDestroy, etc.)
-Different from traditional Java application lifecycle
-System can destroy/recreate activities at any time

UI Management:
-UI defined in XML, not programmatically like Swing/AWT
-findViewById instead of new Component()
-Different component hierarchy and event system

Permissions:
-Runtime permission system (since Android 6.0)
-Must explicitly request sensitive permissions
-Different from traditional Java security model

Resource Management:
-R class for resource references
-Resource IDs for everything (layouts, strings, etc.)
-Context-aware components

Component Architecture:
-Activities instead of frames/windows
-Different threading model
-System-managed lifecycle

 */

/**
 * MainActivity extends AppCompatActivity, not JFrame like old Java GUI apps.
 * Activities are fundamental Android components representing screens.
 * AppCompatActivity provides backward compatibility for newer Android features.
 */
public class MainActivity extends AppCompatActivity {
    // Constants for permission handling - Android 6.0+ requires runtime permissions
    private static final int PERMISSION_REQUEST_CODE = 1;

    // Component references - similar to traditional Java but Android-specific types
    private AudioProcessor audioProcessor;
    private SpectrogramView spectrogramView;
    private Button toggleButton;
    private boolean isWaterfallMode = false;

    /**
     * onCreate is like a constructor but for Android Activities
     * It's part of the Activity lifecycle - called when Activity is created
     * Similar to main() in traditional Java, but for this screen only
     *
     * savedInstanceState contains any saved state if app was killed and restored
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Always call super first in lifecycle methods
        super.onCreate(savedInstanceState);

        // remove the app's title bar/action bar, it just says the title and wastes space...
        // can also be done in res/values/styles.xml
        // e.g. Theme.AppCompat.NoActionBar
        getSupportActionBar().hide();

        // Sets the UI layout from XML - very different from Swing/AWT
        // R.layout.activity_main refers to res/layout/activity_main.xml
        setContentView(R.layout.activity_main);

        // findViewById replaces direct component creation in traditional Java
        // Android UI is typically defined in XML, not in code
        spectrogramView = findViewById(R.id.spectrogramView);
        toggleButton = findViewById(R.id.toggleButton);
        audioProcessor = new AudioProcessor(spectrogramView);
        // Add this line to pass the reference to the AudioProcessor instance
        // this is currently needed to set the gain
        // user adjusts gain, Gain value is passed to SpectrogramView
        // SpectrogramView passes it to AudioProcessor
        // AudioProcessor uses it to multiply the signal magnitudes
        // The amplified magnitudes are then normalized and displayed
        spectrogramView.setAudioProcessor(audioProcessor);

        // Event handling similar to Swing but using Android-specific listeners
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isWaterfallMode = !isWaterfallMode;
                spectrogramView.setWaterfallMode(isWaterfallMode);
                toggleButton.setText(isWaterfallMode ? "Regular" : "Waterfall");
            }
        });

        // Frequency range button
        ImageButton rangeButton = findViewById(R.id.frequencyRangeButton);
        rangeButton.setOnClickListener(v -> showFrequencyRangeDialog());

        // color scheme button
        ImageButton colorSchemeButton = findViewById(R.id.colorSchemeButton);
        colorSchemeButton.setOnClickListener(v -> {
            // Toggle between schemes
            if (spectrogramView.getCurrentColorScheme() == SpectrogramView.ColorScheme.BLUE_TO_RED) {
                spectrogramView.setColorScheme(SpectrogramView.ColorScheme.BLACK_TO_RED);
                // converted to ImageButtons, will have to display elsewhere
                //colorSchemeButton.setText("Blue-Red");
            } else {
                spectrogramView.setColorScheme(SpectrogramView.ColorScheme.BLUE_TO_RED);
                // converted to ImageButtons, will have to display elsewhere
                //colorSchemeButton.setText("Black-Red");
            }
        });

        // the other buttons could be broken out like this as well
        // e.g. setupDisplayModeButton, setupColorSchemeButton, other button setup code...
        setupGainControl();

        // Android 6.0+ requires explicit permission requests at runtime
        if (checkPermission()) {
            startAudioProcessing();
        } else {
            requestPermissions();
        }
    }

    private void showFrequencyRangeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.frequency_range_dialog, null);
        EditText lowFreqInput = dialogView.findViewById(R.id.lowFreqInput);
        EditText highFreqInput = dialogView.findViewById(R.id.highFreqInput);

        new AlertDialog.Builder(this)
                .setTitle("Set Frequency Range")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    try {
                        float lowFreq = Float.parseFloat(lowFreqInput.getText().toString());
                        float highFreq = Float.parseFloat(highFreqInput.getText().toString());

                        // Validate input
                        if (lowFreq >= 0 && highFreq > lowFreq && highFreq <= 22050) {
                            spectrogramView.setFrequencyRange(lowFreq, highFreq);
                        } else {
                            Toast.makeText(this, "Invalid frequency range", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupGainControl() {
        ImageButton gainButton = findViewById(R.id.gainButton);
        gainButton.setOnClickListener(v -> showGainDialog());
    }

    private void showGainDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.gain_dialog, null);

        SeekBar seekBar = dialogView.findViewById(R.id.gainSeekBar);
        TextView valueText = dialogView.findViewById(R.id.gainValue);

        // Convert current gain to seekbar value (0-100)
        float currentGain = spectrogramView.getGainFactor();
        int progress = (int)((currentGain - 1) * 10);
        seekBar.setProgress(progress);
        valueText.setText(String.format("Gain: %.1f", currentGain));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert progress (0-100) to gain value (1.0-11.0)
                float gain = 1.0f + (progress / 10.0f);
                valueText.setText(String.format("Gain: %.1f", gain));
                spectrogramView.setGainFactor(gain);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setView(dialogView)
                .setTitle("Adjust Gain")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Handles Android's runtime permission system
     * Traditional Java assumed all permissions were granted at install time
     * Modern Android requires explicit user approval for sensitive permissions
     */
    private void requestPermissions() {
        // Check if we already have permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission if we don't have it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted
            startAudioProcessing();
        }
    }

    /**
     * Callback for permission request result
     * Part of Android's permission system - no equivalent in traditional Java
     * Called automatically by Android when user responds to permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioProcessing();
            }
        }
    }

    /**
     * Helper method to start audio processing
     * Checks for null because Android components can be destroyed/recreated
     */
    private void startAudioProcessing() {
        if (audioProcessor != null) {
            audioProcessor.start();
        }
    }

    /**
     * Part of Activity lifecycle - called when Activity is being destroyed
     * Critical for cleanup - Android manages app lifecycle differently from desktop Java
     * Must release resources explicitly
     */
    @Override
    protected void onDestroy() {
        audioProcessor.stop();
        // Always call super last in lifecycle methods
        super.onDestroy();
    }
}