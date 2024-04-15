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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;
import androidx.annotation.NonNull;

import de.baarflieger.mobilewebcam.PhotoSettings.Mode;

public class CamActivity extends AppCompatActivity
{
	private static final int REQUEST_CAMERA_PERMISSION = 1;
    public PhotoSettings mSettings = null;
    
    protected Preview mPreview = null;
	
	protected SharedPreferences mPrefs;
    
    protected Handler mHandler = new Handler();
    
    protected int mLayout = R.layout.layout;

    public DrawOnTop mDrawOnTop;
	public TextView mTextView = null;
	public TextView mCamNameView = null;
	public TextView mMotionTextView = null;	
	public TextView mNightTextView = null;	
	public LinearLayout mTextViewFrame = null;
	public RelativeLayout mCamNameViewFrame = null;
    
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // Hide the window title.
//***        requestWindowFeature(Window.FEATURE_NO_TITLE);

		mPrefs = getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		mSettings = new PhotoSettings(CamActivity.this);

		if(mSettings.mFullWakeLock)
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(mLayout);
        mPreview = (Preview)findViewById(R.id.preview);
        mPreview.SetSettings(mSettings);
        
        mSettings.EnableMobileWebCam(mSettings.mCameraStartupEnabled);
    }
    
    private PowerManager.WakeLock mWakeLock = null;
    private WifiManager.WifiLock mWifiLock = null;
    
    @Override
    public void onResume()
    {
    	super.onResume();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Intent intent = new Intent();
			String packageName = getPackageName();
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			if (!pm.isIgnoringBatteryOptimizations(packageName)) {
				intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
				intent.setData(Uri.parse("package:" + packageName));
				startActivity(intent);
			}
		}

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			// Camera permission has not been granted.
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.CAMERA},
					REQUEST_CAMERA_PERMISSION);
		} else {
			// Camera permission has been granted, continue with camera setup.
			setupCamera();
		}

        if(mWakeLock == null || !mWakeLock.isHeld())
        {
        	// get lock for preview
	        PowerManager.WakeLock old = mWakeLock;

	        PowerManager pm = (PowerManager)CamActivity.this.getSystemService(Context.POWER_SERVICE);
	        if(mSettings.mMode == Mode.BACKGROUND)
	        	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.ON_AFTER_RELEASE, "MobileWebCam");
	        else if(mSettings.mFullWakeLock)
	        	mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MobileWebCam");
	        else
	        	mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileWebCam");

	        mWakeLock.acquire();
//		    Log.v("MobileWebCam", "CamActivity WakeLock acquired!");
		    
		    if(old != null)
		    	old.release();
		    
		    WifiManager.WifiLock oldwifi = mWifiLock;
		    
			ConnectivityManager connmgr = (ConnectivityManager)CamActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
			if(connmgr.getNetworkPreference() == ConnectivityManager.TYPE_WIFI)
			{
				WifiManager wmgr = (WifiManager)CamActivity.this.getSystemService(Context.WIFI_SERVICE);
				if(mWifiLock == null || !mWifiLock.isHeld())
				{
					mWifiLock = wmgr.createWifiLock(WifiManager.WIFI_MODE_FULL, "MobileWebCam.CamActivity");
					mWifiLock.acquire();
//				    Log.v("MobileWebCam", "CamActivity mWifiLock acquired!");
				}		
			}		    
		    
		    if(oldwifi != null)
		    	oldwifi.release();
        }
        
    	if(mPreview != null)
    		mPreview.onResume();
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
    	
    	if(mPreview != null)
    		mPreview.onPause();
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();

    	if(mPreview != null)
    		mPreview.onDestroy();
    	mPreview = null;
    }
    
    public void releaseLocks()
    {
		if(mWakeLock != null)
		{
			// release lock for preview
			PowerManager.WakeLock tmp = mWakeLock;
//			Log.v("MobileWebCam", "CamActivity WakeLock released!");
			mWakeLock = null;

			if(tmp.isHeld())
				tmp.release();

			WifiManager.WifiLock tmpwifi = mWifiLock;
//			Log.v("MobileWebCam", "CamActivity WifiLock released!");
			mWifiLock = null;

			if(tmpwifi != null && tmpwifi.isHeld())
				tmpwifi.release();
		}
    }
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_CAMERA_PERMISSION) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Camera permission has been granted, preview can be displayed
				Toast.makeText(this, "Camera permission was granted.", Toast.LENGTH_SHORT).show();
				setupCamera();
			} else {
				// Camera permission was denied, disable camera functionality or inform the user.
				Toast.makeText(this, "Camera permission request was denied.", Toast.LENGTH_SHORT).show();
			}
		}
	}

	// Method to explicitly start the camera preview
	public void startCameraPreview() {
		if (mPreview != null) {
			mPreview.startPreview(); // Assume startPreview() is a method you implement in Preview class
		}
	}

	// Method to explicitly stop the camera preview
	public void stopCameraPreview() {
		if (mPreview != null) {
			mPreview.stopPreview(); // Assume stopPreview() is a method you implement in Preview class
		}
	}

	private void setupCamera() {
		if (mPreview != null) {
			mPreview.onResume();
		}
		// Add any additional camera setup code here.
	}
}