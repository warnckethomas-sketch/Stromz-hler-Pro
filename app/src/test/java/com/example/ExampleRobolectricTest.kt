package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StromZähler", appName)
  }

  @Test
  fun `wrapText wraps long note text across multiple lines`() {
    val paint = android.graphics.Paint().apply {
      textSize = 10f
    }
    val longNote = "Dies ist eine sehr lange Notiz zum Ablesezeitraum, die definitiv über mehrere Zeilen umgebrochen werden muss, damit sie im PDF-Bericht sauber dargestellt wird."
    
    // In Robolectric ShadowPaint, measureText returns text.length.toFloat()
    val fullWidth = paint.measureText(longNote)
    val maxWidth = fullWidth / 3f
    val wrapped = com.example.utils.PdfExporter.wrapText(longNote, paint, maxWidth)
    org.junit.Assert.assertTrue("Should wrap into multiple lines (got ${wrapped.size})", wrapped.size > 1)
    
    for (line in wrapped) {
      org.junit.Assert.assertTrue("Line width must be <= maxWidth", paint.measureText(line) <= maxWidth)
    }
  }

  @Test
  fun `wrapText preserves multiple paragraphs with line breaks`() {
    val paint = android.graphics.Paint().apply {
      textSize = 10f
    }
    val multiParagraphNote = "Erster kurzer Absatz.\nZweiter kurzer Absatz."
    val wrapped = com.example.utils.PdfExporter.wrapText(multiParagraphNote, paint, 400f)
    org.junit.Assert.assertEquals(2, wrapped.size)
    org.junit.Assert.assertEquals("Erster kurzer Absatz.", wrapped[0])
    org.junit.Assert.assertEquals("Zweiter kurzer Absatz.", wrapped[1])
  }
}
