package com.colombianapps.toonizapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import com.colombianapps.toonizapp.ui.viewmodel.StudioViewModel
import java.io.File

object Mp4ExportManager {

    fun exportToMp4(
        context: Context,
        viewModel: StudioViewModel,
        onProgress: (Float) -> Unit
    ): File {
        val project = viewModel.currentProject ?: throw IllegalStateException("No hay proyecto activo")

        // Ensure even width/height dimensions required by H.264 encoder
        val width = if (project.canvasWidth % 2 == 0) project.canvasWidth else project.canvasWidth - 1
        val height = if (project.canvasHeight % 2 == 0) project.canvasHeight else project.canvasHeight - 1
        val fps = project.fps.coerceAtLeast(1)

        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir, "ToonizExports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val cleanName = project.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val outputFile = File(exportDir, "${cleanName}_Animacion.mp4")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000) // 8 Mbps bitrate
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(mime)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val maxFrame = viewModel.getMaxFrameWithStrokes()
        val layers = viewModel.layers
        val bufferInfo = MediaCodec.BufferInfo()

        val composedBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val composedCanvas = Canvas(composedBmp)

        for (f in 1..maxFrame) {
            // 1. Compose white background + layer bitmaps for frame f
            composedCanvas.drawColor(Color.WHITE)
            for (layer in layers) {
                val bmp = viewModel.drawingManager.findBitmap(layer.id.toLong(), f)
                if (bmp != null && !bmp.isRecycled) {
                    composedCanvas.drawBitmap(bmp, 0f, 0f, null)
                }
            }

            // 2. Render composed frame to encoder surface
            val surfaceCanvas = inputSurface.lockHardwareCanvas()
            surfaceCanvas.drawBitmap(composedBmp, 0f, 0f, null)
            inputSurface.unlockCanvasAndPost(surfaceCanvas)

            // 3. Drain encoder buffers
            drainEncoder(encoder, muxer, bufferInfo, false, trackIndex, muxerStarted) { newTrack, started ->
                trackIndex = newTrack
                muxerStarted = started
            }

            onProgress(f.toFloat() / maxFrame)
        }

        composedBmp.recycle()

        // Signal End of Stream
        encoder.signalEndOfInputStream()
        drainEncoder(encoder, muxer, bufferInfo, true, trackIndex, muxerStarted) { _, _ -> }

        encoder.stop()
        encoder.release()
        inputSurface.release()

        if (muxerStarted) {
            muxer.stop()
            muxer.release()
        }

        return outputFile
    }

    private inline fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        initialTrackIndex: Int,
        initialMuxerStarted: Boolean,
        onMuxerState: (Int, Boolean) -> Unit
    ) {
        var trackIndex = initialTrackIndex
        var muxerStarted = initialMuxerStarted

        while (true) {
            val status = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) {
                    throw RuntimeException("El formato del encoder cambió dos veces")
                }
                val newFormat = encoder.outputFormat
                trackIndex = muxer.addTrack(newFormat)
                muxer.start()
                muxerStarted = true
                onMuxerState(trackIndex, muxerStarted)
            } else if (status >= 0) {
                val encodedData = encoder.getOutputBuffer(status)
                if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if (bufferInfo.size != 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                }
                encoder.releaseOutputBuffer(status, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }
}
