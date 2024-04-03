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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.Calendar;

public class StartMobileWebCamAction extends Activity
{
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        KeyguardManager mKeyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        KeyguardLock mLock = mKeyguardManager.newKeyguardLock("MobileWebCam");
        mLock.disableKeyguard();

		try
		{
			Thread.sleep(10000);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
        
		SharedPreferences prefs = getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);

		String v = prefs.getString("camera_mode", "1");
		if(v.length() < 1 || v.length() > 9)
	        v = "1";
		switch(Integer.parseInt(v))
		{
		case 2:
		case 3:
			{
				AlarmManager alarmMgr = (AlarmManager)StartMobileWebCamAction.this.getSystemService(Context.ALARM_SERVICE);
				Intent i = new Intent(StartMobileWebCamAction.this, PhotoAlarmReceiver.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(StartMobileWebCamAction.this, 0, i, 0);
				alarmMgr.cancel(pendingIntent);
				Calendar time = Calendar.getInstance();
				time.setTimeInMillis(System.currentTimeMillis());
				time.add(Calendar.SECOND, 1);
				alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent);
			}
			break;
		case 0:
		case 1:
		default:
			Intent i = new Intent(StartMobileWebCamAction.this, MobileWebCam.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
			break;
		}

	    finish();
    }
}