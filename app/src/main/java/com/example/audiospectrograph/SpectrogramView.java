package com.example.audiospectrograph;

/*
Key differences from traditional Java graphics:

View System:
-Extends View instead of JComponent
-More lightweight than Swing
-Designed for mobile/touch interfaces

Drawing System:
-Canvas instead of Graphics2D
-Paint class combines multiple Java2D concepts
-Simpler, more performance-focused API
-No complex transformations or effects

Threading:
-postInvalidate() for thread-safe updates
-Strict UI thread requirements
-Different event dispatch model

Layout:
-XML-based layout system
-Different sizing/measurement model
-AttributeSet for XML properties

Resource Management:
-Context-based resource access
-Different color handling
-Mobile-optimized drawing operations
 */

// Android-specific imports - very different from AWT/Swing
import android.content.Context;       // Android's context system - core for resource access
import android.graphics.Canvas;       // Android's drawing surface (not Graphics2D)
import android.graphics.Color;        // Color handling (different from java.awt.Color)
import android.graphics.Paint;        // Drawing attributes (replaces Java2D's various attributes)
import android.graphics.Bitmap;
import android.util.AttributeSet;     // For XML layout attributes
import android.util.Log;             // Android logging system
import android.view.View;            // Base class for UI components (not JComponent)
import java.util.ArrayList;

/**
 * Custom View class for spectral visualization
 * Extends View instead of JComponent/JPanel
 * Android Views are more lightweight than Swing components
 */
public class SpectrogramView extends View {
    private float lowFrequency = 300;  // use 300-3400 Hz
    private float highFrequency = 2000;  // 22050 is max frequency (44.1kHz/2)
    private float sampleRate = 44100;  // Should match your actual sample rate

    public enum ColorScheme {
        BLUE_TO_RED,    // original scheme
        BLACK_TO_RED    // new scheme
    }
    private ColorScheme currentColorScheme = ColorScheme.BLUE_TO_RED;
    private Bitmap waterfallBitmap;
    public void setColorScheme(ColorScheme scheme) {
        this.currentColorScheme = scheme;
        invalidate();
    }
    public ColorScheme getCurrentColorScheme() {
        return currentColorScheme;
    }
    public void setFrequencyRange(float low, float high) {
        this.lowFrequency = low;
        this.highFrequency = high;
        invalidate();
    }
    private AudioProcessor audioProcessor;

    public void setAudioProcessor(AudioProcessor processor) {
        this.audioProcessor = processor;
    }
    private float gainFactor = 2.0f;
    public void setGainFactor(float gain) {
        this.gainFactor = gain;
        if (audioProcessor != null) {
            audioProcessor.setGainFactor(gain);
        }
    }
    public float getGainFactor() {
        return gainFactor;
    }
    private static final String TAG = "SpectrogramView";  // Android logging tag
    // Paint replaces various Java2D concepts (Color, Stroke, etc.)
    private Paint paint;
    private float[] magnitudes;
    private ArrayList<float[]> history = new ArrayList<>();
    private static final int HISTORY_SIZE = 100;
    private int width, height;
    private boolean waterfallMode = false;

    /**
     * Constructor required for XML layout inflation
     * Different from Swing - Android typically creates views from XML
     * Context provides access to resources and system services
     * AttributeSet contains XML attributes
     */
    public SpectrogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initialize drawing tools
     * Paint is Android's all-in-one drawing attributes class
     * Replaces Color, Stroke, RenderingHints etc. from Java2D
     */
    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Toggle between display modes
     * invalidate() is Android's repaint() equivalent
     */
    public void setWaterfallMode(boolean enabled) {
        this.waterfallMode = enabled;
        if (!enabled) {
            history.clear();
        }
        invalidate();  // Request redraw
    }

    /**
     * Update visualization data
     * postInvalidate() is thread-safe version of invalidate()
     * Android UI updates must be thread-safe
     */
    public void updateMagnitudes(float[] mag) {
        if (mag == null) return;

        Log.d(TAG, "Updating magnitudes, length: " + mag.length);

        this.magnitudes = mag.clone();
        if (waterfallMode) {
            history.add(0, mag.clone());
            if (history.size() > HISTORY_SIZE) {
                history.remove(history.size() - 1);
            }
        }
        // postInvalidate() is safe to call from non-UI threads
        postInvalidate();
    }

    /**
     * Called when view size changes
     * Part of Android's measurement system
     * Different from Swing's component sizing
     */
    // Add cleanup in onSizeChanged to handle screen rotation/size changes
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        // Clear the old bitmap when size changes
        if (waterfallBitmap != null) {
            waterfallBitmap.recycle();
            waterfallBitmap = null;
        }
    }

    /**
     * Main drawing method - similar to paintComponent() in Swing
     * But uses Canvas instead of Graphics2D
     * Canvas is simpler than Graphics2D - mobile optimized
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK); // Clear background

        if (waterfallMode) {
            drawWaterfall(canvas);
        } else {
            drawRegularFFT(canvas);
        }
    }

    // Add cleanup in onDetachedFromWindow
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (waterfallBitmap != null) {
            waterfallBitmap.recycle();
            waterfallBitmap = null;
        }
    }

    /**
     * Draw regular FFT visualization
     * Canvas drawing is simpler than Java2D
     * No affine transforms or complex paths
     */
    private void drawRegularFFT(Canvas canvas) {
        if (magnitudes == null) return;

        // Calculate frequency bin size
        float binSize = sampleRate / (magnitudes.length * 2);

        // Find array indices for the frequency range
        int lowBin = Math.max(0, Math.round(lowFrequency / binSize));
        int highBin = Math.min(magnitudes.length - 1, Math.round(highFrequency / binSize));

        // Adjust bar width for visible range
        float barWidth = (float) width / (highBin - lowBin + 1);

        float maxHeight = height * 0.9f;
        paint.setStrokeWidth(Math.max(1f, barWidth - 1));

        // Only draw the selected frequency range
        for (int i = lowBin; i <= highBin; i++) {
            float magnitude = magnitudes[i];
            magnitude = Math.min(1.0f, Math.max(0.0f, magnitude));

            // Use getColorForMagnitude instead of direct HSV calculation
            paint.setColor(getColorForMagnitude(magnitude));

            float barHeight = magnitude * maxHeight;
            float x = (i - lowBin) * barWidth;

            canvas.drawLine(x, height, x, height - barHeight, paint);
        }

        // Draw white horizontal line at maximum theoretical height
        // e.g., how where magnitude = 1.0 would be (the maximum possible
        //       height before clipping)
        paint.setColor(Color.RED);
        paint.setStrokeWidth(2f);
        canvas.drawLine(0, height - maxHeight, width, height - maxHeight, paint);
    }

    /**
     * Draw waterfall visualization
     * Uses simple rectangles - Android optimizes these operations
     */
    private void drawWaterfall(Canvas canvas) {
        if (magnitudes == null) return;

        // Calculate frequency bin size
        float binSize = sampleRate / (magnitudes.length * 2);

        // Find array indices for the frequency range
        int lowBin = Math.max(0, Math.round(lowFrequency / binSize));
        int highBin = Math.min(magnitudes.length - 1, Math.round(highFrequency / binSize));
        int visibleBins = highBin - lowBin + 1;

        // Adjust pixel width based on visible frequency range
        float pixelWidth = (float) width / visibleBins;

        // Shift existing data up
        if (waterfallBitmap != null) {
            Canvas tempCanvas = new Canvas(waterfallBitmap);
            tempCanvas.drawBitmap(waterfallBitmap, 0, -3, paint);
        } else {
            waterfallBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        // Draw new line at the bottom
        Canvas tempCanvas = new Canvas(waterfallBitmap);
        for (int i = lowBin; i <= highBin; i++) {
            float magnitude = magnitudes[i];
            magnitude = Math.min(1.0f, Math.max(0.0f, magnitude));

            // Use getColorForMagnitude instead of direct HSV calculation
            paint.setColor(getColorForMagnitude(magnitude));

            float x = (i - lowBin) * pixelWidth;
            tempCanvas.drawRect(
                    x,
                    height - 1,
                    x + pixelWidth,
                    height,
                    paint
            );
        }

        // Draw the waterfall bitmap
        canvas.drawBitmap(waterfallBitmap, 0, 0, paint);
    }

    /**
     * Helper method for color calculation
     * Android uses a different color model than AWT
     */
    private int getColorForMagnitude(float magnitude) {
        switch (currentColorScheme) {
            case BLUE_TO_RED:
                // Original color scheme
                float hue = (1.0f - magnitude) * 240f;
                return Color.HSVToColor(new float[]{hue, 1.0f, 1.0f});

            case BLACK_TO_RED:
                // New color scheme
                if (magnitude < 0.5f) {
                    // Black to Purple
                    int purple = (int)(magnitude * 2 * 255);
                    return Color.rgb(purple, 0, purple);
                } else {
                    // Purple to Red
                    float t = (magnitude - 0.5f) * 2;
                    int purple = (int)((1 - t) * 255);
                    return Color.rgb(255, 0, purple);
                }

            default:
                return Color.WHITE;
        }
    }
}