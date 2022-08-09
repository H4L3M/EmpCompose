package dev.halem.emp

import android.content.Context
import android.util.Pair
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.ErrorMessageProvider
import dev.halem.emp.Util.buildRenderersFactory
import dev.halem.emp.Util.getDataSourceFactory
import kotlin.math.max


open class Emp {

    class Builder(private val context: Context) : StyledPlayerView.ControllerVisibilityListener {

        lateinit var playerView: StyledPlayerView
        private var trackSelector: DefaultTrackSelector? = null

        var player: ExoPlayer? = null

        companion object {

            private var mediaItems: List<MediaItem>? = null
            private var lastSeenTrackGroupArray: TrackGroupArray? = null

            private lateinit var dataSourceFactory: DataSource.Factory
            private lateinit var trackSelectorParameters: DefaultTrackSelector.Parameters

            var lastSeenTracks: Tracks? = null

            private var startAutoPlay = false
            private var startWindow = 0
            private var startPosition: Long = 0

            private var urlSource: String = ""

        }

        fun build(): Builder {

            dataSourceFactory = getDataSourceFactory( /* context= */context)
            playerView = StyledPlayerView(context).apply {
                setErrorMessageProvider(PlayerErrorMessageProvider(context))
                requestFocus()
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            }

            return this
        }

        infix fun mediaSource(url: String): Builder {
            urlSource = url
            if (player == null) {

                lastSeenTracks = Tracks.EMPTY
                val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                trackSelector = DefaultTrackSelector(context)
                trackSelectorParameters = trackSelector!!.parameters
                lastSeenTrackGroupArray = null

                val renderersFactory = buildRenderersFactory(context, true)

                player = ExoPlayer.Builder( /* context = */context, renderersFactory)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setTrackSelector(trackSelector!!)
                    .build().apply {
                        addListener(PlayerEventListener())
                        setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus = */true)
                        playWhenReady = startAutoPlay
                        playerView.player = this
                    }
            }

            val haveStartPosition = startWindow != C.INDEX_UNSET
            if (haveStartPosition) {
                player?.seekTo(startWindow, startPosition)
            }

            val mediaItem = MediaItem.fromUri(urlSource)

            player?.let {
                it.setMediaItem(mediaItem, /* resetPosition = */ !haveStartPosition)
                it.prepare()
                it.play()
            }
            return this
        }

        fun resume() {
            playerView.onResume()
        }

        internal fun clearStartPosition() {
            startAutoPlay = true
            startWindow = C.INDEX_UNSET
            startPosition = C.TIME_UNSET
        }

        fun release() {
            playerView.onPause()
            if (player != null) {
                updateTrackSelectorParameters()
                updateStartPosition()
                mediaItems = emptyList()
                trackSelector = null
                player!!.let {
                    release()
                    null
                }
            }
        }

        private fun updateTrackSelectorParameters() {
            if (trackSelector != null) {
                trackSelectorParameters = trackSelector!!.parameters
            }
        }

        private fun updateStartPosition() {
            if (player != null) {
                player?.let {
                    startAutoPlay = it.playWhenReady
                    startWindow = it.currentMediaItemIndex
                    startPosition = max(0, it.contentPosition)
                }
            }
        }

        override fun onVisibilityChanged(visibility: Int) {
        }


        // User controls
//        private fun updateButtonVisibility() {
//            selectTracksButton.setEnabled(
//                player != null && TrackSelectionDialog.willHaveContent(
//                    player
//                )
//            )
//        }

        class PlayerEventListener : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
                if (playbackState == Player.STATE_ENDED) {
//                    showControls()
                }
//                updateButtonVisibility()
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
//                    player?.let {
//                        it.seekToDefaultPosition()
//                        it.prepare()
//                    }
                } else {
//                    updateButtonVisibility()
//                    showControls()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
//                updateButtonVisibility()
                if (tracks === lastSeenTracks) {
                    return
                }
                if (tracks.containsType(C.TRACK_TYPE_VIDEO)
                    && !tracks.isTypeSupported(
                        C.TRACK_TYPE_VIDEO,  /* allowExceedsCapabilities = */
                        true
                    )
                ) {
//                    showToast(R.string.error_unsupported_video)
                }
                if (tracks.containsType(C.TRACK_TYPE_AUDIO)
                    && !tracks.isTypeSupported(
                        C.TRACK_TYPE_AUDIO,  /* allowExceedsCapabilities = */
                        true
                    )
                ) {
//                    showToast(R.string.error_unsupported_audio)
                }
                lastSeenTracks = tracks
            }
        }

        internal class PlayerErrorMessageProvider(private val context: Context) :
            ErrorMessageProvider<PlaybackException> {
            override fun getErrorMessage(e: PlaybackException): Pair<Int, String> {
                var errorString = context.getString(R.string.error_generic)
                val cause = e.cause
                if (cause is MediaCodecRenderer.DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    errorString = if (cause.codecInfo == null) {
                        if (cause.cause is MediaCodecUtil.DecoderQueryException) {
                            context.getString(R.string.error_querying_decoders)
                        } else if (cause.secureDecoderRequired) {
                            context.getString(
                                R.string.error_no_secure_decoder,
                                cause.mimeType
                            )
                        } else {
                            context.getString(
                                R.string.error_no_decoder,
                                cause.mimeType
                            )
                        }
                    } else {
                        context.getString(
                            R.string.error_instantiating_decoder,
                            cause.codecInfo!!.name
                        )
                    }
                }
                return Pair.create(0, errorString)
            }
        }
    }
}
