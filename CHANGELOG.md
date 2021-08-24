Change Log
==========

Version 1.0.0
-------------

## Bug fixes:
- App: catch exception on stopStream, stopPreview, release,... to avoid crash when a streamer cannot be created.

## Features:
- Add a SRT stream Id set/get API
- Add Audio and Video configuration builder API
- Add streamers builder
- Add a configuration helper `CameraStreamerConfigurationHelper` for `BaseCameraStreamer`. It replaces `CodecUtils` and most configuration classes.

## API changes:
- `BaseCaptureStreamer` has been renamed `BaseCameraStreamer` (`CaptureSrtLiveStreamer` -> `CameraSrtLiveStreamer`,...)

Version 0.8.0
-------------

## Bug fixes:
- Fix [issue 11](https://github.com/ThibaultBee/StreamPack/issues/11): Crash on display orientation on Android 11.
- Fix timestamps when camera timestamp source is [SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME](https://developer.android.com/reference/android/hardware/camera2/CameraMetadata#SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME)
- Fix microphone timestamp source on Android >= N
- Fix TS packet when stuffing length == 1

## Features:
- Dispatch SRT connect API with kotlin coroutines
- Add camera torch switch on/off

## API changes:
- `CaptureSrtLiveStreamer` `connect` and `startStream``must be called in a kotlin coroutine.

Version 0.7.0
-------------

Initial release
