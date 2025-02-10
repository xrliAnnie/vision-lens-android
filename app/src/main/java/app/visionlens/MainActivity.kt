package app.visionlens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
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

/**
 * MainActivity handles the camera preview and ML Kit processing.
 * It uses CameraX for camera operations and ML Kit for text/face detection.
 */
@androidx.camera.core.ExperimentalGetImage
class MainActivity : AppCompatActivity() {
    // CameraX components for camera operations
    private lateinit var cameraProvider: ProcessCameraProvider  // Manages camera lifecycle
    private lateinit var camera: Camera                        // Camera instance
    private lateinit var preview: Preview                      // Camera preview use case
    private lateinit var imageAnalyzer: ImageAnalysis          // Image analysis use case
    
    // UI components
    private lateinit var viewFinder: PreviewView  // Displays camera preview
    private lateinit var resultText: TextView     // Shows detection results
    
    // ML Kit analyzers - initialized lazily to save resources
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )
    
    // Current analysis mode (text or face detection)
    private var analysisMode: AnalysisMode = AnalysisMode.TEXT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize UI components
        viewFinder = findViewById(R.id.viewFinder)
        resultText = findViewById(R.id.resultText)
        
        // Check and request camera permissions if needed
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        
        // Set up mode switching buttons
        findViewById<Button>(R.id.textButton).setOnClickListener {
            analysisMode = AnalysisMode.TEXT
            bindAnalyzer()
        }
        
        findViewById<Button>(R.id.faceButton).setOnClickListener {
            analysisMode = AnalysisMode.FACE
            bindAnalyzer()
        }
    }

    /**
     * Initializes the camera and starts the preview
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindAnalyzer()
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Sets up the camera preview and image analysis use cases
     */
    private fun bindAnalyzer() {
        // Set up camera preview
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(viewFinder.surfaceProvider)

        // Configure image analysis
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Process each frame
        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                // Process image based on selected mode
                when (analysisMode) {
                    AnalysisMode.TEXT -> processText(image)
                    AnalysisMode.FACE -> processFaces(image)
                }
            }
            imageProxy.close()
        }

        try {
            // Unbind previous use cases and bind new ones
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

    /**
     * Processes the image for text recognition
     */
    private fun processText(image: InputImage) {
        textRecognizer.process(image)
            .addOnSuccessListener { text ->
                resultText.text = text.text
            }
            .addOnFailureListener { e ->
                resultText.text = "Error: ${e.message}"
            }
    }

    /**
     * Processes the image for face detection
     */
    private fun processFaces(image: InputImage) {
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                resultText.text = "Detected ${faces.size} faces"
            }
            .addOnFailureListener { e ->
                resultText.text = "Error: ${e.message}"
            }
    }

    /**
     * Checks if all required permissions are granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "VisionLens"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    /**
     * Enum defining the available analysis modes
     */
    enum class AnalysisMode {
        TEXT, FACE
    }
} 