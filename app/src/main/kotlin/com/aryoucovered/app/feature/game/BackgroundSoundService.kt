package com.aryoucovered.app.feature.game

import com.aryoucovered.app.R
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder


class BackgroundSoundService : Service() {
    var player: MediaPlayer? = null
    override fun onBind(arg0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        player = MediaPlayer.create(this, R.raw.bgmusic)
        player!!.setLooping(true) // Set looping
        player!!.setVolume(1f, 1f)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        player!!.start()
        return Service.START_STICKY
    }

    fun onUnBind(arg0: Intent?): IBinder? {
        // TO DO Auto-generated method
        return null
    }

    fun onStop() {
    }

    fun onPause() {
    }

    public override fun onDestroy() {
        player!!.stop()
        player!!.release()
    }

    public override fun onLowMemory() {
    }

    companion object {
        private val TAG: String? = null
    }
}