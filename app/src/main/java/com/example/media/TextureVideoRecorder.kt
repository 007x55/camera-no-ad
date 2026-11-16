package com.example.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Encodes live frames from any IP camera view (RTSP TextureView or MJPEG Bitmap) into a standard
 * H.264 MP4 file using Android's hardware/software MediaCodec + MediaMuxer.
 */
class TextureVideoRecorder(
  private val outputFile: File,
  rawWidth: Int,
  rawHeight: Int,
  private val fps: Int = 15,
) {
  // Align to multiples of 16 for wide MediaCodec hardware compatibility
  val width: Int = ((rawWidth.coerceIn(320, 1280) + 15) / 16) * 16
  val height: Int = ((rawHeight.coerceIn(240, 720) + 15) / 16) * 16

  private var codec: MediaCodec? = null
  private var inputSurface: Surface? = null
  private var muxer: MediaMuxer? = null
  private var trackIndex: Int = -1
  private var muxerStarted = false
  private val running = AtomicBoolean(false)
  private var recordingJob: Job? = null
  private var startTimeNs: Long = 0L

  private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

  fun start(scope: CoroutineScope, frameProvider: () -> Bitmap?) {
    if (running.getAndSet(true)) return

    val format =
      MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(
          MediaFormat.KEY_COLOR_FORMAT,
          MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
        )
        setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
      }

    val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    val surface = mediaCodec.createInputSurface()
    mediaCodec.start()

    codec = mediaCodec
    inputSurface = surface
    muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    trackIndex = -1
    muxerStarted = false
    startTimeNs = System.nanoTime()

    val frameIntervalMs = (1000L / fps).coerceAtLeast(33L)
    val bufferInfo = MediaCodec.BufferInfo()

    recordingJob =
      scope.launch(Dispatchers.Default) {
        val dstRect = Rect(0, 0, width, height)
        while (isActive && running.get()) {
          val loopStart = System.currentTimeMillis()
          val bmp = frameProvider()
          if (bmp != null && !bmp.isRecycled) {
            drawBitmapToInputSurface(surface, bmp, dstRect)
            bmp.recycle()
            drainEncoder(mediaCodec, bufferInfo, endOfStream = false)
          }
          val elapsed = System.currentTimeMillis() - loopStart
          val wait = (frameIntervalMs - elapsed).coerceAtLeast(10L)
          delay(wait)
        }
      }
  }

  private fun drawBitmapToInputSurface(surface: Surface, bitmap: Bitmap, dstRect: Rect) {
    var canvas: Canvas? = null
    try {
      canvas = surface.lockCanvas(null)
      canvas.drawBitmap(bitmap, null, dstRect, paint)
    } catch (_: Throwable) {
    } finally {
      if (canvas != null) {
        runCatching { surface.unlockCanvasAndPost(canvas) }
      }
    }
  }

  suspend fun stop(): Long {
    if (!running.getAndSet(false)) return 0L
    recordingJob?.join()
    recordingJob = null

    val durationMs = ((System.nanoTime() - startTimeNs) / 1_000_000L).coerceAtLeast(0L)
    val mediaCodec = codec
    val bufferInfo = MediaCodec.BufferInfo()
    if (mediaCodec != null) {
      runCatching {
        mediaCodec.signalEndOfInputStream()
        drainEncoder(mediaCodec, bufferInfo, endOfStream = true)
      }
      runCatching { mediaCodec.stop() }
      runCatching { mediaCodec.release() }
    }
    codec = null
    runCatching { inputSurface?.release() }
    inputSurface = null

    val mediaMuxer = muxer
    if (mediaMuxer != null) {
      if (muxerStarted) {
        runCatching { mediaMuxer.stop() }
      }
      runCatching { mediaMuxer.release() }
    }
    muxer = null
    muxerStarted = false
    return durationMs
  }

  private fun drainEncoder(
    mediaCodec: MediaCodec,
    bufferInfo: MediaCodec.BufferInfo,
    endOfStream: Boolean,
  ) {
    val timeoutUs = if (endOfStream) 10_000L else 0L
    var attempts = 0
    while (attempts < 40) {
      attempts++
      val outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, timeoutUs)
      when {
        outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER -> {
          if (!endOfStream) break
        }
        outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
          if (!muxerStarted) {
            val newFormat = mediaCodec.outputFormat
            trackIndex = muxer?.addTrack(newFormat) ?: -1
            if (trackIndex >= 0) {
              muxer?.start()
              muxerStarted = true
            }
          }
        }
        outputBufferId >= 0 -> {
          val encodedData = mediaCodec.getOutputBuffer(outputBufferId)
          if (encodedData != null) {
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
              bufferInfo.size = 0
            }
            if (bufferInfo.size > 0 && muxerStarted && trackIndex >= 0) {
              encodedData.position(bufferInfo.offset)
              encodedData.limit(bufferInfo.offset + bufferInfo.size)
              muxer?.writeSampleData(trackIndex, encodedData, bufferInfo)
            }
            mediaCodec.releaseOutputBuffer(outputBufferId, false)
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
              break
            }
          }
        }
      }
    }
  }
}
