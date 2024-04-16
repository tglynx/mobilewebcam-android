package de.baarflieger.mobilewebcam;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class CameraView extends Activity implements SurfaceHolder.Callback, OnClickListener {
	private static final String TAG = "CameraTest";
	private CameraDevice mCameraDevice;
	private CameraCaptureSession mCaptureSession;
	private HandlerThread mBackgroundThread;
	private Handler mBackgroundHandler;

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cameraview);
		mSurfaceView = findViewById(R.id.surface_camera);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setKeepScreenOn(true);
	}

	@Override
	public void onClick(View v) {
		takePicture();
	}

	private void takePicture() {
		// Method implementation to take a picture using Camera2 API
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		startBackgroundThread();
		openCamera();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Adjust the camera settings or configurations if necessary when surface changes
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		stopCamera();
	}

	private void openCamera() {
		CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		try {
			String cameraId = manager.getCameraIdList()[0]; // Assume the rear camera if not specifying front
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				return; // Permissions should be handled outside this class
			}
			manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
		} catch (CameraAccessException e) {
			Log.e(TAG, "Camera access exception", e);
		}
	}

	private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
		@Override
		public void onOpened(@NonNull CameraDevice camera) {
			mCameraDevice = camera;
			createCameraPreviewSession();
		}

		@Override
		public void onDisconnected(@NonNull CameraDevice camera) {
			camera.close();
		}

		@Override
		public void onError(@NonNull CameraDevice camera, int error) {
			camera.close();
			mCameraDevice = null;
		}
	};

	private void createCameraPreviewSession() {
		try {
			CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			builder.addTarget(mSurfaceHolder.getSurface());
			mCameraDevice.createCaptureSession(java.util.Arrays.asList(mSurfaceHolder.getSurface()),
					new CameraCaptureSession.StateCallback() {
						@Override
						public void onConfigured(@NonNull CameraCaptureSession session) {
							mCaptureSession = session;
							try {
								mCaptureSession.setRepeatingRequest(builder.build(), null, mBackgroundHandler);
							} catch (CameraAccessException e) {
								Log.e(TAG, "CameraAccessException", e);
							}
						}

						@Override
						public void onConfigureFailed(@NonNull CameraCaptureSession session) {
							Toast.makeText(CameraView.this, "Configuration failed", Toast.LENGTH_SHORT).show();
						}
					}, null);
		} catch (CameraAccessException e) {
			Log.e(TAG, "Camera access exception", e);
		}
	}

	private void stopCamera() {
		if (mCaptureSession != null) {
			mCaptureSession.close();
			mCaptureSession = null;
		}
		if (mCameraDevice != null) {
			mCameraDevice.close();
			mCameraDevice = null;
		}
		stopBackgroundThread();
	}

	private void startBackgroundThread() {
		mBackgroundThread = new HandlerThread("Camera Background");
		mBackgroundThread.start();
		mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
	}

	private void stopBackgroundThread() {
		mBackgroundThread.quitSafely();
		try {
			mBackgroundThread.join();
			mBackgroundThread = null;
			mBackgroundHandler = null;
		} catch (InterruptedException e) {
			Log.e(TAG, "Interrupted while stopping background thread", e);
		}
	}
}