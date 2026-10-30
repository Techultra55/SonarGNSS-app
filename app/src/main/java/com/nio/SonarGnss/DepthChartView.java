package com.nio.SonarGnss;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the DepthChartView class and renders a real-time 2D acoustic echogram depth chart.
 */
public class DepthChartView extends View {

    // This is the MAX_POINTS constant and limits the maximum number of depth history points displayed on screen
    private static final int MAX_POINTS = 75;

    // This is the points list field and stores historical depth measurement float values
    private final List<Float> points = new ArrayList<>();

    // This is the linePaint field and defines styling for the acoustic contour depth line
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // This is the fillPaint field and defines gradient shading underneath the depth line
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // This is the axisPaint field and defines styling for outer chart boundary axis lines
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // This is the gridPaint field and defines styling for background grid indicator lines
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // This is the textPaint field and defines styling for depth measurement numbers on the Y axis
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // This is the pointGlowPaint field and defines styling for the outer glowing circle on the latest depth point
    private final Paint pointGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // This is the pointPaint field and defines styling for the inner solid dot on the latest depth point
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // This is the path field and builds geometric line segments for the acoustic trace
    private final Path path = new Path();
    // This is the fillPath field and builds closed polygon shapes for water column gradient shading
    private final Path fillPath = new Path();

    // This is the DepthChartView constructor and configures paint colors, line widths, and rendering styles
    public DepthChartView(Context context, AttributeSet attrs) {
        // This line passes context and attributes to the parent View constructor taking insights from context and attrs
        super(context, attrs);

        // This line sets cyan color for the depth line taking insights from hex color string #00E5FF
        linePaint.setColor(Color.parseColor("#00E5FF"));
        // This line sets thickness for the depth contour line taking insights from float value 4.5f
        linePaint.setStrokeWidth(4.5f);
        // This line sets drawing style to stroke outline taking insights from Paint.Style.STROKE
        linePaint.setStyle(Paint.Style.STROKE);
        // This line sets rounded caps at line segment ends taking insights from Paint.Cap.ROUND
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        // This line sets rounded joints where line segments meet taking insights from Paint.Join.ROUND
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // This line sets fill style for the area under the depth curve taking insights from Paint.Style.FILL
        fillPaint.setStyle(Paint.Style.FILL);

        // This line sets dark slate color for boundary axes taking insights from hex color string #334155
        axisPaint.setColor(Color.parseColor("#334155"));
        // This line sets axis line width taking insights from float value 2f
        axisPaint.setStrokeWidth(2f);

        // This line sets dark navy color for grid lines taking insights from hex color string #1E293B
        gridPaint.setColor(Color.parseColor("#1E293B"));
        // This line sets grid line width taking insights from float value 1.2f
        gridPaint.setStrokeWidth(1.2f);

        // This line sets light slate gray color for axis text labels taking insights from hex color string #94A3B8
        textPaint.setColor(Color.parseColor("#94A3B8"));
        // This line sets font size for depth text labels taking insights from float value 26f
        textPaint.setTextSize(26f);
        // This line aligns text to the right side of specified coordinates taking insights from Paint.Align.RIGHT
        textPaint.setTextAlign(Paint.Align.RIGHT);

        // This line sets semi-transparent cyan color for beacon glow circle taking insights from hex color #5500E5FF
        pointGlowPaint.setColor(Color.parseColor("#5500E5FF"));
        // This line sets fill style for the glowing beacon taking insights from Paint.Style.FILL
        pointGlowPaint.setStyle(Paint.Style.FILL);

        // This line sets solid white color for current depth beacon center taking insights from hex color #FFFFFF
        pointPaint.setColor(Color.parseColor("#FFFFFF"));
        // This line sets fill style for the center beacon dot taking insights from Paint.Style.FILL
        pointPaint.setStyle(Paint.Style.FILL);
    }

    // This is the addDepth function and appends a new depth measurement to the history list and triggers a redraw
    public void addDepth(Float depth) {
        // This line adds the new depth reading to our list taking insights from depth parameter
        points.add(depth);

        // This line checks if stored points exceed maximum buffer limit taking insights from points size and MAX_POINTS
        if (points.size() > MAX_POINTS) {
            // This line removes oldest depth point to keep chart scrolling horizontally taking insights from index 0
            points.remove(0);
        }

        // This line requests Android view system to redraw the chart taking insights from updated points list
        postInvalidateOnAnimation();
    }

    // This is the clear function and clears all depth history points to reset the chart
    public void clear() {
        // This line clears all items from the depth points list taking insights from points list
        points.clear();
        // This line requests view redraw to render empty chart taking insights from cleared points
        postInvalidateOnAnimation();
    }

    // This is the onDraw function and renders background, grid lines, depth curve, gradient, and latest beacon
    @Override
    protected void onDraw(Canvas canvas) {
        // This line calls parent View draw method taking insights from canvas
        super.onDraw(canvas);

        // This line gets current view width taking insights from getWidth()
        float width = getWidth();
        // This line gets current view height taking insights from getHeight()
        float height = getHeight();

        // This line draws dark marine canvas background color taking insights from hex color #070B14
        canvas.drawColor(Color.parseColor("#070B14"));

        // This line defines left padding for Y-axis text labels taking insights from float value 85f
        float paddingLeft = 85f;
        // This line defines right padding from view edge taking insights from float value 30f
        float paddingRight = 30f;
        // This line defines top padding from view top taking insights from float value 25f
        float paddingTop = 25f;
        // This line defines bottom padding from view bottom taking insights from float value 30f
        float paddingBottom = 30f;

        // This line sets chart left boundary coordinate taking insights from paddingLeft
        float left = paddingLeft;
        // This line sets chart right boundary coordinate taking insights from width and paddingRight
        float right = width - paddingRight;
        // This line sets chart bottom boundary coordinate taking insights from height and paddingBottom
        float bottom = height - paddingBottom;

        // This line calculates printable chart width taking insights from right and left boundaries
        float chartWidth = right - left;
        // This line calculates printable chart height taking insights from bottom boundary and paddingTop
        float chartHeight = bottom - paddingTop;

        // This line checks if points list is empty taking insights from points.isEmpty()
        if (points.isEmpty()) {
            // This line draws blank grid axes taking insights from boundary coordinates and default min/max
            drawAxes(canvas, left, bottom, right, paddingTop, chartHeight, 0f, 1f);
            return;
        }

        // This line initializes minimum depth tracking variable taking insights from maximum possible float
        float min = Float.MAX_VALUE;
        // This line initializes maximum depth tracking variable taking insights from lowest possible float
        float max = -Float.MAX_VALUE;
        // This line initializes boolean flag to check if any valid depth numbers exist taking insights from false
        boolean hasValidData = false;

        // This line iterates over all historical points to calculate min and max range taking insights from points list
        for (Float value : points) {
            // This line ignores null readings taking insights from value
            if (value == null) {
                continue;
            }
            // This line marks that valid reading exists taking insights from non-null value
            hasValidData = true;
            // This line updates minimum value taking insights from value and current min
            if (value < min) {
                min = value;
            }
            // This line updates maximum value taking insights from value and current max
            if (value > max) {
                max = value;
            }
        }

        // This line checks if no valid data points were found taking insights from hasValidData flag
        if (!hasValidData) {
            // This line draws empty grid axes taking insights from boundary coordinates and default min/max
            drawAxes(canvas, left, bottom, right, paddingTop, chartHeight, 0f, 1f);
            return;
        }

        // This line checks if depth readings are all identical flat line taking insights from difference between max and min
        boolean isFlatLine = Math.abs(max - min) < 0.000001f;

        // This line assigns display minimum taking insights from calculated min
        float displayMin = min;
        // This line assigns display maximum taking insights from calculated max
        float displayMax = max;

        // This line provides slight vertical margin when data is a flat line taking insights from isFlatLine flag
        if (isFlatLine) {
            // This line increases displayMax slightly taking insights from max
            displayMax = max + 0.05f;
            // This line decreases displayMin slightly taking insights from min
            displayMin = min - 0.05f;
        }

        // This line draws grid lines and axis numbers taking insights from boundary coordinates and display range
        drawAxes(
                canvas,
                left,
                bottom,
                right,
                paddingTop,
                chartHeight,
                displayMin,
                displayMax
        );

        // This line resets the contour path taking insights from path object
        path.reset();
        // This line resets the gradient fill path taking insights from fillPath object
        fillPath.reset();

        // This line calculates horizontal step between consecutive data points taking insights from points count and chartWidth
        float stepX = points.size() > 1
                ? chartWidth / (float) (points.size() - 1)
                : chartWidth;

        // This line initializes tracking flag for path starting point taking insights from initial false value
        boolean started = false;
        // This line stores horizontal starting coordinate taking insights from left boundary
        float firstX = left;
        // This line stores last plotted horizontal coordinate taking insights from left boundary
        float lastX = left;
        // This line stores last plotted vertical coordinate taking insights from bottom boundary
        float lastY = bottom;

        // This line loops over each depth value to plot points and build curve paths taking insights from points list
        for (int i = 0; i < points.size(); i++) {
            // This line retrieves depth reading at index i taking insights from points list
            Float value = points.get(i);
            // This line skips null values and breaks line continuity taking insights from value
            if (value == null) {
                started = false;
                continue;
            }

            // This line calculates X coordinate for index i taking insights from left boundary and stepX
            float x = left + (i * stepX);
            float y;

            // This line calculates Y coordinate placing point in center if flat or scaling proportionally taking insights from isFlatLine
            if (isFlatLine) {
                y = paddingTop + (chartHeight / 2f);
            } else {
                // This line calculates normalized fraction between min and max taking insights from value, min, and max
                float normalized = (value - min) / (max - min);
                // This line computes vertical screen position inverting Y axis for depth taking insights from normalized fraction
                y = paddingTop + chartHeight - (normalized * chartHeight);
            }

            // This line starts path or connects to next point taking insights from started flag
            if (!started) {
                // This line moves contour path to initial point taking insights from x and y
                path.moveTo(x, y);
                // This line moves fill path to chart bottom taking insights from x and bottom
                fillPath.moveTo(x, bottom);
                // This line draws line from bottom up to first depth point taking insights from x and y
                fillPath.lineTo(x, y);
                // This line records first X coordinate for closing fill polygon taking insights from x
                firstX = x;
                // This line marks path as actively started taking insights from true
                started = true;
            } else {
                // This line draws continuous line segment to next point taking insights from x and y
                path.lineTo(x, y);
                // This line draws continuous fill segment to next point taking insights from x and y
                fillPath.lineTo(x, y);
            }

            // This line saves current X coordinate as latest position taking insights from x
            lastX = x;
            // This line saves current Y coordinate as latest position taking insights from y
            lastY = y;
        }

        // This line finalizes and renders the gradient shaded water area taking insights from started flag
        if (started) {
            // This line draws fill edge down to chart bottom at last point taking insights from lastX and bottom
            fillPath.lineTo(lastX, bottom);
            // This line draws fill edge back to first X at bottom to close polygon taking insights from firstX and bottom
            fillPath.lineTo(firstX, bottom);
            // This line closes fill polygon shape taking insights from fillPath
            fillPath.close();

            // This line creates vertical linear gradient shader from cyan to transparent taking insights from vertical bounds
            fillPaint.setShader(new LinearGradient(
                    0, paddingTop, 0, bottom,
                    Color.parseColor("#3800E5FF"),
                    Color.parseColor("#05001A26"),
                    Shader.TileMode.CLAMP
            ));
            // This line paints water column gradient onto canvas taking insights from fillPath and fillPaint
            canvas.drawPath(fillPath, fillPaint);
        }

        // This line renders the glowing acoustic contour depth line onto canvas taking insights from path and linePaint
        canvas.drawPath(path, linePaint);

        // This line renders pulsing position beacon on latest reading taking insights from started flag
        if (started) {
            // This line draws outer cyan glow circle taking insights from lastX, lastY, and pointGlowPaint
            canvas.drawCircle(lastX, lastY, 12f, pointGlowPaint);
            // This line draws inner white beacon dot taking insights from lastX, lastY, and pointPaint
            canvas.drawCircle(lastX, lastY, 5f, pointPaint);
        }
    }

    // This is the drawAxes function and renders boundary axes, horizontal/vertical grid lines, and depth value text labels
    private void drawAxes(
            Canvas canvas,
            float left,
            float bottom,
            float right,
            float paddingTop,
            float chartHeight,
            float min,
            float max
    ) {
        // This line draws vertical Y-axis line on left edge taking insights from left, paddingTop, and bottom
        canvas.drawLine(left, paddingTop, left, bottom, axisPaint);
        // This line draws horizontal X-axis line on bottom edge taking insights from left, bottom, and right
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        // This line specifies number of horizontal grid subdivisions taking insights from constant 4
        int horizontalSteps = 4;
        // This line draws horizontal grid lines across chart taking insights from horizontalSteps
        for (int i = 0; i <= horizontalSteps; i++) {
            // This line calculates height ratio for grid line taking insights from index i and horizontalSteps
            float ratio = i / (float) horizontalSteps;
            // This line computes Y screen position for grid line taking insights from ratio and chartHeight
            float y = paddingTop + chartHeight - (ratio * chartHeight);
            // This line draws horizontal grid line taking insights from left, right, and y
            canvas.drawLine(left, y, right, y, gridPaint);
        }

        // This line specifies number of vertical grid subdivisions taking insights from constant 5
        int verticalSteps = 5;
        // This line calculates chart horizontal width taking insights from right and left boundaries
        float chartWidth = right - left;
        // This line draws vertical grid lines across chart taking insights from verticalSteps
        for (int i = 1; i <= verticalSteps; i++) {
            // This line computes X screen position for vertical grid line taking insights from step fraction
            float x = left + (chartWidth * i / (float) verticalSteps);
            // This line draws vertical grid line taking insights from x, paddingTop, and bottom
            canvas.drawLine(x, paddingTop, x, bottom, gridPaint);
        }

        // This line calculates midpoint depth label value taking insights from min and max average
        float mid = (min + max) / 2f;
        // This line sets horizontal position for right-aligned text labels taking insights from left minus 12f offset
        float labelX = left - 12f;

        // This line renders maximum depth text label taking insights from max, labelX, and paddingTop
        canvas.drawText(
                String.format("%.1fm", max),
                labelX,
                paddingTop + (textPaint.getTextSize() / 2f),
                textPaint
        );

        // This line computes vertical position for midpoint text label taking insights from paddingTop and half chart height
        float midY = paddingTop + (chartHeight / 2f);
        // This line renders midpoint depth text label taking insights from mid, labelX, and midY
        canvas.drawText(
                String.format("%.1fm", mid),
                labelX,
                midY + (textPaint.getTextSize() / 2f),
                textPaint
        );

        // This line renders minimum depth text label taking insights from min, labelX, and bottom
        canvas.drawText(
                String.format("%.1fm", min),
                labelX,
                bottom - 4f,
                textPaint
        );
    }
}
