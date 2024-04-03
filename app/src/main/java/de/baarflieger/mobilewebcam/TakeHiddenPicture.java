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

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

// ----------------------------------------------------------------------

public class TakeHiddenPicture extends CamActivity
{
	KeyguardLock mLock = null;
	long mStartTime; 
	
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
		 WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);    	
    	
//        PowerManager pm = (PowerManager)TakeHiddenPicture.this.getSystemService(Context.POWER_SERVICE);
//        if(!pm.isScreenOn())
        {
/*	        WindowManager.LayoutParams lp = getWindow().getAttributes();
	        lp.screenBrightness = 0.0f;
	        getWindow().setAttributes(lp);
*/
//	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        mLayout = R.layout.hiddenlayout;
    	super.onCreate(savedInstanceState);

/*    	mDrawOnTop.setVisibility(View.INVISIBLE);
    	mPreview.setVisibility(View.INVISIBLE);
    	mTextView.setVisibility(View.INVISIBLE);
    	mMotionTextView.setVisibility(View.INVISIBLE);*/
    }

	@Override
	public boolean dispatchKeyEvent (KeyEvent event)
	{
		return super.dispatchKeyEvent(event);
	}
	
    @Override
    public void onResume()
    {
    	Log.v("MobileWebCam", "TakeHiddenPicture.onResume");

/*        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 0.0f;
        getWindow().setAttributes(lp);
*/
        KeyguardManager mgr = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
    	if(mgr.inKeyguardRestrictedInputMode())
    	{
        	Log.v("MobileWebCam", "Disable keyguard.");

        	mLock = mgr.newKeyguardLock("MobileWebCam");
	        mLock.disableKeyguard();

			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException e)
			{
			}

        	Log.v("MobileWebCam", "Keyguard unlocked!");
    	}

		super.onResume();

		if(!MobileWebCam.gIsRunning && !MobileWebCam.gInSettings)
    	{
			mPreview.setVisibility(View.VISIBLE);
    		
	    	mStartTime = System.currentTimeMillis();
	    			
			// timeout in case anything went wrong!
			mHandler.removeCallbacks(mTimeOut);
			mHandler.postDelayed(mTimeOut, 120 * 1000);
    	}
    	else
    	{
    		finish();
    	}
    }
    
    private Runnable mTimeOut = new Runnable()
	{
		@Override
		public void run()
		{
			long curtime = System.currentTimeMillis();
			MobileWebCam.LogE("TakeHiddenPicture timeout - finish after " + ((curtime - mStartTime) / 1000) + " s!");
			if(Preview.mPhotoLock.getAndSet(false))
				MobileWebCam.LogE("PhotoLock released!");
			finish();
		}
	};

    @Override
    public void onPause()
    {
    	super.onPause();

    	Log.v("MobileWebCam", "TakeHiddenPicture.onPause");
		
		closeDown();		
    	
/*    	if(mLock != null)
    	{
	    	mLock.reenableKeyguard();
	    	mLock = null;
    	} */
    }
    
    @Override
    public void onDestroy()
    {
    	Log.v("MobileWebCam", "TakeHiddenPicture.onDestroy");
		
		closeDown();
    	
    	if(mLock != null)
    	{
	    	mLock.reenableKeyguard();
	    	mLock = null;
	    	
        	Log.v("MobileWebCam", "Keyguard locked again!");	    	
    	}

    	super.onDestroy();
    }
    
    private void closeDown()
    {
		mHandler.removeCallbacks(mTimeOut);

		if(Preview.mPhotoLock.getAndSet(false))
			Log.w("MobileWebCam", "PhotoLock released because activity is going down!");
    }
    
/*	@Override
	protected void onNewIntent(Intent intent)
	 {
    	if(!MobileWebCam.gIsRunning)
    	{
			setIntent(intent); //must store the new intent unless getIntent() will return the old one
	
			Bundle extras=intent.getExtras();
	    	if(extras != null)
			{
	    		if(extras.getString("alarm") != null && extras.getString("alarm").startsWith("photo"))
				{
					if(mPreview != null)
					{
						mPreview.TakePhoto();
					}
				}
	    	}
	    }
    	else
    	{
    		finish();
    	}
	}    */
}