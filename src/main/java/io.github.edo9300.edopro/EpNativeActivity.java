package io.github.edo9300.edopro;

import android.app.AlertDialog;
import android.app.NativeActivity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.LinkedList;

import libwindbot.windbot.WindBot;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class EpNativeActivity extends NativeActivity {

	public static native void putComboBoxResult(int index);

	public static native void putMessageBoxResult(String text, boolean isenter);

	public static native void errorDialogReturn();

	private boolean use_windbot;

	static {
		//on 4.2 libraries aren't properly loaded automatically
		//https://stackoverflow.com/questions/28806373/android-4-2-ndk-library-loading-crash-load-librarylinker-cpp750-soinfo-l/28817942
		/*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
			System.loadLibrary("hidapi");
			System.loadLibrary("SDL2");
			System.loadLibrary("mpg123");
			System.loadLibrary("SDL2_mixer");
		}*/
		System.loadLibrary("EDOProClient");
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		var ex = getIntent().getExtras();
		assert ex != null;
		use_windbot = ex.getBoolean("USE_WINDBOT", true);
		var filter = new IntentFilter();
		filter.addAction("RUN_WINDBOT");
		filter.addAction("ATTACH_WINDBOT_DATABASE");
		filter.addAction("INPUT_TEXT");
		filter.addAction("MAKE_CHOICE");
		filter.addAction("INSTALL_UPDATE");
		filter.addAction("OPEN_SCRIPT");
		filter.addAction("SHOW_ERROR_WINDOW");
		filter.addAction("SHARE_FILE");
		LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, filter);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onResume() {
		super.onResume();
		makeFullScreen();
	}

	@SuppressWarnings("ObsoleteSdkInt")
	private void makeFullScreen() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			this.getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus)
			makeFullScreen();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if ((event.getSource() & InputDevice.SOURCE_MOUSE) != InputDevice.SOURCE_MOUSE)
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			var action = intent.getAction();
			if ("RUN_WINDBOT".equals(action)) {
				var args = intent.getStringExtra("args");
				Log.i("EDOProWindBotIgnite", "Launching WindBot Ignite with " + args + " as parameters.");
				WindBot.runAndroid(args);
			} else if ("ATTACH_WINDBOT_DATABASE".equals(action)) {
				var args = intent.getStringExtra("args");
				Log.i("EDOProWindBotIgnite", "Loading database: " + args + ".");
				WindBot.addDatabase(args);
			} else if ("INPUT_TEXT".equals(action)) {
				new TextEntry().Show(EpNativeActivity.this, intent.getStringExtra("current"));
			} else if ("MAKE_CHOICE".equals(action)) {
				var parameters = intent.getStringArrayExtra("args");
				new AlertDialog.Builder(EpNativeActivity.this)
						//.setCancelable(false)
						.setItems(parameters, (dialog, id) -> putComboBoxResult(id))
						.show();
			} else if ("INSTALL_UPDATE".equals(action)) {
				var path = intent.getStringExtra("args");
				assert path != null;
				Log.i("EDOProUpdater", "Installing update from: \"" + path + "\".");
				var updateInstallIntent = new Intent(Intent.ACTION_VIEW);
				updateInstallIntent.setDataAndType(FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", new File(path)), "application/vnd.android.package-archive");
				updateInstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				updateInstallIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				startActivity(updateInstallIntent);
			} else if ("OPEN_SCRIPT".equals(action)) {
				var path = intent.getStringExtra("args");
				assert path != null;
				Log.i("EDOPro", "opening script from: " + path);
				var fileIntent = new Intent(Intent.ACTION_VIEW);
				Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", new File(path));
				fileIntent.setDataAndType(uri, "text/*");
				fileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				fileIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
				startActivity(fileIntent);
			} else if ("SHOW_ERROR_WINDOW".equals(action)) {
				var message_context = intent.getStringExtra("context");
				var message = intent.getStringExtra("message");
				Log.i("EDOPro", "Received show error dialog " + message_context + " " + message);
				new AlertDialog.Builder(EpNativeActivity.this)
						.setTitle(message_context)
						.setMessage(message)
						.setPositiveButton("OK", (dialog, id) -> errorDialogReturn())
						.setCancelable(false)
						.show();
			} else if ("SHARE_FILE".equals(action)) {
				var path = intent.getStringExtra("args");
				assert path != null;
				Log.i("EDOPro", "sharing file from: " + path);
				var uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", new File(path));
				var fileIntent = new Intent(Intent.ACTION_SEND);
				fileIntent.setType("text/plain");
				fileIntent.putExtra(Intent.EXTRA_STREAM, uri);
				fileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(fileIntent);
			}
		}
	};

	@SuppressWarnings({"unused", "deprecation"})
	public void launchWindbot(String parameters) {
		if (!use_windbot)
			return;
		var intent = new Intent();
		intent.putExtra("args", parameters);
		intent.setAction("RUN_WINDBOT");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressWarnings({"unused", "deprecation"})
	public void addWindbotDatabase(String database) {
		if (!use_windbot)
			return;
		var intent = new Intent();
		intent.putExtra("args", database);
		intent.setAction("ATTACH_WINDBOT_DATABASE");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressWarnings({"unused", "deprecation"})
	public void showDialog(String current) {
		var intent = new Intent();
		intent.putExtra("current", current);
		intent.setAction("INPUT_TEXT");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressWarnings({"unused", "deprecation"})
	public void showComboBox(String[] parameters) {
		Intent intent = new Intent();
		intent.putExtra("args", parameters);
		intent.setAction("MAKE_CHOICE");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressWarnings({"unused", "deprecation"})
	public void installUpdate(String path) {
		try {
			var file = new File(getFilesDir(), "should_copy_update");
			if (!file.createNewFile()) {
				Log.e("EDOPro", "error when creating should_copy_update file:");
			}
		} catch (Exception e) {
			Log.e("EDOPro", "error when creating should_copy_update file: " + e.getMessage());
		}
		var intent = new Intent();
		intent.putExtra("args", path);
		intent.setAction("INSTALL_UPDATE");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressWarnings("unused")
	public void openUrl(String url) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(browserIntent);
	}

	@SuppressWarnings({"unused", "deprecation"})
	public void openFile(String path) {
		var intent = new Intent();
		intent.putExtra("args", path);
		intent.setAction("OPEN_SCRIPT");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressWarnings({"unused", "deprecation"})
	public void showErrorDialog(String context, String message) {
		var intent = new Intent();
		intent.putExtra("context", context);
		intent.putExtra("message", message);
		intent.setAction("SHOW_ERROR_WINDOW");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressWarnings({"unused", "deprecation"})
	public void shareFile(String path) {
		var intent = new Intent();
		intent.putExtra("args", path);
		intent.setAction("SHARE_FILE");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@SuppressWarnings("unused")
	public float getDensity() {
		return getResources().getDisplayMetrics().density;
	}

	@SuppressWarnings("unused")
	public int getDisplayWidth() {
		return getResources().getDisplayMetrics().widthPixels;
	}

	@SuppressWarnings("unused")
	public int getDisplayHeight() {
		return getResources().getDisplayMetrics().heightPixels;
	}

	@SuppressWarnings("unused")
	public byte[][] getLocalIpAddresses() {
		var ret = new LinkedList<byte[]>();
		try {
			for (var intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (!intf.isUp() || intf.isLoopback())
					continue;
				for (var addr : Collections.list(intf.getInetAddresses())) {
					if (!(addr instanceof Inet4Address) && !(addr instanceof Inet6Address))
						continue;
					ret.add(addr.getAddress());
				}
			}
		} catch (Exception ignored) {
		}
		return ret.toArray(new byte[ret.size()][]);
	}

	@SuppressWarnings("unused")
	public void setClipboard(final String text) {
		EpNativeActivity.this.runOnUiThread(() -> {
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
					.setPrimaryClip(ClipData.newPlainText("", text));
		});
	}

	class RunnableObject implements Runnable {
		public String result = "";

		public void run() {
			var clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			if (!(clipboard.hasPrimaryClip()) || clipboard.getPrimaryClip() == null || (clipboard.getPrimaryClipDescription() == null)
					|| !(clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {
				result = "";
			} else {
				var clip = clipboard.getPrimaryClip();
				result = clip.getItemAt(0).getText().toString();
			}
			synchronized (this) {
				this.notify();
			}
		}
	}

	@SuppressWarnings("unused")
	public String getClipboard() {
		var myRunnable = new RunnableObject();
		EpNativeActivity.this.runOnUiThread(myRunnable);
		try {
			myRunnable.wait(); // unlocks myRunable while waiting
		} catch (InterruptedException e) {
			return "";
		}
		return myRunnable.result;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
	}
}
