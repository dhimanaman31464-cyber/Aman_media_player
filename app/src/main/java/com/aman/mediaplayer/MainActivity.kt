package com.aman.mediaplayer

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

private data class MediaItem(val name: String, val uri: Uri, val kind: String)

class MainActivity : Activity() {
    private val green = Color.rgb(30, 215, 96)
    private val bg = Color.rgb(10, 16, 12)
    private val panel = Color.rgb(24, 34, 27)
    private val muted = Color.rgb(166, 180, 169)
    private val items = mutableListOf<MediaItem>()
    private var filter = "All"
    private var player: MediaPlayer? = null
    private lateinit var list: LinearLayout
    private lateinit var nowPlaying: TextView
    private lateinit var playPause: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildScreen()
        if (hasMediaAccess()) loadMedia() else requestMediaAccess()
    }

    private fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun hasMediaAccess(): Boolean = if (Build.VERSION.SDK_INT >= 33) {
        listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES).any {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        } || (Build.VERSION.SDK_INT >= 34 &&
            checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED)
    } else checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestMediaAccess() {
        if (Build.VERSION.SDK_INT >= 23) requestPermissions(permissions(), 410)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == 410) {
            if (hasMediaAccess()) loadMedia()
            else Toast.makeText(this, "Allow media access to show your library.", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(18), dp(18), dp(18), dp(8))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "🔥  AMAN MEDIA"
            textSize = 23f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(Button(this).apply {
            text = "Rescan"
            setTextColor(green)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { if (hasMediaAccess()) loadMedia() else requestMediaAccess() }
        })
        root.addView(header)
        root.addView(TextView(this).apply {
            text = "Your music, videos & photos in one place"
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(4), 0, dp(18))
        })
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(tabs)
        buildTabs(tabs)
        root.addView(TextView(this).apply {
            text = "YOUR LIBRARY"
            textSize = 11f
            letterSpacing = 0.12f
            setTextColor(green)
            setPadding(0, dp(20), 0, dp(10))
        })
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(panel)
        }
        nowPlaying = TextView(this).apply {
            text = "Choose a song to start playing"
            textSize = 14f
            setTextColor(Color.WHITE)
            maxLines = 1
        }
        footer.addView(nowPlaying)
        playPause = Button(this).apply {
            text = "▶  Play"
            setTextColor(Color.BLACK)
            setBackgroundColor(green)
            setOnClickListener {
                val p = player
                if (p != null) {
                    if (p.isPlaying) { p.pause(); text = "▶  Play" }
                    else { p.start(); text = "Ⅱ  Pause" }
                }
            }
        }
        footer.addView(playPause)
        root.addView(footer)
        setContentView(root)
    }

    private fun buildTabs(tabs: LinearLayout) {
        tabs.removeAllViews()
        listOf("All", "Music", "Videos", "Photos").forEach { name ->
            tabs.addView(Button(this).apply {
                text = name
                textSize = 12f
                isAllCaps = false
                setTextColor(if (filter == name) Color.BLACK else Color.WHITE)
                setBackgroundColor(if (filter == name) green else panel)
                setOnClickListener { filter = name; buildTabs(tabs); renderItems() }
            }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) })
        }
    }

    private fun loadMedia() {
        items.clear()
        query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "Music", true)
        query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Videos", true)
        query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Photos", false)
        renderItems()
    }

    private fun query(uri: Uri, kind: String, hasDuration: Boolean) {
        val columns = if (hasDuration) arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED, MediaStore.Video.Media.DURATION
        ) else arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED
        )
        try {
            contentResolver.query(uri, columns, null, null,
                MediaStore.MediaColumns.DATE_ADDED + " DESC")?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol) ?: "Unnamed media"
                    items.add(MediaItem(name, ContentUris.withAppendedId(uri, id), kind))
                }
            }
        } catch (_: SecurityException) { }
    }

    private fun renderItems() {
        list.removeAllViews()
        val shown = items.filter { filter == "All" || it.kind == filter }
        if (shown.isEmpty()) {
            list.addView(TextView(this).apply {
                text = if (hasMediaAccess()) "No media found in this category." else "Media permission is needed to scan your library."
                textSize = 14f
                setTextColor(muted)
                setPadding(dp(8), dp(24), dp(8), dp(24))
            })
            return
        }
        shown.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundColor(panel)
                addView(TextView(this@MainActivity).apply {
                    text = when (item.kind) { "Music" -> "♫"; "Videos" -> "▶"; else -> "▧" }
                    textSize = 25f
                    setTextColor(green)
                    gravity = Gravity.CENTER
                }, LinearLayout.LayoutParams(dp(42), dp(46)))
                val details = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
                details.addView(TextView(this@MainActivity).apply {
                    text = item.name
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    maxLines = 1
                })
                details.addView(TextView(this@MainActivity).apply {
                    text = item.kind
                    textSize = 11f
                    setTextColor(muted)
                })
                addView(details, LinearLayout.LayoutParams(0, -2, 1f))
                setOnClickListener { openItem(item) }
            }
            list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
        }
    }

    private fun openItem(item: MediaItem) {
        if (item.kind == "Music") {
            try {
                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(this@MainActivity, item.uri)
                    setOnPreparedListener {
                        it.start()
                        nowPlaying.text = "♫  " + item.name
                        playPause.text = "Ⅱ  Pause"
                    }
                    setOnCompletionListener { playPause.text = "▶  Play" }
                    prepareAsync()
                }
                nowPlaying.text = "Loading: " + item.name
            } catch (_: Exception) {
                Toast.makeText(this, "Couldn't play this audio file.", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(item.uri, if (item.kind == "Videos") "video/*" else "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            } catch (_: Exception) {
                Toast.makeText(this, "No app available to open this file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
