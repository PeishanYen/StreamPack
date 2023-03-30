/*
 * Copyright (C) 2021 Thibault B.
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
package io.github.thibaultbee.streampack.screenrecorder

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions3.RxPermissions
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ACTIVITY_RESULT_KEY
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.AUDIO_CONFIG_KEY
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.BITRATE
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.BYTE_FORMAT
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.CHANNEL_CONFIG
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENABLE_BITRATE_REGULATION
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENABLE_ECHO_CANCELER
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENABLE_NOISE_SUPPRESSOR
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENDPOINT_CONFIG_KEY
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.ENDPOINT_TYPE
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.IP
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.MIME_TYPE
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.MUXER_CONFIG_KEY
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PASSPHRASE
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PORT
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.PROVIDER
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.RESOLUTION_HEIGHT
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.RESOLUTION_WIDTH
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.RTMP_URL
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.SAMPLE_RATE
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.SERVICE
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.STREAM_ID
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.VIDEO_BITRATE_REGULATION_LOWER
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.VIDEO_BITRATE_REGULATION_UPPER
import io.github.thibaultbee.streampack.screenrecorder.ScreenRecorderService.ConfigKeys.Companion.VIDEO_CONFIG_KEY
import io.github.thibaultbee.streampack.screenrecorder.databinding.ActivityMainBinding
import io.github.thibaultbee.streampack.screenrecorder.settings.SettingsActivity
import io.github.thibaultbee.streampack.streamers.bases.BaseScreenRecorderStreamer

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val tag = this::class.simpleName
    private val configuration by lazy {
        Configuration(this)
    }
    private val rxPermissions: RxPermissions by lazy { RxPermissions(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        binding.actions.setOnClickListener {
            showPopup()
        }

        binding.liveButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rxPermissions
                    .request(Manifest.permission.RECORD_AUDIO)
                    .subscribe { granted ->
                        if (!granted) {
                            showPermissionAlertDialog(this) { this.finish() }
                        } else {
                            getContent.launch(
                                BaseScreenRecorderStreamer.createScreenRecorderIntent(
                                    this
                                )
                            )
                        }
                    }
            } else {
                stopService(Intent(this, ScreenRecorderService::class.java))
            }
        }
    }

    private var getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intent = Intent(this, ScreenRecorderService::class.java)
            createAudioConfigBundle()?.let {
                intent.putExtra(AUDIO_CONFIG_KEY, it)
            }
            intent.putExtra(VIDEO_CONFIG_KEY, createVideoConfigBundle())
            intent.putExtra(MUXER_CONFIG_KEY, createMuxerConfigBundle())
            intent.putExtra(ENDPOINT_CONFIG_KEY, createEndpointConfigBundle())
            intent.putExtra(ACTIVITY_RESULT_KEY, result)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            moveTaskToBack(true)
        }

    private fun createMuxerConfigBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(PROVIDER, configuration.muxer.provider)
        bundle.putString(SERVICE, configuration.muxer.service)
        return bundle
    }

    private fun createEndpointConfigBundle(): Bundle {
        val bundle = Bundle()
        bundle.putInt(ENDPOINT_TYPE, configuration.endpoint.type.id)

        // Srt
        bundle.putString(IP, configuration.endpoint.srt.ip)
        bundle.putInt(PORT, configuration.endpoint.srt.port)
        bundle.putString(PASSPHRASE, configuration.endpoint.srt.passPhrase)
        bundle.putString(STREAM_ID, configuration.endpoint.srt.streamID)
        bundle.putBoolean(
            ENABLE_BITRATE_REGULATION,
            configuration.endpoint.srt.enableBitrateRegulation
        )
        bundle.putInt(
            VIDEO_BITRATE_REGULATION_LOWER,
            configuration.endpoint.srt.videoBitrateRange.lower
        )
        bundle.putInt(
            VIDEO_BITRATE_REGULATION_UPPER,
            configuration.endpoint.srt.videoBitrateRange.upper
        )

        // RTMP
        bundle.putString(
            RTMP_URL,
            configuration.endpoint.rtmp.url
        )
        return bundle
    }

    private fun createAudioConfigBundle(): Bundle? {
        return if (configuration.audio.enable) {
            val bundle = Bundle()
            bundle.putString(MIME_TYPE, configuration.audio.encoder)
            bundle.putInt(BITRATE, configuration.audio.bitrate)
            bundle.putInt(SAMPLE_RATE, configuration.audio.sampleRate)
            bundle.putInt(CHANNEL_CONFIG, configuration.audio.numberOfChannels)
            bundle.putInt(BYTE_FORMAT, configuration.audio.byteFormat)
            bundle.putBoolean(ENABLE_ECHO_CANCELER, configuration.audio.enableEchoCanceler)
            bundle.putBoolean(ENABLE_NOISE_SUPPRESSOR, configuration.audio.enableNoiseSuppressor)
            bundle
        } else {
            null
        }
    }

    private fun createVideoConfigBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(MIME_TYPE, configuration.video.encoder)
        bundle.putInt(BITRATE, configuration.video.bitrate * 1000)  // to b/s
        bundle.putInt(RESOLUTION_WIDTH, configuration.video.resolution.width)
        bundle.putInt(RESOLUTION_HEIGHT, configuration.video.resolution.height)
        return bundle
    }

    private fun showPopup() {
        val popup = PopupMenu(this, binding.actions)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.actions, popup.menu)
        popup.show()
        popup.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_settings) {
                goToSettingsActivity()
            } else {
                Timber.tag(TAG).e("Unknown menu item ${it.itemId}")
            }
            true
        }
    }

    private fun goToSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actions, menu)
        return true
    }

    fun showPermissionAlertDialog(context: Context, afterPositiveButton: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle(R.string.permission)
            .setMessage(R.string.permission_not_granted)
            .setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                afterPositiveButton()
            }
            .show()
    }
}
