package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;

import edu.buffalo.cse.cse486586.simpledht.SimpleDhtMainActivity.forClient;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class SimpleDhtProvider extends ContentProvider {
	Context pcontext;
	private static final int KEY = 1;
	private static final int VALUE = 2;
	private static final String KEY_S = "key";
	private static final String VALUE_S = "value";

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	public void ins(String key, String value) {
		String port = SimpleDhtMainActivity.selfPort;
		Log.i("Provider ins "+port,"key:"+key+",value:"+value);
		key = key.concat(".txt");
		try {
			FileOutputStream fos = pcontext.openFileOutput(key,
					Context.MODE_PRIVATE);
			OutputStreamWriter osw = new OutputStreamWriter(fos);
			osw.write(value);
			osw.flush();
			osw.close();
		} catch (Exception e) {
			Log.i("chatStorage, fileCreate()", "Exception e = " + e);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String key = (String) values.get(KEY_S);
		String value = (String) values.get(VALUE_S);
		String selfPort = SimpleDhtMainActivity.selfPort;
		String succ = SimpleDhtMainActivity.succ;
		String pred = SimpleDhtMainActivity.pred;
		Boolean largest = SimpleDhtMainActivity.largest;
		Boolean smallest = SimpleDhtMainActivity.smallest;
		//Log.i("Provider insert","key:"+key+",value:"+value);
		try {
			if (smallest == true) {
				if (genHash(key).compareTo(genHash(selfPort)) <= 0
						|| genHash(key).compareTo(genHash(pred)) > 0) {
					ins(key, value);
				} else if (genHash(key).compareTo(genHash(selfPort)) > 0) {
					new Thread(new forClient(key + ";" + value,
							Integer.parseInt(succ) * 2, 1)).start();
				}
			} else {
				if (genHash(key).compareTo(genHash(selfPort)) <= 0) {
					if (genHash(key).compareTo(genHash(pred)) > 0) {
						ins(key, value);
					} else
						new Thread(new forClient(key + ";" + value,
								Integer.parseInt(pred) * 2, 1)).start();
				} else if (genHash(key).compareTo(genHash(selfPort)) > 0) {
					new Thread(new forClient(key + ";" + value,
							Integer.parseInt(succ) * 2, 1)).start();
				}
			}
		} catch (NoSuchAlgorithmException e) {

		}
		getContext().getContentResolver().notifyChange(uri, null);
		return uri;
	}

	@Override
	public boolean onCreate() {
		pcontext = getContext();
		return false;
	}

	Cursor quer(String selection) {
		String[] cols = { KEY_S, VALUE_S };
		String fname = selection.concat(".txt");
		String value;
		Log.i("Provider quer","key:"+selection);
		try {
			FileInputStream fin = pcontext.openFileInput(fname);
			InputStreamReader inpReader = new InputStreamReader(fin);
			BufferedReader br = new BufferedReader(inpReader);
			// Fill the buffer with data from file
			value = br.readLine();
		} catch (Exception e) {
			Log.i("chatStorage, readFile()", "Exception e = " + e);
			value = null;
		}
		Log.i("Provider quer","value:"+value);
		MatrixCursor cur = new MatrixCursor(cols);
		String[] row = { selection, value };
		cur.addRow(row);
		return cur;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String selfPort = SimpleDhtMainActivity.selfPort;
		String succ = SimpleDhtMainActivity.succ;
		String pred = SimpleDhtMainActivity.pred;
		Boolean largest = SimpleDhtMainActivity.largest;
		Boolean smallest = SimpleDhtMainActivity.smallest;
		String[] cols = { KEY_S, VALUE_S };
		Cursor cur = null;
		SimpleDhtMainActivity.queryValue = null;
		try {
			if (smallest == true) {
				if (genHash(selection).compareTo(genHash(selfPort)) <= 0
						|| genHash(selection).compareTo(genHash(pred)) > 0) {
					cur = quer(selection);
				} else if (genHash(selection).compareTo(genHash(selfPort)) > 0) {
					new Thread(new forClient(selection + ";" + selfPort,
							Integer.parseInt(succ) * 2, 2)).start();
					while(SimpleDhtMainActivity.queryValue == null){}
					MatrixCursor mcur = new MatrixCursor(cols);
					String[] row = { selection,
							SimpleDhtMainActivity.queryValue };
					mcur.addRow(row);
					cur = mcur;
				}
			} else {
				if (genHash(selection).compareTo(genHash(selfPort)) <= 0) {
					if (genHash(selection).compareTo(genHash(pred)) > 0) {
						cur = quer(selection);
					} else {
						new Thread(new forClient(selection + ";" + selfPort,
								Integer.parseInt(pred) * 2, 2)).start();
						while(SimpleDhtMainActivity.queryValue == null){}
						MatrixCursor mcur = new MatrixCursor(cols);
						String[] row = { selection, SimpleDhtMainActivity.queryValue };
						mcur.addRow(row);
						cur = mcur;
					}
				} else if (genHash(selection).compareTo(genHash(selfPort)) > 0) {
					new Thread(new forClient(selection + ";" + selfPort,
							Integer.parseInt(succ) * 2, 2)).start();
					while(SimpleDhtMainActivity.queryValue == null){}
					MatrixCursor mcur = new MatrixCursor(cols);
					String[] row = { selection, SimpleDhtMainActivity.queryValue };
					mcur.addRow(row);
					cur = mcur;
				}
			}
		} catch (NoSuchAlgorithmException e) {

		}
		return cur;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	// to identify AVD executing the app
	public String getAVD() {
		TelephonyManager tel = (TelephonyManager) getContext()
				.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(
				tel.getLine1Number().length() - 4);
		return portStr;
	}

	// client thread to multicast messages
	class forClient extends Thread {
		String msg, TAG = "forClient";
		int type, port;

		forClient(String msg, int port, int msgType) {
			this.msg = msg;
			this.port = port;
			type = msgType;
		}

		public void run() {
			Socket clSock;
			try {
				//Log.i("Client Thread Provider", msg);
				// connect to server
				clSock = new Socket("10.0.2.2", port);
				//Log.i("Sending Message type" + type + "=", msg);
				// send the message to server
				PrintWriter sendData = new PrintWriter(clSock.getOutputStream());
				if (type == 1) // insert
					sendData.println("%" + msg);
				if (type == 2) // query
					sendData.println("$" + msg);
				sendData.flush();
				sendData.close();
				clSock.close();
				//Log.i("Message sent=", msg);

			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, "Number format Exception!\n");
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, "Unknown Host Exception!\n");
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, "I/O error occured when creating the socket!\n");
				e.printStackTrace();
			} catch (SecurityException e) {
				Log.i(TAG, "Security Exception!\n");
				e.printStackTrace();
			}

		}
	}// end of client thread

}
