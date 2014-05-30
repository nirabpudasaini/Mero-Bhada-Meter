package com.nirab.merobhadameter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class DownloadOfflineData extends AsyncTask<String, Integer, String> {

	private Activity activity;
	private ProgressDialog mProgressDialog;
	private AsyncTaskCompleteListener callback;
	private Context context;

	public DownloadOfflineData(Context c) {
		this.context = c;
		Activity a = (Activity) c;
		this.activity = a;
		this.callback = (AsyncTaskCompleteListener) a;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		
		mProgressDialog = new ProgressDialog(activity);
		mProgressDialog.setMessage("Downloading, Please have Patience");
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(false);
		
		
		mProgressDialog.show();
	}

	@Override
	protected String doInBackground(String... sUrl) {
		// take CPU lock to prevent CPU from going off if the user
		// presses the power button during download
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
		wl.acquire();

		try {
			InputStream input = null;
			OutputStream output = null;
			HttpURLConnection connection = null;
			try {
				URL url = new URL(sUrl[0]);
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();

				// expect HTTP 200 OK, so we don't mistakenly save error
				// report
				// instead of the file
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					return "Server returned HTTP "
							+ connection.getResponseCode() + " "
							+ connection.getResponseMessage();

				File filecheck = new File(Environment
						.getExternalStorageDirectory().getPath()
						+ "/merobhadameter/maps/kathmandu-gh/kathmandu.map");

				if (filecheck.exists()) {
					Log.i("File Exists", "Code Gets here, file exists");
					return "exists";
					// if (connection.getResponseCode() ==
					// HttpURLConnection.HTTP_NOT_MODIFIED) {
					//
					// return null;
					// }
				}

				// this will be useful to display download percentage
				// might be -1: server did not report the length
				int fileLength = connection.getContentLength();
				Log.i("Length", String.valueOf(fileLength));

				// download the file
				input = connection.getInputStream();
				output = new FileOutputStream(Environment
						.getExternalStorageDirectory().getPath()
						+ "/merobhadameter/maps" + "/kathmandu-gh.zip");

				byte data[] = new byte[4096];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					// allow canceling with back button
					if (isCancelled())
						return null;
					total += count;
					// publishing the progress....
					if (fileLength > 0) // only if total length is known
						publishProgress((int) (total * 100 / fileLength));
					output.write(data, 0, count);
				}
			} catch (Exception e) {
				return e.toString();
			} finally {
				try {
					if (output != null)
						output.close();
					if (input != null)
						input.close();
				} catch (IOException ignored) {
				}

				if (connection != null)
					connection.disconnect();
			}
		} finally {
			wl.release();
		}
		return null;
	}
	

	@Override
	protected void onProgressUpdate(Integer... progress) {
		super.onProgressUpdate(progress);
		// if we get here, length is known, now set indeterminate to false
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setMax(100);
		mProgressDialog.setProgress(progress[0]);
	}
	
	@Override
	protected void onPostExecute(String result) {
		mProgressDialog.dismiss();
		if (result != null) {
			if (result == "exists") {
				callback.onTaskComplete("File Already Exists and is up to date");
			} else {
				callback.onTaskComplete("Download error: " + result);
			}
		}

		else {
			
			callback.onTaskComplete("File downloaded");
			
			Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT)
					.show();
			unpackZip(Environment.getExternalStorageDirectory().getPath()
					+ "/merobhadameter/maps/", "kathmandu-gh.zip");
		}
	}
	
	
	// To unzip files
	private boolean unpackZip(String path, String zipname) {
		InputStream is;
		ZipInputStream zis;
		try {
			String filename;
			is = new FileInputStream(path + zipname);
			zis = new ZipInputStream(new BufferedInputStream(is));
			ZipEntry ze;
			byte[] buffer = new byte[1024];
			int count;

			while ((ze = zis.getNextEntry()) != null) {

				filename = ze.getName();

				// Need to create directories if not exists, or
				// it will generate an Exception...
				if (ze.isDirectory()) {
					File fmd = new File(path + filename);
					fmd.mkdirs();
					continue;
				}

				FileOutputStream fout = new FileOutputStream(path + filename);

				while ((count = zis.read(buffer)) != -1) {
					fout.write(buffer, 0, count);
				}

				fout.close();
				zis.closeEntry();
			}

			zis.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

}








	






