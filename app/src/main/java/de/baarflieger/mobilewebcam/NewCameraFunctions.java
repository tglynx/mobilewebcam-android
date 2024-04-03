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

import android.hardware.Camera;
import android.hardware.Camera.Size;

import java.util.List;

public class NewCameraFunctions
{
	private static boolean mNewCameraAvailable; // front camera API level 9
	private static boolean mPictureSizesAvailable; // picture sizes API level 5
	
	/* establish whether the "new" class is available to us */
	static
	{
		try
		{
			NewCameraWrapper.checkAvailable();
			mNewCameraAvailable = true;
		}
		catch (Throwable t)
		{
			mNewCameraAvailable = false;
		}

		try
		{
			NewPictureSizesWrapper.checkAvailable();
			mPictureSizesAvailable = true;
		}
		catch (Throwable t)
		{
			mPictureSizesAvailable = false;
		}
	}

	public static List<Size> getSupportedPictureSizes(Camera.Parameters params)
	{
		if(mPictureSizesAvailable)
			return NewPictureSizesWrapper.getSupportedPictureSizes(params);

		return null;
	}

	public static void setPictureSize(Camera.Parameters params, int w, int h)
	{
		if(mPictureSizesAvailable)
			NewPictureSizesWrapper.setPictureSize(params, w, h);
	}
	
	public static int getNumberOfCameras()
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.getNumberOfCameras();
		
		return 1;
	}
	
	public static boolean isFrontCamera(int camIdx)
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.isFrontCamera(camIdx);

		return false;
	}

	public static Camera openFrontCamera()
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.openFrontCamera();

		return null;
	}
	
	public static boolean isZoomSupported(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.isZoomSupported(params);
		
		return false;
	}
	
	public static void setZoom(Camera.Parameters params, int zoom)
	{
		if(mNewCameraAvailable)
			NewCameraWrapper.setZoom(params, zoom);
	}

	public static List<String> getSupportedWhiteBalance(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewPictureSizesWrapper.getSupportedWhiteBalance(params);
		
		return null;
	}
	
	public static void setWhiteBalance(Camera.Parameters params, String balance)
	{
		if(mNewCameraAvailable)
			NewPictureSizesWrapper.setWhiteBalance(params, balance);
	}

	public static List<String> getSupportedSceneModes(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewPictureSizesWrapper.getSupportedSceneModes(params);
		
		return null;
	}
	
	public static void setSceneMode(Camera.Parameters params, String mode)
	{
		if(mNewCameraAvailable)
			NewPictureSizesWrapper.setSceneMode(params, mode);
	}
	
	public static List<String> getSupportedColorEffects(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewPictureSizesWrapper.getSupportedColorEffects(params);
		
		return null;
	}
	
	public static void setColorEffect(Camera.Parameters params, String mode)
	{
		if(mNewCameraAvailable)
			NewPictureSizesWrapper.setColorEffect(params, mode);
	}
	
	public static int getMinExposureCompensation(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.getMinExposureCompensation(params);
		
		return 0;
	}
	
	public static int getMaxExposureCompensation(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.getMaxExposureCompensation(params);
		
		return 0;
	}
	
	public static void setExposureCompensation(Camera.Parameters params, int value)
	{
		if(mNewCameraAvailable)
			NewCameraWrapper.setExposureCompensation(params, value);
	}	
	
	public static boolean isFlashSupported(Camera.Parameters params)
	{
		if(mNewCameraAvailable)
			return NewCameraWrapper.isFlashSupported(params);
		
		return false;
	}
	
	public static void setFlash(Camera.Parameters params, String flashmode)
	{
		if(mNewCameraAvailable)
			NewCameraWrapper.setFlash(params, flashmode);
	}
}