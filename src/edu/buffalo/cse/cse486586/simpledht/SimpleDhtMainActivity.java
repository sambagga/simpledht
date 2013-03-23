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

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class SimpleDhtMainActivity extends Activity {
	String selfPort;
	Handler handle = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dht_main);

		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());
		selfPort = getAVD();
		findViewById(R.id.button3).setOnClickListener(
				new OnTestClickListener(tv, getContentResolver()));

		// initiate the server thread
		Thread serv = new forServer();
		serv.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
		return true;
	}

	// to identify AVD executing the app
	public String getAVD() {
		TelephonyManager tel = (TelephonyManager) getBaseContext()
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
				Log.i("Client Thread", msg);
				// connect to server
				clSock = new Socket("10.0.2.2", port);
				Log.i("Sending Message type" + type + "=", msg);
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
				sendData.flush();
				sendData.close();
				Log.i("Message sent=", msg);

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
	
	private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
	
	// server thread
	class forServer extends Thread {
		String succ;
		String pred;
		Boolean largest;
		Boolean smallest;
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
				Log.d("Starting Server", "Forserver");
				while (true) {
					// listen for client
					Socket recvSock = serSock.accept();
					Log.i("Connection", "Accepted");
					// get the message
					InputStreamReader readStream = new InputStreamReader(
							recvSock.getInputStream());
					BufferedReader recvInp = new BufferedReader(readStream);
					Log.i("Reader", "Initialized");
					String recvMsg = recvInp.readLine();
					Log.i("Received Message:", recvMsg);
					// recognise message type
					switch (recvMsg.charAt(0)) {
					case '%': // insert
						break;
					case '$': // query
						break;
					case '@': // query response

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
		
		void joinNode(String newNode) throws NoSuchAlgorithmException{
			String currNode = selfPort;
			int small=0, large=0;
			if(genHash(newNode).compareTo(currNode)<0){
				if(genHash(newNode).compareTo(pred)>0 || smallest==true){
					if(smallest==true){
						smallest = false;
						small=1;
					}
					new Thread(new forClient(pred+";"+currNode+";"+small+";"+large, Integer.parseInt(newNode)*2, 2)).start();
					new Thread(new forClient(newNode, Integer.parseInt(pred)*2, 3)).start();
					pred = newNode;
				}
				else if(genHash(newNode).compareTo(pred)<0){
					new Thread(new forClient(newNode, Integer.parseInt(pred), 1)).start();
				}
			}
			else if(genHash(newNode).compareTo(currNode)>0){
				if(genHash(newNode).compareTo(succ)<0 || largest==true){
					if(largest==true){
						largest = false;
						large=1;
					}
					new Thread(new forClient(currNode+";"+succ+";"+small+";"+large, Integer.parseInt(newNode)*2, 2)).start();
					new Thread(new forClient(newNode, Integer.parseInt(succ)*2, 4)).start();
					succ = newNode;
				}
				else if(genHash(newNode).compareTo(pred)<0){
					new Thread(new forClient(newNode, Integer.parseInt(succ), 1)).start();
				}
			}
		}
		
		//update node
		void updateNode(String msg){
			StringTokenizer sTok = new StringTokenizer(msg, ";");
			pred = sTok.nextToken();
			succ = sTok.nextToken();
			smallest = sTok.nextToken().equals("1");
			largest = sTok.nextToken().equals("1");
			Log.i(""+selfPort, "Pred:" + pred + ",Succ:" + succ + ",Smallest:" + smallest + ",Largest:"+largest);
		}
		
		//set Successor
		void setSucc(String msg){
			succ = msg;
			Log.i(""+selfPort, "Pred:" + pred + ",Succ:" + succ + ",Smallest:" + smallest + ",Largest:"+largest);
		}
		
		//set Predecessor
		void setPred(String msg){
			pred = msg;
			Log.i(""+selfPort, "Pred:" + pred + ",Succ:" + succ + ",Smallest:" + smallest + ",Largest:"+largest);
		}
	}// end of server thread
}
