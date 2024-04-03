package de.baarflieger.mobilewebcam;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.Camera.Size;

import java.util.List;

@TargetApi(5)
public class NewPictureSizesWrapper
{
	/* calling here forces class initialization */
	public static void checkAvailable() {}

	public static List<Size> getSupportedPictureSizes(Camera.Parameters params)
	{
		try
		{
			return params.getSupportedPictureSizes();
		}
		catch(NoSuchMethodError e)
		{
		}	
		
		return null;
	}

	public static void setPictureSize(Camera.Parameters params, int w, int h)
	{
		try
		{
			params.setPictureSize(w, h);
		}
		catch(NoSuchMethodError e)
		{
		}	
	}


	public static List<String> getSupportedWhiteBalance(Camera.Parameters params)
	{
		try
		{
			return params.getSupportedWhiteBalance();
		}
		catch(NoSuchMethodError e)
		{
		}
		
		return null;
	}
	
	public static void setWhiteBalance(Camera.Parameters params, String balance)
	{
		try
		{
			params.setWhiteBalance(balance);
		}
		catch(NoSuchMethodError e)
		{
		}
	}	

	public static List<String> getSupportedSceneModes(Camera.Parameters params)
	{
		try
		{
			return params.getSupportedSceneModes();
		}
		catch(NoSuchMethodError e)
		{
		}
		
		return null;
	}
	
	public static void setSceneMode(Camera.Parameters params, String mode)
	{
		try
		{
			params.setSceneMode(mode);
		}
		catch(NoSuchMethodError e)
		{
		}
	}
	
	public static List<String> getSupportedColorEffects(Camera.Parameters params)
	{
		try
		{
			return params.getSupportedColorEffects();
		}
		catch(NoSuchMethodError e)
		{
		}
		
		return null;
	}
	
	public static void setColorEffect(Camera.Parameters params, String mode)
	{
		try
		{
			params.setColorEffect(mode);
		}
		catch(NoSuchMethodError e)
		{
		}
	}
}