package de.baarflieger.mobilewebcam;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MobileWebCamHttpService extends Service implements OnSharedPreferenceChangeListener
{
	int NOTIFICATION_ID = 1;

	private MobileWebCamHttpServer mServer = null;

	public static AtomicBoolean gImageDataLock = new AtomicBoolean(false);
	public static byte[] gImageData = null;
	public static int gImageWidth = -1;
	public static int gImageHeight = -1;
	public static int gOriginalImageWidth = -1;
	public static int gOriginalImageHeight = -1;
	public static int gImageIndex = 0;

	public static void start(Context context)
	{
	    SharedPreferences prefs = context.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
	    if(prefs.getBoolean("server_enabled", true))
	    {
			Intent i = new Intent(context, MobileWebCamHttpService.class);
			i.putExtra("port", getPort(prefs));

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				context.startForegroundService(i);
				// context.startService(i);
			} else {
				context.startService(i);
			}

	    }
	}

	public static boolean stop(Context context)
	{
		Intent i = new Intent(context, MobileWebCamHttpService.class);
		return context.stopService(i);
	}

	static int getPort(SharedPreferences prefs)
	{
		String v = prefs.getString("port", "8080");
		if(v.length() == 0)
			v = "8080";
		return Integer.parseInt(v);
	}

	/* @Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

		SharedPreferences prefs = MobileWebCamHttpService.this.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
	    if(prefs.getBoolean("server_enabled", true))
	    {
			if(mServer == null)
			{
				try
				{
					if(intent != null && intent.getExtras() != null)
					{
						mServer = new MobileWebCamHttpServer(intent.getExtras().getInt("port", 8080), MobileWebCamHttpService.this);
					}
					else
					{
						mServer = new MobileWebCamHttpServer(getPort(prefs), MobileWebCamHttpService.this);
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
	    }
	}
	 */

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if(mServer != null)
			mServer.stop();
		mServer = null;
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d("Service", "Starting foreground service with ID: " + NOTIFICATION_ID);
		startForeground(NOTIFICATION_ID, createServiceNotification());
		Log.d("Service", "Foreground service started");

		SharedPreferences prefs = MobileWebCamHttpService.this.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
		if(prefs.getBoolean("server_enabled", true))
		{
			if(mServer == null)
			{
				try
				{
					if(intent != null && intent.getExtras() != null)
					{
						mServer = new MobileWebCamHttpServer(intent.getExtras().getInt("port", 8080), MobileWebCamHttpService.this);
					}
					else
					{
						mServer = new MobileWebCamHttpServer(getPort(prefs), MobileWebCamHttpService.this);
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return START_STICKY;
	}

	private Notification createServiceNotification() {

		Intent notificationIntent = new Intent(getApplicationContext(), MobileWebCam.class);
		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT); // For API level 31 and above, FLAG_IMMUTABLE or FLAG_MUTABLE should be used

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.notification_servicechannel_id))
				.setContentTitle("Service Running")
				.setContentText("Doing background work")
				.setSmallIcon(R.drawable.notification_icon)
				.setContentIntent(contentIntent)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT) // Set the priority for Android 7.1 and lower.
				.setOngoing(true); // Make this notification ongoing so it can’t be dismissed by the user.

		return builder.build();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if(key.equals("port"))
		{
			if(mServer != null)
				mServer.stop();
			try
			{
				mServer = new MobileWebCamHttpServer(getPort(sharedPreferences), MobileWebCamHttpService.this);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else if(key.equals("server_enabled"))
		{
			if(sharedPreferences.getBoolean(key, true))
			{
				Intent i = new Intent();
	        	i.setClassName("de.baarflieger.mobilewebcam", "de.baarflieger.mobilewebcam.MobileWebCamHttpService");
				startService(i);
			}
			else
			{
				if(mServer != null)
					mServer.stop();
				mServer = null;
				stopSelf();
			}
		}
	}
}
