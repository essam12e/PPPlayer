package com.ppplayer.app.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ppplayer.app.PPPlayerApp

/** Keeps playback alive outside the player screen and publishes the media notification. */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = PlayerHolder.ensure(this)
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player).setSessionActivity(open).build()
        PlayerHolder.onPlayerReplaced = { session?.player = it }
        PlayerHolder.onBeforeRelease = {
            releaseSession()
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        val settings = (application as PPPlayerApp).prefs.settings.value
        val keepGoing = player != null && player.playWhenReady && player.mediaItemCount > 0 &&
            (settings.backgroundPlay || player.currentMediaItem?.isAudio == true)
        if (!keepGoing) {
            PlayerHolder.release()
            stopSelf()
        }
    }

    override fun onDestroy() {
        releaseSession()
        if (!PlayerHolder.activityAttached && PlayerHolder.player.value?.isPlaying != true) {
            PlayerHolder.release()
        }
        super.onDestroy()
    }

    private fun releaseSession() {
        PlayerHolder.onPlayerReplaced = null
        PlayerHolder.onBeforeRelease = null
        session?.release()
        session = null
    }
}
