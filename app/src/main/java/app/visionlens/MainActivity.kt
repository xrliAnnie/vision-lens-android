package app.visionlens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@androidx.camera.core.ExperimentalGetImage
class MainActivity : AppCompatActivity() {
    // CameraX components
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var preview: Preview
    private lateinit var imageAnalyzer: ImageAnalysis
    
    // UI components
    private lateinit var viewFinder: PreviewView
    private lateinit var resultText: TextView
    
    // ML Kit analyzers
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )
    
    private var analysisMode: AnalysisMode = AnalysisMode.TEXT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        viewFinder = findViewById(R.id.viewFinder)
        resultText = findViewById(R.id.resultText)
        
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        
        findViewById<Button>(R.id.textButton).setOnClickListener {
            analysisMode = AnalysisMode.TEXT
            bindAnalyzer()
        }
        
        findViewById<Button>(R.id.faceButton).setOnClickListener {
            analysisMode = AnalysisMode.FACE
            bindAnalyzer()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindAnalyzer()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindAnalyzer() {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(viewFinder.surfaceProvider)

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                when (analysisMode) {
                    AnalysisMode.TEXT -> processText(image)
                    AnalysisMode.FACE -> processFaces(image)
                }
            }
            imageProxy.close()
        }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun processText(image: InputImage) {
        textRecognizer.process(image)
            .addOnSuccessListener { text ->
                resultText.text = text.text
            }
            .addOnFailureListener { e ->
                resultText.text = "Error: ${e.message}"
            }
    }

    private fun processFaces(image: InputImage) {
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                resultText.text = "Detected ${faces.size} faces"
            }
            .addOnFailureListener { e ->
                resultText.text = "Error: ${e.message}"
            }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "VisionLens"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    enum class AnalysisMode {
        TEXT, FACE
    }
} 