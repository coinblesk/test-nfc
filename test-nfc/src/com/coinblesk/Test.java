package com.coinblesk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import ch.uzh.csg.btlib.BTSetup;
import ch.uzh.csg.comm.CommSetup;
import ch.uzh.csg.comm.NfcInitiatorHandler;
import ch.uzh.csg.comm.NfcLibException;
import ch.uzh.csg.comm.NfcResponseHandler;
import ch.uzh.csg.comm.ResponseLater;
import ch.uzh.csg.nfclib.NfcSetup;

public class Test extends Activity {

	private static final int SEED = 42;
	final Random rnd = new Random(SEED);
	private static final String HEX_DIGITS = "0123456789abcdef";
	
	private List<byte[]> data = new ArrayList<byte[]>();
	private int dataSize = 0;
	private List<byte[]> receivedData = new ArrayList<byte[]>();
	
	private volatile CommSetup initiator = null;
	
	private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.err.println("CREATEEE");
		super.onCreate(savedInstanceState);
		
		
		
		setContentView(R.layout.activity_test);
		final Button button1 = (Button) findViewById(R.id.button1);
		button1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					send50b();
				} catch (NfcLibException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
		final Button button2 = (Button) findViewById(R.id.button2);
		button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					send500b();
				} catch (NfcLibException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});
		final Button button3 = (Button) findViewById(R.id.button3);
		button3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					send5k();
				} catch (NfcLibException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		final Button button4 = (Button) findViewById(R.id.button4);
		button4.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				initiator.stopInitiating(Test.this);
			}
		});
		
		//BLUETOOTH PART
		/*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
			
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        
		// BLUETOOTH PART
		/*
		 * We need to enforce that Bluetooth is first enabled, and take the user
		 * to settings to enable it if they have not done so.
		 */
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			// Bluetooth is disabled
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableBtIntent);
			finish();
			return;
		}

		/*
		 * Check for Bluetooth LE Support. In production, our manifest entry
		 * will keep this from installing on these devices, but this will allow
		 * test devices or other sideloads to report whether or not the feature
		 * exists.
		 */
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
			//finish();
			//return;
		}
		
		final Button button5 = (Button) findViewById(R.id.button5);
		button5.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				initiator = new BTSetup(initNfc, initResponse, Test.this);
				
			}
		});
		
	}
		
	@Override
	protected void onPause() {
		System.err.println("PAUSSEEE");
		super.onPause();
		
		if(initiator!=null) {
			initiator.shutdown(this);
		}
	}
	
	@Override
	protected void onResume() {
		System.err.println("MAKE INIT");
		super.onResume();
		try {
			if(initiator!=null) {
				initiator.shutdown(this);
			}
			initiator = new NfcSetup(initNfc, initResponse, this);
		} catch (NfcLibException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}

	private void send50b() throws NfcLibException {
		dataSize = getRepeat();
		this.data.clear();
		receivedData.clear();
		for(int i=0;i<dataSize;i++) {
			byte[] data = new byte[50];
			rnd.nextBytes(data);
			this.data.add(data);
			System.err.println("added data !!!");
		}
		
		final TextView textView1 = (TextView) findViewById(R.id.textView1);
		textView1.setText(digest(data));
		initiator.startInitiating(this);
	}

	

	private void send500b() throws NfcLibException {
		dataSize = getRepeat();
		this.data.clear();
		receivedData.clear();
		for(int i=0;i<dataSize;i++) {
			byte[] data = new byte[500];
			rnd.nextBytes(data);
			this.data.add(data);
		}

		final TextView textView1 = (TextView) findViewById(R.id.textView1);
		textView1.setText(digest(data));
		initiator.startInitiating(this);
	}

	protected void send5k() throws NfcLibException {
		dataSize = getRepeat();
		this.data.clear();
		receivedData.clear();
		for(int i=0;i<dataSize;i++) {
			byte[] data = new byte[5000];
			rnd.nextBytes(data);
			this.data.add(data);
		}		
		
		final TextView textView1 = (TextView) findViewById(R.id.textView1);
		textView1.setText(digest(data));
		initiator.startInitiating(this);
	}
	
	private int getRepeat() {
		RadioGroup g = (RadioGroup) findViewById(R.id.radioGroup1);
		RadioButton r = (RadioButton) findViewById(g.getCheckedRadioButtonId());
		if(r.getText().toString().contains("1x")) {
			return 1;
		}
		if(r.getText().toString().contains("2x")) {
			return 2;
		}
		if(r.getText().toString().contains("5x")) {
			return 5;
		}
		return -1;
	}
	
	NfcInitiatorHandler initNfc = new NfcInitiatorHandler() {
		
		@Override
		public byte[] nextMessage() {
			return data.remove(0);
		}
		
		@Override
		public boolean hasMoreMessages() {
			System.err.println("about to return: " +data.size()); 
			boolean retVal = data.size() > 0; 
			return retVal; 
		}
		
		@Override
		public boolean isFirst() {
			return data.size() == dataSize;
		}
		
		@Override
		public void handleMessageReceived(final byte[] message) {
			Test.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					receivedData.add(message);
					final TextView textView2 = (TextView) findViewById(R.id.textView2);
					textView2.setText(digest(receivedData));
				}
			});
			
		}
		
		@Override
		public void handleStatus(String message) {
			System.err.println("status sender: " + message);
		}
		
		@Override
		public void handleFailed(String message) {
			System.err.println("error sender: " + message);
		}
	};
	
	NfcResponseHandler initResponse = new NfcResponseHandler() {
		
		@Override
		public void handleStatus(String message) {
			System.err.println("status recipient: " + message);
		}
		
		@Override
		public byte[] handleMessageReceived(final byte[] message,
				final ResponseLater responseLater) {
			final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
			
			final int nr = toggleButton.isChecked() ? 100 : 0;
			if(nr == 0) {
				return message;
			} else {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						try {
							Thread.sleep(nr);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						responseLater.response(message);
						
					}
				}).start();
				return null;
			}
		}
		
		@Override
		public void handleFailed(String message) {
			System.err.println("error recipient: " + message);
			
		}
	};

	private static String digest(List<byte[]> input) {
		MessageDigest digester;
		try {
			
			digester = MessageDigest.getInstance("MD5");
			for(byte[] inp:input) {
				System.err.println("udptae with: "+Arrays.toString(inp));
				digester.update(inp);
				System.err.println("done with: "+Arrays.toString(inp));
			}
			byte[] output = digester.digest();
			System.err.println("output with: "+Arrays.toString(output));
			return toHex(output);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "n/a";

	}

	private static String toHex(byte[] data) {
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i != data.length; i++) {
			int v = data[i] & 0xff;

			buf.append(HEX_DIGITS.charAt(v >> 4));
			buf.append(HEX_DIGITS.charAt(v & 0xf));

			buf.append(" ");
		}

		return buf.toString();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.test, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
