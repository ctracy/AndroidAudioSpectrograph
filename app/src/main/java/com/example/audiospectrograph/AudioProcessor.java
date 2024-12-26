package com.example.audiospectrograph;

/*
Key Android-specific concepts highlighted in the comments:

Threading model:
-Android has a strict single-thread UI model
-Heavy processing must be off the main thread
-UI updates must be posted back to main thread

Resource management:
-Explicit start/stop required
-Must release resources properly
-No reliance on garbage collection for cleanup

Error handling:
-Android uses error codes rather than exceptions in many cases
-Logging system is more structured than desktop Java
-Must check component states explicitly

Audio system:
-Different from JavaSound API
-More low-level control required
-Buffer management is more critical
-Explicit state management needed

UI updates:
-Must use post() mechanism
-Can't update UI directly from background thread
-Thread safety is crucial
 */

// Android-specific imports for audio handling
import android.media.AudioFormat;     // Defines audio data formats
import android.media.AudioRecord;      // Low-level audio input access
import android.media.MediaRecorder;    // High-level recording facilities
import android.util.Log;              // Android's logging system, better than System.out.println
import org.jtransforms.fft.FloatFFT_1D;

/**
 * Handles real-time audio capture and FFT processing for spectral analysis.
 *
 * Key Android concepts:
 * - AudioRecord: Low-level audio capture API (unlike old Java Sound API)
 * - Handler/Thread management: Android UI has a single main thread
 * - Activity lifecycle awareness: Must handle pause/resume
 */

public class AudioProcessor {
    // TAG is Android convention for logging identification
    private static final String TAG = "AudioProcessor";

    // Audio configuration constants
    // 44.1kHz is standard audio sampling rate
    private static final int SAMPLE_RATE = 44100;
    private static final int FFT_SIZE = 2048;

    // Android-specific audio format constants
    // These differ from old JavaSound - they're optimized for mobile
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    // Using ENCODING_PCM_FLOAT instead of traditional 16-bit PCM
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT;

    // Android's audio capture component - different from JavaSound's TargetDataLine
    private AudioRecord audioRecord;
    private boolean isRunning = false;
    private Thread processingThread;
    private FloatFFT_1D fft;
    private float[] buffer;
    private float[] magnitudes;
    private float gainFactor = 2.0f;
    // Reference to UI component - must be careful about threading
    private SpectrogramView spectrogramView;
    public void setGainFactor(float gain) {
        this.gainFactor = gain;
    }

    /**
     * Constructor initializes audio capture system.
     * Android requires careful resource management and permission checking.
     */
    public AudioProcessor(SpectrogramView view) {
        this.spectrogramView = view;
        this.fft = new FloatFFT_1D(FFT_SIZE);
        this.buffer = new float[FFT_SIZE];

        // Android requires explicit buffer size calculation
        // Unlike JavaSound, which handles this internally
        int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT);

        // Android logging is structured and filterable
        // Use Log.d for debug, Log.e for errors, etc.
        //Log.d(TAG, "Minimum buffer size: " + minBufferSize);

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Error getting min buffer size");
            return;
        }

        // Double buffer size for safety - Android audio system is less forgiving
        // than desktop Java about buffer overruns
        int bufferSize = Math.max(minBufferSize * 2, FFT_SIZE * 2);

        try {
            // AudioRecord configuration is more complex than JavaSound
            // Requires explicit source selection and buffer size management
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,  // Android-specific audio source
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize);

            // Must explicitly check initialization - Android won't throw exceptions
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize");
                return;
            }
            Log.d(TAG, "AudioRecord initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating AudioRecord", e);
        }
    }
    /**
     * Starts audio capture and processing.
     * Unlike JavaSound, Android requires explicit start/stop management
     * and careful state checking.
     */
    public void start() {
        // Android components need explicit state checking
        if (isRunning || audioRecord == null) return;

        // Double-check initialization state - Android audio system can be finicky
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized");
            return;
        }

        isRunning = true;
        try {
            // Explicit recording start - different from JavaSound's automatic flow
            audioRecord.startRecording();
            Log.d(TAG, "Started recording");

            // Android best practice: Use separate thread for audio processing
            // Main thread (UI thread) must stay responsive
            processingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    process();
                }
            });
            processingThread.start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting AudioRecord", e);
            isRunning = false;
        }
    }

    /**
     * Stops audio capture and processing.
     * Android requires explicit resource cleanup to prevent memory leaks
     * and system resource exhaustion.
     */
    public void stop() {
        isRunning = false;
        if (processingThread != null) {
            try {
                // Clean thread shutdown - important for Android resource management
                processingThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping processing thread", e);
            }
        }
        if (audioRecord != null) {
            try {
                // Android requires explicit cleanup of audio resources
                // Unlike JavaSound which has garbage collection safety nets
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord", e);
            }
        }
    }

    /**
     * Main processing loop for audio analysis.
     * Runs on separate thread to avoid blocking UI.
     * Android's single-thread UI model makes this separation crucial.
     */
    private void process() {
        Log.d(TAG, "Processing thread started");
        float[] tempBuffer = new float[FFT_SIZE];

        while (isRunning) {
            try {
                // Android's audio read is different from JavaSound
                // READ_BLOCKING is similar to old Java blocking reads
                int readSize = audioRecord.read(tempBuffer, 0, FFT_SIZE, AudioRecord.READ_BLOCKING);

                // Android requires explicit error checking on reads
                if (readSize == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Invalid read operation");
                    continue;
                }
                if (readSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Bad value on read");
                    continue;
                }
                if (readSize != FFT_SIZE) {
                    Log.w(TAG, "Unexpected read size: " + readSize);
                    continue;
                }

                // Standard array copy - same as desktop Java
                System.arraycopy(tempBuffer, 0, buffer, 0, FFT_SIZE);

                // Debug check for signal presence
                float sum = 0;
                for (float sample : buffer) {
                    sum += Math.abs(sample);
                }
                //Log.d(TAG, "Buffer sum: " + sum);

                // Standard DSP operations - same as desktop Java
                // Apply Hanning window
                for (int i = 0; i < FFT_SIZE; i++) {
                    buffer[i] *= 0.5 * (1 - Math.cos(2*Math.PI*i / (FFT_SIZE-1)));
                }

                // FFT processing - using JTransforms library
                fft.realForward(buffer);

                // Calculate magnitude spectrum
                magnitudes = new float[FFT_SIZE/2];
                float maxMagnitude = 0;
                for (int i = 0; i < FFT_SIZE/2; i++) {
                    float re = buffer[2*i];
                    float im = buffer[2*i+1];
                    // This is where the gain actually affects the signal
                    //magnitudes[i] = (float) Math.sqrt(re*re + im*im) * gainFactor;
                    magnitudes[i] = (float) Math.sqrt(re*re + im*im);
                    maxMagnitude = Math.max(maxMagnitude, magnitudes[i]);
                }

                // Normalize, apply gain, and scale the spectrum
                if (maxMagnitude > 0) {
                    for (int i = 0; i < FFT_SIZE/2; i++) {
                        magnitudes[i] = magnitudes[i] / maxMagnitude;
                        // Apply gain after normalization
                        magnitudes[i] = Math.min(1.0f, magnitudes[i] * gainFactor);
                        // Log scaling for better visualization
                        magnitudes[i] = (float) (Math.log10(1 + magnitudes[i]) / Math.log10(100));
                    }
                }

                //Log.d(TAG, "Max magnitude: " + maxMagnitude);
                //Log.d(TAG, "Gain factor: " + gainFactor);

                // CRITICAL ANDROID UI UPDATE PATTERN:
                // Must use post() to update UI from background thread
                // Android enforces single-thread UI model strictly
                if (maxMagnitude > 0) {
                    final float[] finalMagnitudes = magnitudes.clone();
                    spectrogramView.post(new Runnable() {
                        @Override
                        public void run() {
                            // This code runs on UI thread
                            spectrogramView.updateMagnitudes(finalMagnitudes);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in processing loop", e);
            }
        }
        Log.d(TAG, "Processing thread stopped");
    }
}
