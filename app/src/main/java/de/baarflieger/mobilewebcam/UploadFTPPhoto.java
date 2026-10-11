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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class UploadFTPPhoto extends Upload
{
	public static class FTPConnection
	{
		public static FTPClient client = null;
		public static Session sftpSession = null;
		public static ChannelSftp sftpChannel = null;
	}

	protected UploadFTPPhoto(Context c, ITextUpdater tu, PhotoSettings s, byte[] jpeg, Date date, String event)
	{
		super(c, tu, s, jpeg, date, event);
	}

	private static void disconnectSFTP()
	{
		if (FTPConnection.sftpChannel != null)
		{
			try { FTPConnection.sftpChannel.disconnect(); } catch (Exception ignored) {}
			FTPConnection.sftpChannel = null;
		}
		if (FTPConnection.sftpSession != null)
		{
			try { FTPConnection.sftpSession.disconnect(); } catch (Exception ignored) {}
			FTPConnection.sftpSession = null;
		}
	}

	@Override
	public void run()
	{
		doInBackgroundBegin();

		if (mSettings.mRefreshDuration >= 10 && (mSettings.mFTPBatch == 1 || ((MobileWebCam.gPictureCounter % mSettings.mFTPBatch) == 0)))
			publishProgress(mContext.getString(R.string.uploading, mSettings.mFTP + mSettings.mFTPDir));
		else if (mSettings.mFTPBatch > 1 || ((MobileWebCam.gPictureCounter % mSettings.mFTPBatch) != 0))
			publishProgress("batch ftp store");

ftpupload:	{
			try
			{
				BufferedInputStream buffIn = null;
				buffIn = new BufferedInputStream(new ByteArrayInputStream(mJpeg));

				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

				String filename = mSettings.mDefaultname;
				if (mSettings.mFTPKeepPics == 0)
				{
					if (mSettings.mFTPNumbered)
					{
						if (mSettings.mFTPTimestamp)
							filename = MobileWebCam.gPictureCounter + sdf.format(mDate) + ".jpg";
						else
							filename = MobileWebCam.gPictureCounter + ".jpg";
					}
					else if (mSettings.mFTPTimestamp)
					{
						filename = sdf.format(mDate) + ".jpg";
					}
				}

				boolean result = false;
				boolean deletetmpfile = false;
				boolean upload_now = true;
				if (mSettings.mStoreGPS)
				{
					try
					{
						byte[] buffer = new byte[1024 * 8];
						FileOutputStream fos;
						fos = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
						int r = 0;
						while ((r = buffIn.read(buffer)) > -1)
							fos.write(buffer, 0, r);
						buffIn.close();
						fos.close();
						deletetmpfile = true;
					}
					catch (Exception e)
					{
						e.printStackTrace();
						MobileWebCam.LogE("No file to write EXIF gps tag!");
					}

					File filePath = mContext.getFilesDir();
					File file = new File(filePath, filename);
					ExifWrapper.addCoordinates(file.getAbsolutePath(), WorkImage.gLatitude, WorkImage.gLongitude, WorkImage.gAltitude);

					buffIn = new BufferedInputStream(mContext.openFileInput(filename));
				}
				else if (mSettings.mFTPBatch > 1 && ((MobileWebCam.gPictureCounter % mSettings.mFTPBatch) != 0))
				{
					byte[] buffer = new byte[1024 * 8];
					FileOutputStream fos = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
					int r = 0;
					while ((r = buffIn.read(buffer)) > -1)
						fos.write(buffer, 0, r);
					buffIn.close();
					fos.close();
					upload_now = false;
				}

				if (upload_now)
				{
					if ("SFTP".equalsIgnoreCase(mSettings.mFTPProtocol))
					{
						result = performSFTPUpload(filename, buffIn, deletetmpfile);
					}
					else
					{
						result = performFTPUpload(filename, buffIn, deletetmpfile);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				if (e.getMessage() != null)
				{
					publishProgress("Upload exception:\n" + e.getMessage());
					MobileWebCam.LogE("Upload exception:\n" + e.getMessage());
				}
				else
				{
					publishProgress("Upload exception");
					MobileWebCam.LogE("Upload exception");
				}
				FTPConnection.client = null;
				disconnectSFTP();
			}
		}

		doInBackgroundEnd(mSettings.mFTPBatch == 1 || ((MobileWebCam.gPictureCounter % mSettings.mFTPBatch) == 0));
	}

	private boolean performSFTPUpload(String filename, BufferedInputStream buffIn, boolean deletetmpfile) throws Exception
	{
		boolean result = false;
		if (FTPConnection.sftpChannel == null || !FTPConnection.sftpChannel.isConnected())
		{
			JSch jsch = new JSch();
			if (mSettings.mSFTPKeyPath != null && !mSettings.mSFTPKeyPath.trim().isEmpty())
			{
				File keyFile = new File(mSettings.mSFTPKeyPath);
				if (keyFile.exists())
				{
					jsch.addIdentity(keyFile.getAbsolutePath());
				}
			}

			FTPConnection.sftpSession = jsch.getSession(mSettings.mFTPLogin, mSettings.mFTP, mSettings.mFTPPort);
			if (mSettings.mFTPPassword != null && !mSettings.mFTPPassword.isEmpty())
			{
				FTPConnection.sftpSession.setPassword(mSettings.mFTPPassword);
			}

			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			FTPConnection.sftpSession.setConfig(config);
			FTPConnection.sftpSession.connect(15000);

			FTPConnection.sftpChannel = (ChannelSftp) FTPConnection.sftpSession.openChannel("sftp");
			FTPConnection.sftpChannel.connect(15000);
		}

		if (mSettings.mFTPDir != null && !mSettings.mFTPDir.trim().isEmpty())
		{
			try
			{
				FTPConnection.sftpChannel.cd(mSettings.mFTPDir);
			}
			catch (SftpException e)
			{
				publishProgress("wrong sftp cwd response: " + e.getMessage() + "\nIs the directory correct?");
				MobileWebCam.LogE("wrong sftp cwd response: " + e.getMessage() + "\nIs the directory correct?");
				disconnectSFTP();
				return false;
			}
		}

		if (mSettings.mFTPKeepPics > 0)
		{
			String replacename = filename.replace(".jpg", "");
			for (int i = mSettings.mFTPKeepPics - 1; i > 0; i--)
			{
				try
				{
					FTPConnection.sftpChannel.rename(replacename + i + ".jpg", replacename + (i + 1) + ".jpg");
				}
				catch (Exception ignored) {}
			}
			try
			{
				FTPConnection.sftpChannel.rename(mSettings.mDefaultname, replacename + "1.jpg");
			}
			catch (Exception e)
			{
				if (e.getMessage() != null)
					MobileWebCam.LogE(e.getMessage());
			}
		}

		do
		{
			FTPConnection.sftpChannel.put(buffIn, filename, ChannelSftp.OVERWRITE);
			buffIn.close();
			buffIn = null;

			if (deletetmpfile)
				mContext.deleteFile(filename);

			if (mSettings.mFTPBatch > 1 || mSettings.mReliableUpload)
			{
				File file = mContext.getFilesDir();
				String[] pics = file.list((dir, name) -> name.endsWith(".jpg"));
				if (pics != null && pics.length > 0)
				{
					filename = pics[0];
					buffIn = new BufferedInputStream(mContext.openFileInput(filename));
					deletetmpfile = true;
					MobileWebCam.LogI("sftp batched upload: " + filename);
				}
			}
		}
		while (buffIn != null);

		if (mSettings.mLogUpload)
		{
			String log = MobileWebCam.GetLog(mContext, mContext.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0), mSettings);
			InputStream logIS = new ByteArrayInputStream(log.getBytes("UTF-8"));
			FTPConnection.sftpChannel.put(logIS, "log.txt", ChannelSftp.OVERWRITE);
		}

		result = true;
		publishProgress("ok");
		MobileWebCam.LogI("ok");

		if (!mSettings.mFTPKeepConnected)
		{
			disconnectSFTP();
		}

		return result;
	}

	private boolean performFTPUpload(String filename, BufferedInputStream buffIn, boolean deletetmpfile) throws Exception
	{
		boolean result = false;
		FTPConnection.client = new FTPClient();
		FTPConnection.client.connect(InetAddress.getByName(mSettings.mFTP), mSettings.mFTPPort);

		FTPConnection.client.login(mSettings.mFTPLogin, mSettings.mFTPPassword);
		if (!FTPConnection.client.getReplyString().contains("230"))
		{
			publishProgress("wrong ftp login response: " + FTPConnection.client.getReplyString() + "\nAre your credentials correct?");
			MobileWebCam.LogE("wrong ftp login response: " + FTPConnection.client.getReplyString() + "\nAre your credentials correct?");
			FTPConnection.client = null;
			return false;
		}

		FTPConnection.client.changeWorkingDirectory(mSettings.mFTPDir);
		if (!FTPConnection.client.getReplyString().contains("250"))
		{
			publishProgress("wrong ftp cwd response: " + FTPConnection.client.getReplyString() + "\nIs the directory correct?");
			MobileWebCam.LogE("wrong ftp cwd response: " + FTPConnection.client.getReplyString() + "\nIs the directory correct?");
			FTPConnection.client = null;
			return false;
		}

		FTPConnection.client.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);

		if (mSettings.mFTPPassive)
			FTPConnection.client.enterLocalPassiveMode();
		else
			FTPConnection.client.enterLocalActiveMode();

		if (mSettings.mFTPKeepPics > 0)
		{
			String replacename = filename.replace(".jpg", "");
			for (int i = mSettings.mFTPKeepPics - 1; i > 0; i--)
			{
				try
				{
					FTPConnection.client.rename(replacename + i + ".jpg", replacename + (i + 1) + ".jpg");
				}
				catch (IOException ignored) {}
			}
			try
			{
				FTPConnection.client.rename(mSettings.mDefaultname, replacename + "1.jpg");
			}
			catch (Exception e)
			{
				if (e.getMessage() != null)
					MobileWebCam.LogE(e.getMessage());
			}
		}

		do
		{
			result = FTPConnection.client.storeFile(filename, buffIn);
			buffIn.close();
			buffIn = null;

			if (deletetmpfile)
				mContext.deleteFile(filename);

			if (mSettings.mFTPBatch > 1 || mSettings.mReliableUpload)
			{
				File file = mContext.getFilesDir();
				String[] pics = file.list((dir, name) -> name.endsWith(".jpg"));
				if (pics != null && pics.length > 0)
				{
					filename = pics[0];
					buffIn = new BufferedInputStream(mContext.openFileInput(filename));
					deletetmpfile = true;
					MobileWebCam.LogI("ftp batched upload: " + filename);
				}
			}
		}
		while (buffIn != null);

		InputStream logIS = null;
		if (mSettings.mLogUpload)
		{
			String log = MobileWebCam.GetLog(mContext, mContext.getSharedPreferences(MobileWebCam.SHARED_PREFS_NAME, 0), mSettings);
			logIS = new ByteArrayInputStream(log.getBytes("UTF-8"));
			if (logIS != null)
			{
				result &= FTPConnection.client.storeFile("log.txt", logIS);
			}
		}

		if (result)
		{
			publishProgress("ok");
			MobileWebCam.LogI("ok");
		}
		else
		{
			publishProgress("ftp error: " + FTPConnection.client.getReplyString());
			MobileWebCam.LogE("ftp error: " + FTPConnection.client.getReplyString());
		}

		if (!mSettings.mFTPKeepConnected)
		{
			FTPConnection.client.logout();
			FTPConnection.client.disconnect();
			FTPConnection.client = null;
		}

		return result;
	}
}
