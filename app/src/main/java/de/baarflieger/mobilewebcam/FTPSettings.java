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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import java.io.IOException;

public class FTPSettings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(MobileWebCam.SHARED_PREFS_NAME);
		this.addPreferencesFromResource(R.layout.ftpserversettings);
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		updateUIForProtocol();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if ("ftp_protocol".equals(key))
		{
			String protocol = sharedPreferences.getString("ftp_protocol", "FTP");
			String currentPort = sharedPreferences.getString("ftp_port", "21");
			if ("SFTP".equals(protocol) && "21".equals(currentPort))
			{
				sharedPreferences.edit().putString("ftp_port", "22").apply();
			}
			else if ("FTP".equals(protocol) && "22".equals(currentPort))
			{
				sharedPreferences.edit().putString("ftp_port", "21").apply();
			}
			updateUIForProtocol();
		}

		if ("ftp_keep_open".equals(key) || "ftp_protocol".equals(key) || "ftpserver_url".equals(key))
		{
			if (UploadFTPPhoto.FTPConnection.client != null)
			{
				try
				{
					UploadFTPPhoto.FTPConnection.client.abort();
					UploadFTPPhoto.FTPConnection.client.disconnect();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				UploadFTPPhoto.FTPConnection.client = null;
			}
			UploadFTPPhoto.FTPConnection.sftpSession = null;
			UploadFTPPhoto.FTPConnection.sftpChannel = null;
		}
	}

	private void updateUIForProtocol()
	{
		SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		String protocol = prefs.getString("ftp_protocol", "FTP");
		Preference passivePref = findPreference("cam_passiveftp");
		if (passivePref != null)
		{
			passivePref.setEnabled("FTP".equals(protocol));
		}
	}

	@Override
	protected void onDestroy()
	{
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		MobileWebCam.gInSettings = true;
		updateUIForProtocol();
	}

	public void onPause()
	{
		super.onPause();

		MobileWebCam.gInSettings = false;
	}
}
