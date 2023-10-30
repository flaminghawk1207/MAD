// References : https://github.com/tensorflow/examples/blob/master/lite/examples/object_detection/android/app/src/main/java/org/tensorflow/lite/examples/objectdetection/OverlayView.kt

/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.LinkedList
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var currentMoveDirection: String = ""

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var screenRectPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE

        screenRectPaint.color = ContextCompat.getColor(context!!, R.color.black)
        screenRectPaint.strokeWidth = 8F
        screenRectPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // Draw the 5 rects that cover the screen.
        val screenRects = arrayListOf<RectF>(
            RectF(0f, 0f, width / 5f, height.toFloat()),
            RectF(width / 5f, 0f, 2 *  width / 5f, height.toFloat()),
            RectF(2 * width / 5f, 0f, 3 * width / 5f, height.toFloat()),
            RectF(3 * width / 5f, 0f, 4 * width / 5f, height.toFloat()),
            RectF(4 * width / 5f, 0f, 5 * width / 5f, height.toFloat())
        );

        for (rect in screenRects) {
            canvas.drawRect(rect, screenRectPaint)
        }

        if (results.size == 0) {
            currentMoveDirection = "Continue"
        }

        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected objects
            val drawableText =
                result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)

            // Detecting if any of the screenRects contain / intersect the object.
            val rectContainsOrIntersects = mutableListOf<Boolean>();
            for (rect in screenRects) {
                if (rect.contains(drawableRect) || rect.intersect(drawableRect)) {
                    rectContainsOrIntersects += true
                } else {
                    rectContainsOrIntersects += false
                }
            }

            if (rectContainsOrIntersects[1]  && rectContainsOrIntersects[2] && rectContainsOrIntersects[3]) {
                currentMoveDirection = "STOP"
            }
            else {
                if (rectContainsOrIntersects[1] && !rectContainsOrIntersects[3]) {
                    currentMoveDirection = "RIGHT"
                } else if (rectContainsOrIntersects[3] && !rectContainsOrIntersects[1]) {
                    currentMoveDirection = "LEFT"
                } else if (rectContainsOrIntersects[2] && !rectContainsOrIntersects[3]) {
                    currentMoveDirection = "RIGHT"
                } else if (rectContainsOrIntersects[2] && !rectContainsOrIntersects[1]) {
                    currentMoveDirection = "LEFT"
                }
                else if (!rectContainsOrIntersects[1]  && !rectContainsOrIntersects[2] && !rectContainsOrIntersects[3]) {
                    currentMoveDirection = "CONTINUE"
                }
            }
        }

        drawMoveDirection(canvas)
    }

    fun drawMoveDirection(canvas: Canvas) {
        canvas.drawText(currentMoveDirection, 10f, canvas.height / 2f, textPaint)
    }
    fun setResults(
      detectionResults: MutableList<Detection>,
      imageHeight: Int,
      imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
