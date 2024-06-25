/* Copyright 2012 Michael Haar

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package de.baarflieger.mobilewebcam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.graphics.ImageFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.widget.Toast;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

@SuppressLint("Instantiatable")
public class PhotoService {
	private Handler mHandler = new Handler();

	private Context mContext = null;

	private PhotoSettings mSettings = null;

	private ITextUpdater mTextUpdater = null;

	//NEW TAKEPICTURE VARIABLES
	private String mPhotoEvent;
	private Handler mBackgroundHandler;
	private CameraDevice mCameraDevice;
	private CameraCaptureSession mCaptureSession;
	private ImageReader mImageReader;

	public PhotoService(Context c, ITextUpdater tu) {
		mContext = c;
		mTextUpdater = tu;

		mSettings = new PhotoSettings(mContext);
	}

	public static volatile Camera mCamera = null;

	private static CameraManager cameraManager;
	private static Handler backgroundHandler;

	public static boolean CheckHiddenCamInit(Context context) {
		if (!Preview.mPhotoLock.getAndSet(true)) {
			Preview.mPhotoLockTime = System.currentTimeMillis();

			cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
			HandlerThread handlerThread = new HandlerThread("CameraBackground");
			handlerThread.start();
			backgroundHandler = new Handler(handlerThread.getLooper());

			final CountDownLatch latch = new CountDownLatch(1);
			final boolean[] isPhotoTaken = {false};

			try {
				String cameraId = cameraManager.getCameraIdList()[0];

				if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
					return false;
				}
				cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
					@Override
					public void onOpened(CameraDevice camera) {
						try {
							ImageReader reader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1);
							Surface surface = reader.getSurface();
							CaptureRequest.Builder captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
							captureRequestBuilder.addTarget(surface);

							reader.setOnImageAvailableListener(imageReader -> {
								imageReader.acquireLatestImage().close();
								isPhotoTaken[0] = true;
								camera.close();
								latch.countDown();
							}, backgroundHandler);

							camera.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
								@Override
								public void onConfigured(CameraCaptureSession session) {
									try {
										session.capture(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
											@Override
											public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
												super.onCaptureCompleted(session, request, result);
												// Additional confirmation if needed
											}
										}, backgroundHandler);
									} catch (CameraAccessException e) {
										e.printStackTrace();
									}
								}

								@Override
								public void onConfigureFailed(CameraCaptureSession session) {
									camera.close();
									latch.countDown();
								}
							}, backgroundHandler);
						} catch (CameraAccessException e) {
							camera.close();
							latch.countDown();
						}
					}

					@Override
					public void onDisconnected(CameraDevice camera) {
						camera.close();
						latch.countDown();
					}

					@Override
					public void onError(CameraDevice camera, int error) {
						camera.close();
						latch.countDown();
					}
				}, backgroundHandler);
			} catch (CameraAccessException e) {
				e.printStackTrace();
				return false;
			}

			try {
				latch.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}

			Preview.mPhotoLock.set(false);
			return isPhotoTaken[0];
		}
		return false;
	}

	public void TakePictureLegacy(String event)
	{
		Log.i("MobileWebCam", "TakePicture");
		mCamera = null;
		if(Preview.mPhotoLock.getAndSet(true))
		{
			Log.w("MobileWebCam", "Photo locked!");
			return;
		}
		Preview.mPhotoLockTime = System.currentTimeMillis();

		try
		{
			if(mSettings.mFrontCamera || NewCameraFunctions.getNumberOfCameras() == 1 && NewCameraFunctions.isFrontCamera(0))
			{
				Log.v("MobileWebCam", "Trying to open CAMERA 1!");
				mCamera = NewCameraFunctions.openFrontCamera();
			}

			if(mCamera == null)
			{
				try
				{
					mCamera = Camera.open();
				}
				catch(RuntimeException e)
				{
					e.printStackTrace();
					if(e.getMessage() != null)
					{
						MobileWebCam.LogE(e.getMessage());
						mTextUpdater.Toast(e.getMessage(), Toast.LENGTH_SHORT);
					}
				}
			}
			if(mCamera != null)
			{
				mCamera.setErrorCallback(new Camera.ErrorCallback()
				{
					@Override
					public void onError(int error, Camera camera)
					{
						if(error != 0) // Samsung Galaxy S returns 0? https://groups.google.com/forum/?fromgroups=#!topic/android-developers/ZePJqveaExk
						{
							MobileWebCam.LogE("Camera TakePicture error: " + error);
							mCamera = null;
							camera.setPreviewCallback(null);
							camera.stopPreview();
							camera.release();
							System.gc();
							mTextUpdater.JobFinished();
						}
					}
				});
			}
		}
		catch(RuntimeException e)
		{
			e.printStackTrace();
			if(e.getMessage() != null)
			{
				mTextUpdater.Toast(e.getMessage(), Toast.LENGTH_SHORT);
				MobileWebCam.LogE(e.getMessage());
			}
		}
		if(mCamera != null)
		{
			mCamera.startPreview();

			if(!mSettings.mShutterSound)
			{
				AudioManager mgr = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
				mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			}

			Camera.Parameters params = mCamera.getParameters();
			if(params != null)
			{
				if(mSettings.mImageSize == 4 || mSettings.mImageSize == 5)
				{
	        		List<Camera.Size> sizes = NewCameraFunctions.getSupportedPictureSizes(params);
	        		if(sizes != null)
	        		{
	        			params.setPictureSize(sizes.get(0).width, sizes.get(0).height);
	        			if(mSettings.mImageSize == 5)
	        			{
	        				// find best matching size (next larger)
	        				for(int i = sizes.size() - 1; i >= 0; i--)
	        				{
	        					Camera.Size s = sizes.get(i);
	        					if(s.width >= mSettings.mCustomImageW && s.height >= mSettings.mCustomImageH)
	        					{
		        					params.setPictureSize(s.width, s.height);
		        					break;
	        					}
	        				}
	        			}
	        		}
				}
				mSettings.SetCameraParameters(params, false);
        		mCamera.setParameters(params);
			}

			MobileWebCam.gLastMotionKeepAliveTime = System.currentTimeMillis();

			Log.i("MobileWebCam", "takePicture");
/*			if(mSettings.mAutoFocus)
				mCamera.autoFocus(autofocusCallback);
			else*/
			try
			{
				photoCallback.mPhotoEvent = event;
				mCamera.takePicture(shutterCallback, null, photoCallback);
				Log.i("MobileWebCam", "takePicture done");
			}
			catch(RuntimeException e)
			{
				MobileWebCam.LogE("takePicture failed!");
				e.printStackTrace();
				Preview.mPhotoLock.set(false);

				//we try THIS (test CameraView.java)
				/*Intent i = new Intent(mContext, CameraView.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				try {
					mContext.startActivity(i);
				} catch (Exception ex) {
					ex.printStackTrace();
				}*/
			}
		}
		else
		{
			mTextUpdater.Toast("Error: unable to open camera", Toast.LENGTH_SHORT);
			Preview.mPhotoLock.set(false);
		}
	}

	//START TAKEPICTURE
	public void TakePicture(String event) {

		Log.i("MobileWebCam", "TakePicture");
		if(Preview.mPhotoLock.getAndSet(true))
		{
			Log.w("MobileWebCam", "Photo locked!");
			return;
		}
		Preview.mPhotoLockTime = System.currentTimeMillis();

		mPhotoEvent = event;
		initializeCamera();
	}

	@SuppressLint("MissingPermission")
	private void initializeCamera() {
		CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
		try {
			String cameraId = getCameraId(manager, mSettings.mFrontCamera);
			if (cameraId != null) {
				manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
			} else {
				mTextUpdater.Toast("Suitable camera not found", Toast.LENGTH_SHORT);
				throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Suitable camera not found");
			}
		} catch (CameraAccessException e) {
			Preview.mPhotoLock.set(false);
			mTextUpdater.Toast(e.getMessage(), Toast.LENGTH_SHORT);
			Log.e("MobileWebCam", "Cannot access the camera.", e);
		}
	}

	private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
		@Override
		public void onOpened(CameraDevice cameraDevice) {
			mCameraDevice = cameraDevice;

			CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

			try {
				CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
				// Get supported picture sizes for JPEG format
				StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				android.util.Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);

				// Select a size - for example, the largest available size.
				android.util.Size selectedSize = Collections.max(Arrays.asList(sizes), new CompareSizesByArea());

				if (mSettings.mImageSize == 5) {
					// Find best matching size (next larger or exact size)
					for (android.util.Size size : sizes) {
						if (size.getWidth() >= mSettings.mCustomImageW && size.getHeight() >= mSettings.mCustomImageH) {
							selectedSize = size;
							break;
						}
					}
				}

				createCameraPreviewSession(selectedSize);
			} catch (CameraAccessException e) {
				e.printStackTrace();
				Preview.mPhotoLock.set(false);
				mTextUpdater.Toast(e.getMessage(), Toast.LENGTH_SHORT);
			}
		}

		@Override
		public void onDisconnected(CameraDevice cameraDevice) {

			cameraDevice.close();
			Preview.mPhotoLock.set(false);

		}

		@Override
		public void onError(CameraDevice cameraDevice, int error) {
			cameraDevice.close();
			mCameraDevice = null;
			Preview.mPhotoLock.set(false);
		}
	};

	private void createCameraPreviewSession(android.util.Size selectedSize) {
		try {
			mImageReader = ImageReader.newInstance(selectedSize.getWidth(), selectedSize.getHeight(), ImageFormat.JPEG, 1); // Adjust size as needed

			// Set the listener to handle new images.
			mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
				@Override
				public void onImageAvailable(ImageReader reader) {
					try (Image image = reader.acquireNextImage()) {
						ByteBuffer buffer = image.getPlanes()[0].getBuffer();
						byte[] bytes = new byte[buffer.remaining()];
						buffer.get(bytes);

						// Process the image data as needed
						MobileWebCam.gLastMotionKeepAliveTime = System.currentTimeMillis();
						processImage(bytes, image.getWidth(), image.getHeight());
					}
				}
			}, mBackgroundHandler);

			Surface surface = mImageReader.getSurface();
			final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
			builder.addTarget(surface);

			CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());

			// Check if the camera supports auto-exposure compensation
			Range<Integer> aeCompensationRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			if (aeCompensationRange != null) {
				int compensationValue = -4; // Define this based on your needs
				if (aeCompensationRange.contains(compensationValue)) {
					builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, compensationValue);
				}
			}

			mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
				@Override
				public void onConfigured(CameraCaptureSession session) {
					mCaptureSession = session;
					capturePhoto(builder);
				}

				@Override
				public void onConfigureFailed(CameraCaptureSession session) {
					Preview.mPhotoLock.set(false);
					Log.e("MobileWebCam", "Failed to configure camera.");
				}
			}, mBackgroundHandler);

		} catch (CameraAccessException e) {
			Preview.mPhotoLock.set(false);
			Log.e("MobileWebCam", "Failed to create capture session.", e);
		}
	}

	private void capturePhoto(CaptureRequest.Builder builder) {
		try {
			mCaptureSession.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
				@Override
				public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
					super.onCaptureCompleted(session, request, result);
					Log.i("MobileWebCam", "Image captured for event: " + mPhotoEvent);
					// Here you might handle the image data as it ends up
				}
			}, mBackgroundHandler);
		} catch (CameraAccessException e) {
			mTextUpdater.Toast(e.getMessage(), Toast.LENGTH_SHORT);
			Log.e("MobileWebCam", "Failed to capture photo.", e);
		}
	}

	private void processImage(byte[] imageData, int width, int height) {
		Date date = new Date(); // Capture the current time.
		Log.i("MobileWebCam", "Image captured");

		Size imageSize = new Size(width, height); // Using the custom Size class to stay somewhat compatible with the legacy camera.size
		WorkImage work = new WorkImage(mContext, mTextUpdater, imageData, imageSize, date, mPhotoEvent);
		MobileWebCam.gPictureCounter++;

		// Start the work on a new thread.
		new Thread(work).start();

		Preview.mPhotoLock.set(false);
		Log.i("MobileWebCam", "Work submitted " + MobileWebCam.gPictureCounter);

	}

	private String getCameraId(CameraManager manager, boolean useFrontCamera) throws CameraAccessException {
		String[] cameraIdList = manager.getCameraIdList();
		for (String cameraId : cameraIdList) {
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
			Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
			if (lensFacing != null) {
				if ((useFrontCamera && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) ||
						(!useFrontCamera && lensFacing == CameraCharacteristics.LENS_FACING_BACK)) {
					return cameraId;
				}
			}
		}
		return null;
	}
	// END TAKEPICTURE


	Camera.AutoFocusCallback autofocusCallback = new Camera.AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean success, Camera camera)
		{
			Log.i("MobileWebCam", "takePicture onAutoFocus");

			// take picture now
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					Log.i("MobileWebCam", "takePicture onAutoFocus.run");

					if(mCamera != null)
						mCamera.takePicture(shutterCallback, null, photoCallback);

					Log.i("MobileWebCam", "takePicture onAutoFocus.run takePicture done");
				}
			});
		}
	};

	Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback()
	{
		public void onShutter()
		{
			// no sound?
		}
	};
	
	public static abstract class EventPictureCallback implements Camera.PictureCallback
	{
		public String mPhotoEvent = null;
	}

	EventPictureCallback photoCallback = new EventPictureCallback()
	{
		public void onPictureTaken(byte[] data, Camera camera)
		{
			Date date = new Date(); // first store current time!
			
			WorkImage work = null;

			Log.i("MobileWebCam", "onPictureTaken");
			Camera.Parameters parameters = camera.getParameters();
			Camera.Size s = parameters.getPictureSize();
			if(s != null)
			{

				Size imageSize = new Size(s.width, s.height);
				work = new WorkImage(mContext, mTextUpdater, data, imageSize, date, mPhotoEvent);
				MobileWebCam.gPictureCounter++;
				
				mTextUpdater.UpdateText();
				
//				mHandler.post(work);
				Log.i("MobileWebCam", "work to do " + MobileWebCam.gPictureCounter);

				
				// now start to work on the data
				if(work != null)
					new Thread(work).start();
			}

			if(mCamera != null)
			{
				mCamera.startPreview();
			
				if(ControlReceiver.takePicture())
				{
					// PHOTO intent requested several pictures!
					mCamera.takePicture(shutterCallback, null, photoCallback);
					Log.i("MobileWebCam", "another takePicture done");
					return; // do not yet shut camera down!
				}
			}
			
			Log.i("MobileWebCam", "onPictureTaken end");			
			
			AudioManager mgr = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);			
			
			mHandler.post(new Runnable()
				{
					@Override
					public void run()
					{
						if(mCamera != null)
						{
							Camera c = mCamera;
							mCamera = null;
							c.setPreviewCallback(null);
							c.stopPreview();
							c.release();
							System.gc();
						}
						
						Log.i("MobileWebCam", "takePicture finished");
					}
				});
		}
	};

}

