package com.example.pokemon

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.pokemon.data.AppDatabase
import com.example.pokemon.data.Creature
import com.example.pokemon.data.CreatureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class ARActivity : BaseActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var creatureOverlay: FrameLayout
    private lateinit var btnCapture: ImageButton
    private lateinit var db: AppDatabase
    private var currentCreature: ImageView? = null
    private var currentCreatureName: String? = null
    private val creatures = listOf(
        R.drawable.creature1, R.drawable.creature2, R.drawable.creature3,
        R.drawable.creature4, R.drawable.creature5, R.drawable.creature6
    )
    private val creatureNames = listOf(
        "Fluffy", "Sparky", "Leafy", "Aqua", "Rocko", "Shadow"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        previewView = findViewById(R.id.previewView)
        creatureOverlay = findViewById(R.id.creatureOverlay)
        btnCapture = findViewById(R.id.btnCapture)
        db = AppDatabase.getDatabase(this)

        if (checkCameraPermission()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }

        spawnRandomCreature()

        btnCapture.setOnClickListener {
            if (currentCreatureName != null) {
                captureCreature(currentCreatureName!!)
            } else {
                Toast.makeText(this, getString(R.string.no_creature_near_toast), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                Log.e("ARActivity", "Camera start failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun spawnRandomCreature() {
        currentCreature?.let { creatureOverlay.removeView(it) }

        val index = Random.nextInt(creatures.size)
        val res = creatures[index]
        val name = creatureNames[index]
        currentCreatureName = name

        val creatureView = ImageView(this)
        creatureView.setImageResource(res)
        val size = Random.nextInt(200, 400)
        val layoutParams = FrameLayout.LayoutParams(size, size)
        layoutParams.leftMargin = Random.nextInt(100, creatureOverlay.width.coerceAtLeast(500))
        layoutParams.topMargin = Random.nextInt(200, creatureOverlay.height.coerceAtLeast(800))
        creatureView.layoutParams = layoutParams

        creatureOverlay.addView(creatureView)
        currentCreature = creatureView

        // Respawn another after delay
        creatureOverlay.postDelayed({ spawnRandomCreature() }, 25000)
    }

    private fun captureCreature(name: String) {
        val res = creatures[creatureNames.indexOf(name)]
        lifecycleScope.launch {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val creature = Creature(
                    name = name,
                    sprite = res,
                    capturedAt = System.currentTimeMillis(),
                    syncedToFirebase = false,
                    userId = userId
                )

                withContext(Dispatchers.IO) {
                    db.creatureDao().insert(creature)

                    // Try to sync to Firebase if online
                    if (isOnline()) {
                        CreatureRepository.syncToFirebase(this@ARActivity)
                    }
                }

                Toast.makeText(this@ARActivity, getString(R.string.creature_captured, name), Toast.LENGTH_SHORT).show()
                currentCreature?.let { creatureOverlay.removeView(it) }
                currentCreature = null
                currentCreatureName = null
            } catch (e: Exception) {
                Log.e("ARActivity", getString(R.string.failed_capturetxt), e)
                Toast.makeText(this@ARActivity, getString(R.string.capture_failtxt), Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
