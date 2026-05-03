package com.example.cosmos_discovery.ui.admin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.Collections;
import java.util.List;

/**
 * Simple vertical bar chart for the admin dashboard.
 * Draws white semi-transparent bars on whatever background the parent provides.
 * Call {@link #setEntries(List)} to supply data; the view re-draws itself.
 */
public class BarChartView extends View {

    public static class BarEntry {
        public final String label;
        public final int    value;

        public BarEntry(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }

    private List<BarEntry> mEntries = Collections.emptyList();

    private final Paint mBarPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mEmptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGridPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float sp = getResources().getDisplayMetrics().scaledDensity;

        // Dark purple bars (#803366 = color_primary)
        mBarPaint.setColor(Color.parseColor("#803366"));

        mLabelPaint.setColor(Color.parseColor("#999999")); // color_text_hint
        mLabelPaint.setTextSize(10 * sp);
        mLabelPaint.setTextAlign(Paint.Align.CENTER);

        mValuePaint.setColor(Color.parseColor("#000000")); // color_text_secondary
        mValuePaint.setTextSize(10 * sp);
        mValuePaint.setTextAlign(Paint.Align.CENTER);
        mValuePaint.setTypeface(Typeface.DEFAULT_BOLD);

        mEmptyPaint.setColor(Color.parseColor("#999999")); // color_text_hint
        mEmptyPaint.setTextSize(12 * sp);
        mEmptyPaint.setTextAlign(Paint.Align.CENTER);

        mGridPaint.setColor(Color.parseColor("#CAC4D0")); // color_chip_stroke — light gray
        mGridPaint.setAlpha(120);
        mGridPaint.setStrokeWidth(1f);
    }

    public void setEntries(List<BarEntry> entries) {
        mEntries = entries != null ? entries : Collections.emptyList();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float dp = getResources().getDisplayMetrics().density;
        int   w  = getWidth();
        int   h  = getHeight();

        if (mEntries.isEmpty()) {
            canvas.drawText("No data for last 30 days", w / 2f, h / 2f + mEmptyPaint.getTextSize() / 3, mEmptyPaint);
            return;
        }

        int   count      = mEntries.size();
        float labelAreaH = 18 * dp;
        float valueAreaH = 16 * dp;
        float barAreaH   = h - labelAreaH - valueAreaH - 4 * dp;
        float barAreaTop = valueAreaH + 2 * dp;
        float barAreaBot = barAreaTop + barAreaH;

        // Draw 3 subtle horizontal grid lines
        for (int i = 1; i <= 3; i++) {
            float y = barAreaTop + barAreaH * (1f - i / 4f);
            canvas.drawLine(0, y, w, y, mGridPaint);
        }

        float slotW = (float) w / count;
        float barW  = slotW * 0.55f;
        float cornerR = 4 * dp;

        // Find max value
        int maxVal = 1;
        for (BarEntry e : mEntries) {
            if (e.value > maxVal) maxVal = e.value;
        }

        for (int i = 0; i < count; i++) {
            BarEntry entry = mEntries.get(i);
            float cx    = slotW * i + slotW / 2f;
            float left  = cx - barW / 2f;
            float right = cx + barW / 2f;

            float fraction = (float) entry.value / maxVal;
            float barH     = Math.max(fraction * barAreaH, cornerR * 2);
            float barTop   = barAreaBot - barH;

            // Bar
            RectF rect = new RectF(left, barTop, right, barAreaBot);
            canvas.drawRoundRect(rect, cornerR, cornerR, mBarPaint);

            // Value above bar
            canvas.drawText(String.valueOf(entry.value), cx, barTop - 3 * dp, mValuePaint);

            // Label below chart area
            canvas.drawText(abbreviate(entry.label), cx, h - 2 * dp, mLabelPaint);
        }
    }

    /** Returns the first word if ≤ 7 chars, otherwise truncates to 6 chars + "…". */
    private static String abbreviate(String s) {
        if (s == null || s.isEmpty()) return "—";
        int space = s.indexOf(' ');
        if (space > 0 && space <= 7) return s.substring(0, space);
        return s.length() > 6 ? s.substring(0, 6) + "…" : s;
    }
}
