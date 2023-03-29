/*
 * Copyright (C) 2022 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.live.streampack

import android.Manifest
import android.content.Context
import android.view.Surface
import androidx.annotation.RequiresPermission
import io.github.thibaultbee.streampack.ext.srt.streamers.interfaces.ISrtLiveStreamer
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.interfaces.ICameraStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.ILiveStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.IStreamer
import io.github.thibaultbee.streampack.streamers.interfaces.settings.IBaseCameraStreamerSettings
import io.github.thibaultbee.streampack.utils.CameraSettings
import io.github.thibaultbee.streampack.utils.getBackCameraList
import io.github.thibaultbee.streampack.utils.getFrontCameraList
import io.github.thibaultbee.streampack.utils.isBackCamera

class StreamerManager(
    private val context: Context,
    val configuration: Configuration
) {
    var streamer: IStreamer? = null

    var onErrorListener: OnErrorListener?
        get() = streamer?.onErrorListener
        set(value) {
            streamer?.onErrorListener = value
        }

    var onConnectionListener: OnConnectionListener?
        get() = getLiveStreamer()?.onConnectionListener
        set(value) {
            getLiveStreamer()?.onConnectionListener = value
        }

    val cameraId: String?
        get() = getCameraStreamer()?.camera

    private inline fun <reified T> getStreamer(): T? {
        return if (streamer is T) {
            streamer as T
        } else {
            null
        }
    }

    private fun getBaseStreamer(): IStreamer? {
        return getStreamer<IStreamer>()
    }

    fun getCameraStreamer(): ICameraStreamer? {
        return getStreamer<ICameraStreamer>()
    }

    private fun getSrtLiveStreamer(): ISrtLiveStreamer? {
        return getStreamer<ISrtLiveStreamer>()
    }

    private fun getLiveStreamer(): ILiveStreamer? {
        return getStreamer<ILiveStreamer>()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun rebuildStreamer() {
        streamer = StreamerFactory(context, configuration).build()
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startPreview(previewSurface: Surface) {
        getCameraStreamer()?.startPreview(previewSurface)
    }

    fun stopPreview() {
        getCameraStreamer()?.stopPreview()
    }

    suspend fun startStream() {
        when(configuration.protocol) {
            LivestreamProtocolType.SRT ->
                getSrtLiveStreamer()?.let {
                    it.streamId = configuration.endpoint.srt.streamID
                    it.passPhrase =
                        configuration.endpoint.srt.passPhrase
                    it.connect(
                        configuration.endpoint.srt.ip,
                        configuration.endpoint.srt.port
                    )
                }
            LivestreamProtocolType.RTMP ->
                getLiveStreamer()?.connect(
                    configuration.endpoint.rtmp.url
                )
        }
        streamer?.startStream()
    }

    fun stopStream() {
        streamer?.stopStream()
        getLiveStreamer()?.disconnect()
    }

    fun release() {
        streamer?.release()
    }

    fun toggleCamera() {
        getCameraStreamer()?.let {
            if (context.isBackCamera(it.camera)) {
                it.camera = context.getFrontCameraList()[0]
            } else {
                it.camera = context.getBackCameraList()[0]
            }
        }
    }

    val cameraSettings: CameraSettings?
        get() {
            val settings = getBaseStreamer()?.settings
            return if (settings is IBaseCameraStreamerSettings) {
                settings.camera
            } else {
                null
            }
        }

    var isMuted: Boolean
        get() = getBaseStreamer()?.settings?.audio?.isMuted ?: true
        set(value) {
            getBaseStreamer()?.settings?.audio?.isMuted = value
        }

    val isFrontCamera: Boolean =
        getCameraStreamer()?.let { context.isBackCamera(it.camera) } ?: true
}