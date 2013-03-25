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
import java.util.StringTokenizer;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
	Context pcontext;
	private static final String KEY_S = "key";
	private static final String VALUE_S = "value";
	static String selfPort;
	static String succ;
	static String pred;
	static Boolean largest;
	static Boolean smallest;
	static String queryValue;
	ContentResolver conRes;
	static String[] cols = { KEY_S, VALUE_S };
	static MatrixCursor gcur = new MatrixCursor(cols);
	static int gcount = 0;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String[] savFiles = pcontext.fileList();
		for (int i = 0; i < savFiles.length; i++) {
			pcontext.deleteFile(savFiles[i]);
		}
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	public void ins(String key, String value) {
		Log.i("Provider ins " + selfPort, "key:" + key + ",value:" + value);
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
		// Log.i("Provider insert","key:"+key+",value:"+value);
		try {
			if (smallest == true) {
				if (genHash(key).compareTo(genHash(selfPort)) <= 0
						|| genHash(key).compareTo(genHash(pred)) > 0) {
					ins(key, value);
				} else if (genHash(key).compareTo(genHash(selfPort)) > 0) {
					new Thread(new forClient(key + ";" + value,
							Integer.parseInt(succ) * 2, 6)).start();
				}
			} else {
				if (genHash(key).compareTo(genHash(selfPort)) <= 0) {
					if (genHash(key).compareTo(genHash(pred)) > 0) {
						ins(key, value);
					} else
						new Thread(new forClient(key + ";" + value,
								Integer.parseInt(pred) * 2, 6)).start();
				} else if (genHash(key).compareTo(genHash(selfPort)) > 0) {
					new Thread(new forClient(key + ";" + value,
							Integer.parseInt(succ) * 2, 6)).start();
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
		selfPort = getAVD();
		conRes = pcontext.getContentResolver();
		// initiate the server thread
		Thread serv = new forServer();
		serv.start();
		return false;
	}

	String[] quer(String selection) {
		String fname = selection;
		String value;
		Log.i("Provider quer", "key:" + selection);
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
		Log.i("Provider quer", "value:" + value);
		String[] row = { selection, value };
		return row;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		MatrixCursor mcur = new MatrixCursor(cols);
		queryValue = null;
		if (selectionArgs != null && selectionArgs.length > 0) {
			if (selectionArgs[0].equals("ldump")) {
				String[] savFiles = pcontext.fileList();
				for (int i = 0; i < savFiles.length; i++) {
					mcur.addRow(quer(savFiles[i]));
				}
			}
			if (selectionArgs[0].equals("gdump")) {
				String[] savFiles = pcontext.fileList();
				for (int i = 0; i < savFiles.length; i++) {
					gcur.addRow(quer(savFiles[i]));
				}
				if (pred.equals(succ)) {
					new Thread(new forClient(selfPort,
							Integer.parseInt(pred) * 2, 8)).start();
					while (gcount != 1) {
					}
				} else {
					new Thread(new forClient(selfPort,
							Integer.parseInt(pred) * 2, 8)).start();
					new Thread(new forClient(selfPort,
							Integer.parseInt(succ) * 2, 8)).start();
					while (gcount != 2) {
					}
				}
				return gcur;
			}
		} else {
			try {
				if (smallest == true) {
					if (genHash(selection).compareTo(genHash(selfPort)) <= 0
							|| genHash(selection).compareTo(genHash(pred)) > 0) {
						String[] row = quer(selection);
						mcur.addRow(row);
					} else if (genHash(selection).compareTo(genHash(selfPort)) > 0) {
						new Thread(new forClient(selection + ";" + selfPort,
								Integer.parseInt(succ) * 2, 7)).start();
						while (queryValue == null) {
						}
						String[] row = { selection, queryValue };
						mcur.addRow(row);
					}
				} else {
					if (genHash(selection).compareTo(genHash(selfPort)) <= 0) {
						if (genHash(selection).compareTo(genHash(pred)) > 0) {
							String[] row = quer(selection);
							mcur.addRow(row);
						} else {
							new Thread(new forClient(
									selection + ";" + selfPort,
									Integer.parseInt(pred) * 2, 7)).start();
							while (queryValue == null) {
							}
							String[] row = { selection, queryValue };
							mcur.addRow(row);
						}
					} else if (genHash(selection).compareTo(genHash(selfPort)) > 0) {
						new Thread(new forClient(selection + ";" + selfPort,
								Integer.parseInt(succ) * 2, 7)).start();
						while (queryValue == null) {
						}
						String[] row = { selection, queryValue };
						mcur.addRow(row);
					}
				}
			} catch (NoSuchAlgorithmException e) {

			}
		}
		return mcur;
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
				// Log.i("Client Thread Provider", msg);
				// connect to server
				clSock = new Socket("10.0.2.2", port);
				// Log.i("Sending Message type" + type + "=", msg);
				// send the message to server
				PrintWriter sendData = new PrintWriter(clSock.getOutputStream());
				if (type == 1) // join node request
					sendData.println("#" + msg);
				if (type == 2) // update node request
					sendData.println("&" + msg);
				if (type == 3) // update only successor
					sendData.println("!" + msg);
				if (type == 4) // update only predecessor
					sendData.println("^" + msg);
				if (type == 5) // query response
					sendData.println("@" + msg);
				if (type == 6) // insert
					sendData.println("%" + msg);
				if (type == 7) // query
					sendData.println("$" + msg);
				if (type == 8) // gdump
					sendData.println("*" + msg);
				if (type == 9) // gdump reply
					sendData.println("(" + msg);
				sendData.flush();
				sendData.close();
				clSock.close();
				// Log.i("Message sent=", msg);

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

	// server thread
	class forServer extends Thread {

		String TAG = "forServer";

		public void joinReq() {
			if (selfPort.equals("5554")) {
				succ = "5554";
				pred = "5554";
				largest = true;
				smallest = true;
			} else
				new Thread(new forClient(selfPort, 11108, 1)).start();
		}

		public void run() {
			try {
				joinReq();
				// open connection on port 10000
				ServerSocket serSock = new ServerSocket(10000);
				// Log.d("Starting Server", "Forserver");
				while (true) {
					// listen for client
					Socket recvSock = serSock.accept();
					// Log.i("Connection", "Accepted");
					// get the message
					InputStreamReader readStream = new InputStreamReader(
							recvSock.getInputStream());
					BufferedReader recvInp = new BufferedReader(readStream);
					// Log.i("Reader", "Initialized");
					String recvMsg = recvInp.readLine();
					// Log.i("Received Message Main:", recvMsg);
					// recognise message type
					switch (recvMsg.charAt(0)) {
					case '%': // insert
						insertKey(recvMsg.substring(1));
						break;
					case '$': // query
						Thread th = new queryKey(recvMsg.substring(1));
						th.start();
						break;
					case '#': // join request
						joinNode(recvMsg.substring(1));
						break;
					case '&': // response of join
						updateNode(recvMsg.substring(1));
						break;
					case '!': // set successor
						setSucc(recvMsg.substring(1));
						break;
					case '^': // set predecessor
						setPred(recvMsg.substring(1));
						break;
					case '@':
						queryValue = recvMsg.substring(1);
						break;
					case '*':
						getldump(recvMsg.substring(1));
						break;
					case '(':
						Thread th2 = new storeldump(recvMsg.substring(1));
						th2.start();
						break;
					}
					recvSock.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, "I/O error occured when creating the socket!\n");
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		void joinNode(String newNode) throws NoSuchAlgorithmException {
			String currNode = selfPort;
			int small = 0, large = 0;
			if (genHash(newNode).compareTo(currNode) < 0) {
				if (genHash(newNode).compareTo(pred) > 0 || smallest == true) {
					if (smallest == true) {
						smallest = false;
						small = 1;
					}
					new Thread(new forClient(pred + ";" + currNode + ";"
							+ small + ";" + large,
							Integer.parseInt(newNode) * 2, 2)).start();
					new Thread(new forClient(newNode,
							Integer.parseInt(pred) * 2, 3)).start();
					pred = newNode;
				} else if (genHash(newNode).compareTo(pred) < 0) {
					new Thread(new forClient(newNode,
							Integer.parseInt(pred) * 2, 1)).start();
				}
			} else if (genHash(newNode).compareTo(currNode) > 0) {
				if (genHash(newNode).compareTo(succ) < 0 || largest == true) {
					if (largest == true) {
						largest = false;
						large = 1;
					}
					new Thread(new forClient(currNode + ";" + succ + ";"
							+ small + ";" + large,
							Integer.parseInt(newNode) * 2, 2)).start();
					new Thread(new forClient(newNode,
							Integer.parseInt(succ) * 2, 4)).start();
					succ = newNode;
				} else if (genHash(newNode).compareTo(pred) < 0) {
					new Thread(new forClient(newNode,
							Integer.parseInt(succ) * 2, 1)).start();
				}
			}
		}

		// update node
		void updateNode(String msg) {
			StringTokenizer sTok = new StringTokenizer(msg, ";");
			pred = sTok.nextToken();
			succ = sTok.nextToken();
			smallest = sTok.nextToken().equals("1");
			largest = sTok.nextToken().equals("1");
			// handle.post(new Runnable() {
			// public void run() {
			// tv.append(""+selfPort+ "Pred:" + pred + ",Succ:" + succ +
			// ",Smallest:" + smallest + ",Largest:"+largest+"\n");
			// }
			// });

			Log.i("" + selfPort, "Pred:" + pred + ",Succ:" + succ
					+ ",Smallest:" + smallest + ",Largest:" + largest);
		}

		// set Successor
		void setSucc(String msg) {
			succ = msg;
			// handle.post(new Runnable() {
			// public void run() {
			// tv.append(""+selfPort+ "Pred:" + pred + ",Succ:" + succ +
			// ",Smallest:" + smallest + ",Largest:"+largest+"\n");
			// }
			// });
			Log.i("" + selfPort, "Pred:" + pred + ",Succ:" + succ
					+ ",Smallest:" + smallest + ",Largest:" + largest);
		}

		// set Predecessor
		void setPred(String msg) {
			pred = msg;
			// handle.post(new Runnable() {
			// public void run() {
			// tv.append(""+selfPort+ "Pred:" + pred + ",Succ:" + succ +
			// ",Smallest:" + smallest + ",Largest:"+largest+"\n");
			// }
			// });
			Log.i("" + selfPort, "Pred:" + pred + ",Succ:" + succ
					+ ",Smallest:" + smallest + ",Largest:" + largest);
		}

		// build Uri
		private Uri buildUri(String scheme, String authority) {
			Uri.Builder uriBuilder = new Uri.Builder();
			uriBuilder.authority(authority);
			uriBuilder.scheme(scheme);
			return uriBuilder.build();
		}

		// insert key-value
		void insertKey(String msg) {
			Uri mUri = buildUri("content",
					"edu.buffalo.cse.cse486586.simpledht.provider");
			StringTokenizer sTok = new StringTokenizer(msg, ";");
			String key = sTok.nextToken();
			String value = sTok.nextToken();
			// Log.i("Server insertKey","key:"+key+",value:"+value);
			ContentValues cv = new ContentValues();
			cv.put("key", key);
			cv.put("value", value);
			conRes.insert(mUri, cv);
		}

		// query key
		class queryKey extends Thread {
			String msg;

			queryKey(String msg) {
				this.msg = msg;
			}

			public void run() {
				Uri mUri = buildUri("content",
						"edu.buffalo.cse.cse486586.simpledht.provider");
				StringTokenizer sTok = new StringTokenizer(msg, ";");
				String key = sTok.nextToken();
				String port = sTok.nextToken();
				Log.i("Server queryKey", "key:" + key + ",port:" + port);
				Cursor resultCursor = conRes.query(mUri, null, key, null, null);
				int valIndex = resultCursor.getColumnIndex("value");
				resultCursor.moveToFirst();
				String value = resultCursor.getString(valIndex);
				resultCursor.close();
				new Thread(new forClient(value, Integer.parseInt(port) * 2, 5))
						.start();
			}
		}// end of queryKey thread

		void getldump(String port) {
			String[] selArgs = { "ldump" };
			String ldump = "";
			Uri mUri = buildUri("content",
					"edu.buffalo.cse.cse486586.simpledht.provider");
			Cursor resultCursor = conRes.query(mUri, null, null, selArgs, null);
			int keyIndex = resultCursor.getColumnIndex("key");
			int valueIndex = resultCursor.getColumnIndex("value");
			resultCursor.moveToFirst();
			while (!resultCursor.isAfterLast()) {
				String key = resultCursor.getString(keyIndex);
				String val = resultCursor.getString(valueIndex);
				String d = key + "," + val + ";";
				ldump = ldump + d;
				resultCursor.moveToNext();
			}
			resultCursor.close();
			new Thread(new forClient(ldump, Integer.parseInt(port) * 2, 9))
					.start();
		}

		class storeldump extends Thread {
			String dump;

			storeldump(String msg) {
				this.dump = msg;
			}

			public void run() {
				StringTokenizer sTok = new StringTokenizer(dump, ";");

				while (sTok.hasMoreTokens()) {
					String temp = sTok.nextToken();
					StringTokenizer kvTok = new StringTokenizer(temp, ",");
					String key = kvTok.nextToken();
					String value = kvTok.nextToken();
					String[] row = { key, value };
					gcur.addRow(row);
				}
				gcount++;
			}
		}
	}// end of server thread
}
