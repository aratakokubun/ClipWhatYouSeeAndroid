package com.kkbnart.clipwhatyousee

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.kkbnart.clipwhatyousee.graphics.GraphicOverlay
import com.kkbnart.clipwhatyousee.graphics.TextGraphic
import com.wonderkiln.camerakit.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    lateinit var cameraView: CameraView
    lateinit var cameraButton: Button
    private lateinit var graphicOverlay: GraphicOverlay
    var recognizedTextBlocks: List<FirebaseVisionText.Block> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        graphicOverlay = this.findViewById(R.id.graphi_overlay)
        graphicOverlay.setOnTouchListener { view, motionEvent ->
            clipRecognizedText(motionEvent.x, motionEvent.y)
        }

        cameraView = this.findViewById(R.id.cameraView)
        cameraView.addCameraKitListener(object: CameraKitEventListener {
            override fun onEvent(event: CameraKitEvent) {
                // Nothing to do
            }

            override fun onError(error: CameraKitError) {
                // Nothing to do
            }

            override fun onImage(image: CameraKitImage) {
                var bitmap = image.bitmap
                bitmap = Bitmap.createScaledBitmap(bitmap, cameraView.width, cameraView.height, false)
                cameraView.stop()
                runTextRecognition(bitmap)
            }

            override fun onVideo(video: CameraKitVideo?) {
                // Nothing to do
            }
        })

        cameraButton = this.findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener {
            graphicOverlay.clear()
            cameraView.start()
            cameraView.captureImage()
        }
    }

    private fun clipRecognizedText(x: Float, y: Float): Boolean {
        for (i in recognizedTextBlocks.indices) {
            val lines = recognizedTextBlocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    if (elements[k].boundingBox != null && elements[k].boundingBox!!.contains(x.toInt(), y.toInt())) {
                        clipText(elements[k].text)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun clipText(text: String) {
//        val displayMetrics = DisplayMetrics()
//        windowManager.defaultDisplay.getMetrics(displayMetrics)
//        var width = displayMetrics.widthPixels
//        var height = displayMetrics.heightPixels
        val clipBoardManager: ClipboardManager = applicationContext.getSystemService(
                Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipBoardManager.primaryClip = ClipData.newPlainText("label", text)
        val message = "text '%s' clipped!!".format(text)
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
        toast.show()
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val detector = FirebaseVision.getInstance().visionTextDetector
        detector.detectInImage(image)
                .addOnSuccessListener {
                    texts -> processTextRecognitionResult(texts as FirebaseVisionText)}
                .addOnFailureListener {
                    e -> e.printStackTrace()}
    }

    private fun processTextRecognitionResult(texts: FirebaseVisionText) {
        val blocks = texts.blocks
        if (blocks.size == 0) {
            Log.d(TAG, "no text found")
            return
        }

        graphi_overlay.clear()
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    val textGraphic = TextGraphic(graphicOverlay, elements[k])
                    graphicOverlay.add(textGraphic)
                }
            }
        }
        // Save recognized elements to cache
        recognizedTextBlocks = blocks
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }
}
