package com.example.pokemon

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.pokemon.data.AppDatabase
import com.example.pokemon.data.Creature
import com.example.pokemon.data.CreatureRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.views.overlay.Polygon
import kotlin.random.Random
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mapView: MapView
    private lateinit var db: AppDatabase
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var steps = 0
    private var isLoggedOut = false
    private lateinit var playerMarker: Marker
    private lateinit var prefs: SharedPreferences
    private lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener
    private var playerName: String = "You"
    private lateinit var txtSteps: TextView
    private lateinit var btnViewCaptured: Button
    private var initialStepCount: Int? = null
    private var lastAcceleration = 0f
    private var acceleration = 0f
    private var lastStepTime = 0L
    private lateinit var btnPokeball: ImageButton
    private val activeCreatures = mutableListOf<Marker>()
    private lateinit var compassView: ImageView
    private var lastPosition: GeoPoint? = null

    private val creatures = listOf(
        R.drawable.creature1, R.drawable.creature2, R.drawable.creature3, R.drawable.creature4,
        R.drawable.creature5, R.drawable.creature6, R.drawable.creature7, R.drawable.creature8,
        R.drawable.creature9, R.drawable.creature10, R.drawable.creature11, R.drawable.creature12,
        R.drawable.creature13, R.drawable.creature14, R.drawable.creature15, R.drawable.creature16
    )

    private val creatureNames = listOf(
        "Fluffy", "Sparky", "Leafy", "Aqua", "Rocko", "Shadow", "Frosty", "Blaze",
        "Stormy", "Petal", "Bubble", "Boulder", "Nightfang", "Glacier", "Inferno", "Volt"
    )

    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Configuration.getInstance().load(applicationContext, getSharedPreferences("prefs", MODE_PRIVATE))
        setContentView(R.layout.activity_main)
        lifecycleScope.launch {
            CreatureRepository.syncRoomWithServer(this@MainActivity)
        }

        prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        db = AppDatabase.getDatabase(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED) {
                stepCounterSensor?.also { sensor ->
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                }
            }
            if (stepCounterSensor == null) {
                Toast.makeText(this, "No step counter sensor available", Toast.LENGTH_LONG).show()
            }
        }

        setupViews()
        setupMap()
        setupListeners()

        mapView.postDelayed({ spawnRandomCreature() }, 5000)
    }

    private fun setupViews() {
        txtSteps = findViewById(R.id.txtSteps)
        btnViewCaptured = findViewById(R.id.btnViewCaptured)
        btnPokeball = findViewById(R.id.btnPokeball)
        compassView = findViewById(R.id.compassView)

        findViewById<Button>(R.id.btnARMode).setOnClickListener {
            startActivity(Intent(this, ARActivity::class.java))
        }

        findViewById<Button>(R.id.btnEncyclopedia).setOnClickListener {
            startActivity(Intent(this, EncyclopediaActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnViewCaptured.setOnClickListener { showCapturedCreatures() }

        btnPokeball.setOnClickListener {
            if (activeCreatures.isEmpty()) {
                Toast.makeText(this, "No creatures nearby!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val closest = activeCreatures.minByOrNull {
                playerMarker.position.distanceToAsDouble(it.position)
            }

            if (closest != null && playerMarker.position.distanceToAsDouble(closest.position) < 60) {
                val creatureName = closest.title ?: "Unknown"
                val spriteRes = creatures.getOrNull(creatureNames.indexOf(creatureName)) ?: R.drawable.creature1
                captureCreature(creatureName, spriteRes)
                mapView.overlays.remove(closest)
                activeCreatures.remove(closest)
                mapView.invalidate()
            } else {
                Toast.makeText(this, "No creatures in range!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMap() {
        mapView = findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)
        mapView.isTilesScaledToDpi = true
        mapView.isHorizontalMapRepetitionEnabled = false
        mapView.isVerticalMapRepetitionEnabled = false
        mapView.minZoomLevel = 17.0
        mapView.maxZoomLevel = 20.0

        val mapController = mapView.controller
        val playerPosition = GeoPoint(-33.9249, 18.4241)
        mapController.setZoom(18.0)
        mapController.setCenter(playerPosition)
//this is the different options for the map
        when (prefs.getString("pref_map_theme", "default")) {
            "default" -> mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
            "satellite" -> mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.USGS_SAT)
            "dark" -> mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.OpenTopo)
        }

        val rotationEnabled = prefs.getBoolean("pref_map_rotation", true)
        if (rotationEnabled) {
            mapView.setMapOrientation(-45f, false)
        } else {
            mapView.setMapOrientation(0f, false)
        }

        mapController.animateTo(playerPosition, 19.0, 2000L)
//this is for the different avatar sizes
        val sizePref = prefs.getString("pref_avatar_size", "medium")
        val avatarSize = when (sizePref) {
            "small" -> 128
            "medium" -> 200
            "large" -> 300
            else -> 200
        }

        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.player_avatar)
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, avatarSize, avatarSize, true)
        originalBitmap.recycle()
        val playerIcon = BitmapDrawable(resources, scaledBitmap)

        val avatarName = prefs.getString("pref_avatar_name", "You") ?: "You"
        playerName = avatarName

        playerMarker = Marker(mapView).apply {
            position = playerPosition
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = avatarName
            icon = playerIcon
        }
        mapView.overlays.add(playerMarker)

        lastPosition = playerMarker.position

        val nameOverlay = object : org.osmdroid.views.overlay.Overlay() {
            override fun draw(canvas: android.graphics.Canvas, mapView: MapView, shadow: Boolean) {
                if (shadow) return
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 40f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val screenPoint = mapView.projection.toPixels(playerMarker.position, null)
                canvas.drawText(playerName, screenPoint.x.toFloat(), screenPoint.y - 50f, paint)
            }
        }
        mapView.overlays.add(nameOverlay)

        val playerRadius = Polygon(mapView).apply {
            points = Polygon.pointsAsCircle(playerMarker.position, 50.0)
            outlinePaint.color = android.graphics.Color.parseColor("#448AFF")
            outlinePaint.strokeWidth = 4f
            fillPaint.color = android.graphics.Color.parseColor("#33448AFF")
        }
        mapView.overlays.add(playerRadius)

        mapView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val geoPoint = mapView.projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                playerMarker.position = geoPoint
                playerRadius.points = Polygon.pointsAsCircle(geoPoint, 25.0)
                steps++
                txtSteps.text = "Steps: $steps"
                lastPosition?.let { last ->
                    val dx = geoPoint.longitude - last.longitude
                    val dy = geoPoint.latitude - last.latitude
                    val angle = Math.toDegrees(Math.atan2(dy, dx)).toFloat()
                    compassView.rotation = angle
                }
                lastPosition = geoPoint
                mapView.invalidate()
            }
            false
        }
    }
//this works with the settings
    private fun setupListeners() {
        listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (isLoggedOut) return@OnSharedPreferenceChangeListener

            when (key) {
                "pref_avatar_name" -> {
                    playerName = sharedPrefs.getString(key, "You") ?: "You"
                    if (::playerMarker.isInitialized) {
                        playerMarker.title = playerName
                        mapView.invalidate()
                    }
                }
                "pref_avatar_size" -> {
                    if (!::playerMarker.isInitialized) return@OnSharedPreferenceChangeListener
                    val sizePref = sharedPrefs.getString(key, "medium")
                    val avatarSize = when (sizePref) {
                        "small" -> 128
                        "medium" -> 200
                        "large" -> 300
                        else -> 200
                    }
                    val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.player_avatar)
                    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, avatarSize, avatarSize, true)
                    originalBitmap.recycle()
                    val playerIcon = BitmapDrawable(resources, scaledBitmap)
                    playerMarker.icon = playerIcon
                    mapView.invalidate()
                }
                "pref_map_rotation" -> {
                    if (!::mapView.isInitialized) return@OnSharedPreferenceChangeListener
                    val rotationEnabled = sharedPrefs.getBoolean(key, true)
                    if (rotationEnabled) {
                        mapView.setMapOrientation(-45f, false)
                    } else {
                        mapView.setMapOrientation(0f, false)
                    }
                }
                "pref_map_theme" -> {
                    if (!::mapView.isInitialized) return@OnSharedPreferenceChangeListener
                    when (sharedPrefs.getString(key, "default")) {
                        "default" -> mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                        "satellite" -> mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.USGS_SAT)
                        "dark" -> mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.OpenTopo)
                    }
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }
//this initializes the changes made in the settings in the app
    override fun onResume() {
        super.onResume()

        // Check if user logged out
        if (auth.currentUser == null) {
            cleanupOnLogout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (isLoggedOut) return

        stepCounterSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        if (!::playerMarker.isInitialized || !::mapView.isInitialized) return

        playerName = prefs.getString("pref_avatar_name", "You") ?: "You"
        playerMarker.title = playerName

        val sizePref = prefs.getString("pref_avatar_size", "medium")
        val avatarSize = when (sizePref) {
            "small" -> 128
            "medium" -> 200
            "large" -> 300
            else -> 200
        }

        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.player_avatar)
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, avatarSize, avatarSize, true)
        originalBitmap.recycle()
        val playerIcon = BitmapDrawable(resources, scaledBitmap)
        playerMarker.icon = playerIcon

        val rotationEnabled = prefs.getBoolean("pref_map_rotation", true)
        if (rotationEnabled) {
            mapView.setMapOrientation(-45f, false)
        } else {
            mapView.setMapOrientation(0f, false)
        }

        when (prefs.getString("pref_map_theme", "default")) {
            "default" -> mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
            "satellite" -> mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.USGS_SAT)
            "dark" -> mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.OpenTopo)
        }

        mapView.invalidate()
    }

    override fun onPause() {
        super.onPause()
        if (::prefs.isInitialized && ::listener.isInitialized) {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupOnLogout()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || isLoggedOut) return

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (initialStepCount == null) initialStepCount = totalSteps
            val stepsSinceStart = totalSteps - (initialStepCount ?: totalSteps)
            txtSteps.text = "Steps: $stepsSinceStart"
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val g = Math.sqrt((x*x + y*y + z*z).toDouble()).toFloat()
            acceleration = g - lastAcceleration
            lastAcceleration = g
            val now = System.currentTimeMillis()
            if (acceleration > 1.2 && now - lastStepTime > 300) {
                steps++
                lastStepTime = now
                txtSteps.text = "Steps: $steps"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
//this is all the logic for spawning the creatures
    private fun spawnRandomCreature() {
        // Exit early if logged out or components not ready
        if (isLoggedOut || !::playerMarker.isInitialized || !::mapView.isInitialized) {
            return
        }

        try {
            val index = Random.nextInt(creatures.size)
            val creatureDrawableRes = creatures[index]
            val creatureName = creatureNames[index]
            val playerPos = playerMarker.position

            val lat = playerPos.latitude + Random.nextDouble(-0.001, 0.001)
            val lon = playerPos.longitude + Random.nextDouble(-0.001, 0.001)

            val originalBitmap = BitmapFactory.decodeResource(resources, creatureDrawableRes)
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 130, 130, true)
            originalBitmap.recycle()
            val creatureIcon = BitmapDrawable(resources, scaledBitmap)

            // Double-check before creating marker
            if (isLoggedOut || !::mapView.isInitialized) {
                return
            }

            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lon)
                title = creatureName
                icon = creatureIcon
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }

            mapView.overlays.add(marker)
            activeCreatures.add(marker)
            mapView.invalidate()

            val direction = try {
                val bearing = playerPos.bearingTo(marker.position)
                bearingToDirection(bearing)
            } catch (e: Exception) {
                "nearby"
            }

            Toast.makeText(this, "A wild $creatureName appeared to the $direction!", Toast.LENGTH_LONG).show()

            marker.setOnMarkerClickListener { _, _ ->
                if (isLoggedOut) return@setOnMarkerClickListener false

                AlertDialog.Builder(this)
                    .setTitle("A wild $creatureName appeared!")
                    .setMessage("Do you want to capture it?")
                    .setPositiveButton("Capture") { _, _ ->
                        captureCreature(creatureName, creatureDrawableRes)
                        if (::mapView.isInitialized && !isLoggedOut) {
                            mapView.overlays.remove(marker)
                            activeCreatures.remove(marker)
                            mapView.invalidate()
                        }
                    }
                    .setNegativeButton("Ignore", null)
                    .show()
                true
            }

            // Schedule next spawn
            if (!isLoggedOut && ::mapView.isInitialized) {
                mapView.postDelayed({
                    if (!isLoggedOut) {
                        spawnRandomCreature()
                    }
                }, 30000)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error spawning creature", e)
        }
    }

    private fun bearingToDirection(bearing: Double): String {
        val b = (bearing + 360) % 360
        return when {
            b >= 337.5 || b < 22.5 -> "North"
            b >= 22.5 && b < 67.5 -> "Northeast"
            b >= 67.5 && b < 112.5 -> "East"
            b >= 112.5 && b < 157.5 -> "Southeast"
            b >= 157.5 && b < 202.5 -> "South"
            b >= 202.5 && b < 247.5 -> "Southwest"
            b >= 247.5 && b < 292.5 -> "West"
            b >= 292.5 && b < 337.5 -> "Northwest"
            else -> "Unknown"
        }
    }

    private fun captureCreature(name: String, sprite: Int) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.creatureDao().insert(Creature(name = name, sprite = sprite))
                }
                Toast.makeText(this@MainActivity, "$name captured!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to capture creature", e)
                Toast.makeText(this@MainActivity, "Failed to capture $name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCapturedCreatures() {
        lifecycleScope.launch(Dispatchers.IO) {
            val creatures = db.creatureDao().getAll()
            val names = creatures.map { it.name }.toTypedArray()
            withContext(Dispatchers.Main) {
                if (names.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No creatures captured yet.", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Captured Creatures")
                    .setItems(names, null)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    fun cleanupOnLogout() {
        isLoggedOut = true

        // Cancel all delayed tasks FIRST
        if (::mapView.isInitialized) {
            mapView.handler?.removeCallbacksAndMessages(null)
        }

        // Unregister sensors
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }

        // Clear creatures
        activeCreatures.clear()

        // Clean up map
        if (::mapView.isInitialized) {
            try {
                mapView.overlays.clear()
                mapView.invalidate()
                mapView.onDetach()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during map cleanup", e)
            }
        }
    }
}