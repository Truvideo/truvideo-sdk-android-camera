# Manual Testing Steps

To verify that recording continues when the camera activity is closed under memory pressure:

1. Build and install the sample app.
2. Launch `TruvideoSdkCameraActivity` and start a video recording.
3. While recording, trigger an activity destruction:
   - Enable **Developer Options > Don't keep activities** and switch away from the app, or
   - Use the recent-apps screen to dismiss the activity.
4. Wait a few seconds and reopen the app.
5. Observe that the recording is still in progress and the preview is restored.

This confirms that `CameraForegroundService` keeps the recording alive even if the activity is destroyed.
