package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.StringTokenizer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class SimpleDhtMainActivity extends Activity {
	Handler handle = new Handler();

	TextView tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dht_main);

		tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());
		findViewById(R.id.button3).setOnClickListener(
				new OnTestClickListener(tv, getContentResolver()));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
		return true;
	}

	// build Uri
	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	// LDump
	public void LDump(View v) {
		ContentResolver conRes = getContentResolver();
		Uri mUri = buildUri("content",
				"edu.buffalo.cse.cse486586.simpledht.provider");
		String[] selArgs = { "ldump" };
		Cursor resultCursor = conRes.query(mUri, null, null, selArgs, null);
		int keyIndex = resultCursor.getColumnIndex("key");
		int valueIndex = resultCursor.getColumnIndex("value");

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast()) {
			final String key = resultCursor.getString(keyIndex);
			final String value = resultCursor.getString(valueIndex);
			handle.post(new Runnable() {
				public void run() {
					tv.append("key:" + key + ",value:" + value + "\n");
				}
			});

			resultCursor.moveToNext();
		}
		resultCursor.close();
	}

	// GDump
	public void GDump(View v) {
		ContentResolver conRes = getContentResolver();
		Uri mUri = buildUri("content",
				"edu.buffalo.cse.cse486586.simpledht.provider");
		String[] selArgs = { "gdump" };
		Cursor resultCursor = conRes.query(mUri, null, null, selArgs, null);
		int keyIndex = resultCursor.getColumnIndex("key");
		int valueIndex = resultCursor.getColumnIndex("value");
		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast()) {
			final String key = resultCursor.getString(keyIndex);
			final String value = resultCursor.getString(valueIndex);
			handle.post(new Runnable() {
				public void run() {
					tv.append("key:" + key + ",value:" + value + "\n");
				}
			});

			resultCursor.moveToNext();
		}
		resultCursor.close();
	}

	@Override
	protected void onStop() {
		Uri mUri = buildUri("content",
				"edu.buffalo.cse.cse486586.simpledht.provider");
		getContentResolver().delete(mUri, null, null);
		super.onStop();
	}
}
