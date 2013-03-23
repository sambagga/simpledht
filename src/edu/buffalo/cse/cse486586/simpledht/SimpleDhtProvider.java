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

    @Override
    public Uri insert(Uri uri, ContentValues values) {
    	String key = (String) values.get(KEY_S);
		String value = (String) values.get(VALUE_S);
		key = key.concat(".txt");
		try {
			FileOutputStream fos = pcontext.openFileOutput(key, Context.MODE_PRIVATE);
			OutputStreamWriter osw = new OutputStreamWriter(fos);
			osw.write(value);
			osw.flush();
			osw.close();
		} catch (Exception e) {
			Log.i("chatStorage, fileCreate()", "Exception e = " + e);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return uri;
    }

    @Override
    public boolean onCreate() {
    	pcontext = getContext();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
    	String[] cols = {KEY_S,VALUE_S};
		String fname = selection.concat(".txt");
		String value;
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
		MatrixCursor cur = new MatrixCursor(cols);
		String[] row = {selection,value};
		cur.addRow(row);
		return cur;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
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
 		String message, id,TAG = "forClient";
 		int type;

 		forClient(String msg, String msgID, int msgType) {
 			message = msg;
 			id = msgID;
 			type = msgType;
 		}

 		public void run() {
 			Socket clSock;
 			try {
 				Log.i("Client Thread", message);
 				// multicast the message to all the avd's
 				for (int port = 11108; port <= 11116; port += 4) {
 					// connect to server
 					clSock = new Socket("10.0.2.2", port);
 					Log.i("Sending Message type" + type + "=", message);
 					// send the message to server
 					PrintWriter sendData = new PrintWriter(
 							clSock.getOutputStream());
 					if (type == 1) // normal chat message
 						sendData.println("%" + id + ";" + message);
 					if (type == 2) // message from sequencer, to be added in
 									// sequence buffer
 						sendData.println("$" + id + ";" + message);
 					if (type == 3) // initial message to identify test case 2
 						sendData.println("@" + id + ";" + message);
 					sendData.flush();
 					sendData.close();
 					Log.i("Message sent=", message);
 				}
 				if (type == 1 || type == 3) {
 					clSock = new Socket("10.0.2.2", 11108);
 					Log.i("Sending Message to sequencer=", id);
 					// send the message to sequencer
 					PrintWriter sendData = new PrintWriter(
 							clSock.getOutputStream());
 					sendData.println("#" + id);
 					sendData.flush();
 					sendData.close();
 					Log.i("Message sent to sequencer=", id);
 				}

 			} catch (NumberFormatException e) {
 				// TODO Auto-generated catch block
 				Log.i(TAG,"Number format Exception!\n");
 				e.printStackTrace();
 			} catch (UnknownHostException e) {
 				// TODO Auto-generated catch block
 				Log.i(TAG,"Unknown Host Exception!\n");
 				e.printStackTrace();
 			} catch (IOException e) {
 				// TODO Auto-generated catch block
 				Log.i(TAG,"I/O error occured when creating the socket!\n");
 				e.printStackTrace();
 			} catch (SecurityException e) {
 				Log.i(TAG,"Security Exception!\n");
 				e.printStackTrace();
 			}

 		}
 	}// end of client thread
 	
 
}
