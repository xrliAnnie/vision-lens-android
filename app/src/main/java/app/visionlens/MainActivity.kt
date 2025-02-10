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
import java.util.concurrent.Executors

/**
 * MainActivity handles the camera preview and ML Kit processing.
 * It uses CameraX for camera operations and ML Kit for text/face detection.
 */
@androidx.camera.core.ExperimentalGetImage
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "VisionLens"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

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
        Log.d(TAG, "Initializing text recognizer")
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    ).also { Log.d(TAG, "Face detector initialized") }
    
    // Current analysis mode (text or face detection)
    private var analysisMode: AnalysisMode = AnalysisMode.TEXT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(R.layout.activity_main)
        
        // Initialize UI components
        viewFinder = findViewById(R.id.viewFinder)
        resultText = findViewById(R.id.resultText)
        Log.d(TAG, "UI components initialized")
        
        // Check and request camera permissions if needed
        if (allPermissionsGranted()) {
            Log.d(TAG, "Camera permissions already granted")
            startCamera()
        } else {
            Log.d(TAG, "Requesting camera permissions")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        
        // Set up mode switching buttons
        setupModeButtons()
    }

    private fun setupModeButtons() {
        findViewById<Button>(R.id.textButton).setOnClickListener {
            Log.d(TAG, "Switching to TEXT mode")
            analysisMode = AnalysisMode.TEXT
            bindAnalyzer()
        }
        
        findViewById<Button>(R.id.faceButton).setOnClickListener {
            Log.d(TAG, "Switching to FACE mode")
            analysisMode = AnalysisMode.FACE
            bindAnalyzer()
        }
        Log.d(TAG, "Mode buttons initialized")
    }

    /**
     * Initializes the camera and starts the preview
     */
    private fun startCamera() {
        Log.d(TAG, "Starting camera initialization")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "Camera provider initialized successfully")
                bindAnalyzer()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get camera provider", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Sets up the camera preview and image analysis use cases
     */
    private fun bindAnalyzer() {
        Log.d(TAG, "Binding camera use cases, mode: $analysisMode")
        
        // Set up camera preview
        val preview = Preview.Builder()
            .build()
            .also { Log.d(TAG, "Preview use case built") }
        preview.setSurfaceProvider(viewFinder.surfaceProvider)

        // Configure image analysis
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { Log.d(TAG, "Image analyzer built") }

        // Process each frame
        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                when (analysisMode) {
                    AnalysisMode.TEXT -> processText(image)
                    AnalysisMode.FACE -> processFaces(image)
                }
            } else {
                Log.w(TAG, "Skipping frame - null image")
            }
            imageProxy.close()  // ✅ Ensure proper release
        }

        try {
            // Unbind previous use cases and bind new ones
            Log.d(TAG, "Unbinding previous use cases")
            cameraProvider.unbindAll()
            
            camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
            Log.d(TAG, "Camera use cases bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    /**
     * Processes the image for text recognition
     */
    private fun processText(image: InputImage) {
        Log.v(TAG, "Processing image for text recognition")
        textRecognizer.process(image)
            .addOnSuccessListener { text ->
                Log.d(TAG, "Text recognition successful: ${text.text}")
                resultText.text = text.text
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed", e)
                resultText.text = "Error: ${e.message}"
            }
    }

    /**
     * Processes the image for face detection
     */
    private fun processFaces(image: InputImage) {
        Log.v(TAG, "Processing image for face detection")
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                Log.d(TAG, "Face detection successful: ${faces.size} faces found")
                resultText.text = "Detected ${faces.size} faces"
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
                resultText.text = "Error: ${e.message}"
            }
    }

    /**
     * Checks if all required permissions are granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions granted")
                startCamera()
            } else {
                Log.e(TAG, "Permissions not granted by the user")
                resultText.text = "Permissions not granted"
                finish()
            }
        }
    }

    /**
     * Enum defining the available analysis modes
     */
    enum class AnalysisMode {
        TEXT, FACE
    }
} 