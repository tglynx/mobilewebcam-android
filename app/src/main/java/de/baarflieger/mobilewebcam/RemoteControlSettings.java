package de.baarflieger.mobilewebcam;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class RemoteControlSettings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	 @Override
	 public void onCreate(Bundle savedInstanceState)
	 {
        // turn off the window's title bar
//***        requestWindowFeature(Window.FEATURE_NO_TITLE);

//***        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
		 		 
        getPreferenceManager().setSharedPreferencesName(MobileWebCam.SHARED_PREFS_NAME);
		this.addPreferencesFromResource(R.layout.remotecontrol);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		final SharedPreferences prefs = getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
        getPreferenceManager().findPreference("dump_config").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	        @Override
	        public boolean onPreferenceClick(Preference preference)
	        {
	        	String settings = PhotoSettings.DumpSettings(RemoteControlSettings.this, prefs);
	        	
		       	 Intent intent = new Intent(Intent.ACTION_SEND);
		       	 intent.addCategory(Intent.CATEGORY_DEFAULT);
		       	 intent.setType("text/plain");
		       	 intent.putExtra(Intent.EXTRA_SUBJECT, "MobileWebCam Config");
		       	 intent.putExtra(Intent.EXTRA_TEXT, settings);
		         startActivity(intent);
		         return true;
	        }
	    });
        
        getPreferenceManager().findPreference("show_configphp").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	        @Override
	        public boolean onPreferenceClick(Preference preference) {
	       	 Intent intent = new Intent(Intent.ACTION_VIEW);
	       	 intent.setData(Uri.parse("http://www.dngames.de/mobilewebcam/config_php5.html"));
	            startActivity(intent);
	            return true;
	        }
	    });        
        
		setIP();
		
        getPreferenceManager().findPreference("nano").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
	        @Override
	        public boolean onPreferenceClick(Preference preference) {
	       	 Intent intent = new Intent(Intent.ACTION_VIEW);
	       	 intent.setData(Uri.parse("http://elonen.iki.fi/code/nanohttpd/"));
	            startActivity(intent);
	            return true;
	        }
	    });
	 }
	 
     public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
     {
       	 if(key.equals("server_enabled"))
       	 {
       		 if(sharedPreferences.getBoolean(key, true))
   				 MobileWebCamHttpService.start(RemoteControlSettings.this);
       		 else
   				 MobileWebCamHttpService.stop(RemoteControlSettings.this);
       	 }
     }
     
    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public static String getIpAddress(Context c, boolean local)
    {
		ConnectivityManager connManager = (ConnectivityManager)c.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if(mWifi.isConnected() || !local)
		{
			try
			{
				for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
				{
					NetworkInterface intf = en.nextElement();
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
					{
						InetAddress inetAddress = enumIpAddr.nextElement();
						String ipv4 = null;
						if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4=inetAddress.getHostAddress()))
							return ipv4;
					}
				}
			}
			catch (Exception ex)
			{
				Log.e("MobileWebCam", ex.toString());
			}
		}
        return null;
    }

    private void setIP()
    {
		final String myIP = getIpAddress(RemoteControlSettings.this, false);
		if(myIP != null)
		{
			final SharedPreferences prefs = getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0);
			getPreferenceManager().findPreference("info_ip").setTitle("http://" + myIP + ":" + MobileWebCamHttpService.getPort(prefs));
	
	        getPreferenceManager().findPreference("info_ip").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		         @Override
		         public boolean onPreferenceClick(Preference preference) {
		             final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		             emailIntent.setType("text/html");
		             emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Your MobileWebCam URL");
		             emailIntent.putExtra(Intent.EXTRA_TEXT, "http://" + myIP + ":" + MobileWebCamHttpService.getPort(prefs));
		             startActivity(Intent.createChooser(emailIntent, "Send URL via email:"));
		             return true;
		         }
	       });
		}
		else
		{
			getPreferenceManager().findPreference("info_ip").setTitle("no WIFI connection");
			getPreferenceManager().findPreference("info_ip").setEnabled(false);
		}
	}
};