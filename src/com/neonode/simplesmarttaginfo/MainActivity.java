package com.neonode.simplesmarttaginfo;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

public class MainActivity extends Activity {
	private final static String TAG = MainActivity.class.getSimpleName();
	
	/* BUTTONS */	
	private Button scanDevicesButton;
	private Button connectKnownMACDeviceButton;
	private EditText etMacAddress;
	
	private LeDeviceListAdapter mDeviceAdapter = null;
	private LeServiceListAdapter mServiceAdapter = null;
	private LeServiceCharacteristicsListAdapter mCharacteristicAdapter = null;
	
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothGattService mBluetoothGattService;
	private List<BleDeviceInfo> mDeviceInfoList;
	private List<BluetoothGattService> mBluetoothServicesList;
	private List<BluetoothGattCharacteristic> mGattCharacteristicsList;
	private ListView mDeviceListView;
	private ListView mServiceListView;
	private ListView mCharacteristicsListView;
	
	private Handler mBTScanHandler;
	private boolean mScanning;
	
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;
	
	private static long last_notification_time_ms = 0;
	private static long before_last_notification_time_ms = 0;
	private static long notification_delay_ms = 0;
	private static long min_notification_delay_ms = 999999999;
	
	private Resources res;
	
	// Requests to other activities
	private static final int REQ_ENABLE_BT = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/*********** INITIALISE ELEMENTS AND DATA ***********/
		res = this.getResources();
		
		mBTScanHandler = new Handler();
		
		/* get a reference to the buttons */
		scanDevicesButton = (Button)findViewById(R.id.btn_scan);
		scanDevicesButton.setOnClickListener(scanDevicesButtonListener);
		
		connectKnownMACDeviceButton = (Button)findViewById(R.id.btn_connect_known_mac);
		connectKnownMACDeviceButton.setOnClickListener(connectKnownMACDeviceButtonListener);
		
		/* get a reference to text views and edittexts */
		etMacAddress = (EditText)findViewById(R.id.et_mac_address);
		
		//Associating the list view
		mDeviceListView = (ListView)findViewById(R.id.lv_deviceList);
		mServiceListView = (ListView)findViewById(R.id.lv_serviceList);
		mCharacteristicsListView = (ListView)findViewById(R.id.lv_characteristicsList);
		
		
		 // Initialize device list container and device filter
	    mDeviceInfoList = new ArrayList<BleDeviceInfo>();

	    createIntentFilters();
	    initialiseGattService();
		initialiseBTAdapter();
	}
	
	private void initialiseBTAdapter() {
		// Use this check to determine whether BLE is supported on the device. Then
		// you can selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
		    Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
		    finish();
		}
				
		// Initializes Bluetooth adapter.
		final BluetoothManager bluetoothManager =
		        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		
		// Ensures Bluetooth is available on the device and it is enabled. If not,
		// displays a dialog requesting user permission to enable Bluetooth.
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
		}
	}
	
	private void initialiseGattService() {
		// GATT database
	    XmlResourceParser xpp = res.getXml(R.xml.gatt_uuid);
	    new GattInfo(xpp);
	}
	
	
	private void scanLeDevice(final boolean enable) {
		if (enable && !mScanning) {
			if (mDeviceInfoList != null && mDeviceAdapter != null) {
				mDeviceInfoList.clear();
				mDeviceAdapter.notifyDataSetChanged();
			}
			
			if (mBluetoothServicesList != null && mServiceAdapter != null) {
				mBluetoothServicesList.clear();
				mServiceAdapter.notifyDataSetChanged();
			}
			
			if (mGattCharacteristicsList != null && mCharacteristicAdapter != null) {
				mGattCharacteristicsList.clear();
				mCharacteristicAdapter.notifyDataSetChanged();
			}
			
            // Stops scanning after a pre-defined scan period.
			mBTScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
		else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
	}
	
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
	    public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
	    	runOnUiThread(new Runnable() {
				public void run() {
					if (!deviceInfoExists(device.getAddress())) {
	        			// New device
	        			BleDeviceInfo deviceInfo = createDeviceInfo(device, rssi);
	        			addDevice(deviceInfo);
					}
					else {
	        			// Already in list, update RSSI info
	        			BleDeviceInfo deviceInfo = findDeviceInfo(device);
	        			deviceInfo.updateRssi(rssi);
	        			notifyDeviceDataSetChanged();
	        		}
				}
	    	});
	    }
	};
	  
	    
	private BleDeviceInfo findDeviceInfo(BluetoothDevice device) {
		for (int i = 0; i < mDeviceInfoList.size(); i++) {
			if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress().equals(device.getAddress())) {
				return mDeviceInfoList.get(i);
	    	}
	    }
	    return null;
	}
	  
	private BleDeviceInfo createDeviceInfo(BluetoothDevice device, int rssi) {
		BleDeviceInfo deviceInfo = new BleDeviceInfo(device, rssi);

	    return deviceInfo;
	}
	  
	private void addDevice(BleDeviceInfo device) {
		mDeviceInfoList.add(device);
	    notifyDeviceDataSetChanged();
	}
	  
	private boolean deviceInfoExists(String address) {
		for (int i = 0; i < mDeviceInfoList.size(); i++) {
			if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress().equals(address)) {
				return true;
			}
	    }
	    return false;
	}
	
	  
	private void notifyDeviceDataSetChanged() {
		if (mDeviceAdapter == null) {
			mDeviceAdapter = new LeDeviceListAdapter(this, mDeviceInfoList);
	    }
	    mDeviceListView.setAdapter(mDeviceAdapter);
	    
	    mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

		    @Override
		    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
		    	Intent device_selected_intent = new Intent();
		        device_selected_intent.setAction("com.neonode.SimpleSmartTagInfo.DEVICE_SELECTED");
		        device_selected_intent.putExtra("device_listview_position", position);
	
		        sendBroadcast(device_selected_intent);
		    }

	    });
	    
	    mDeviceAdapter.notifyDataSetChanged();
	}
	  
	private void notifyServiceInfoDataSetChanged() {
		if (mBluetoothGatt != null) {
			mBluetoothServicesList = mBluetoothGatt.getServices();
	    	mServiceAdapter = new LeServiceListAdapter(this, mBluetoothServicesList);
	    	mServiceListView.setAdapter(mServiceAdapter);
	    	
	    	mServiceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		        @Override
		        public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
		        	Intent service_selected_intent = new Intent();
	            	service_selected_intent.setAction("com.neonode.SimpleSmartTagInfo.SERVICE_SELECTED");
	            	service_selected_intent.putExtra("service_item_uuid", mBluetoothServicesList.get(position).getUuid().toString());

	            	sendBroadcast(service_selected_intent);
	            }
	    	});
		}
	}
	  
	private void notifyServiceSelectionSetChanged(String serviceItemUUID) {
		if (mBluetoothGatt != null) {
			mBluetoothGattService = mBluetoothGatt.getService(UUID.fromString(serviceItemUUID));
		    	
		    if (mBluetoothGattService != null) {
		    	mGattCharacteristicsList = mBluetoothGattService.getCharacteristics();
		    	mCharacteristicAdapter = new LeServiceCharacteristicsListAdapter(this, mGattCharacteristicsList);
		    	mCharacteristicsListView.setAdapter(mCharacteristicAdapter);
		    	
		    	mCharacteristicsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

		    		@Override
				    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
		    			Intent characteristic_selected_intent = new Intent();
			            characteristic_selected_intent.setAction("com.neonode.SimpleSmartTagInfo.CHARACTERISTIC_SELECTED");
			            characteristic_selected_intent.putExtra("characteristic_item_uuid", mGattCharacteristicsList.get(position).getUuid().toString());
	
			            sendBroadcast(characteristic_selected_intent);
			        }
		    	});
		    }
		}
	}
	  
	private void notifyCharacteristicSelectionSetChanged(String serviceCharacteristicItemUUID) {
		BluetoothGattCharacteristic bleChosenCharacteristic = mBluetoothGattService.getCharacteristic(UUID.fromString(serviceCharacteristicItemUUID));
		  
		mBluetoothGatt.setCharacteristicNotification(bleChosenCharacteristic, true);
		  
		BluetoothGattDescriptor clientConfig = bleChosenCharacteristic.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
		  
		clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		mBluetoothGatt.writeDescriptor(clientConfig);
		  
		/*  DEBUG CODE
			List<BluetoothGattDescriptor> bluetoothGattDescriptors = bleChosenCharacteristic.getDescriptors();
			  
			Log.e(TAG, "CHAR: " + bluetoothGattDescriptors.size());
			for (int i=0; i < bluetoothGattDescriptors.size(); i++) {
				try {
					Log.e(TAG, "CHAR: " + bluetoothGattDescriptors.get(i).getValue().toString());
				}
				catch(Exception e) {
					Log.e(TAG, "ERR: " + e.getMessage());
				}
			}
		*/
	}
	  
	  
	private void connectToGatt(int devicePosition) {
		scanLeDevice(false);
		  
		BluetoothDevice bleDevice = mDeviceInfoList.get(devicePosition).getBluetoothDevice();
		mBluetoothGatt = bleDevice.connectGatt(this, false, mGattCallback);
		  
		if (mBluetoothServicesList != null && mServiceAdapter != null) {
			mBluetoothServicesList.clear();
			mServiceAdapter.notifyDataSetChanged();
		}
		  
		if (mGattCharacteristicsList != null && mCharacteristicAdapter != null) {
			mGattCharacteristicsList.clear();
			mCharacteristicAdapter.notifyDataSetChanged();
		}
	}
	
	private void connectKnownDeviceToGatt(String deviceMacAddress) {
		scanLeDevice(false);
		  
		BluetoothDevice bleDevice = mBluetoothAdapter.getRemoteDevice(deviceMacAddress);
		mBluetoothGatt = bleDevice.connectGatt(this, false, mGattCallback);
		  
		if (mBluetoothServicesList != null && mServiceAdapter != null) {
			mBluetoothServicesList.clear();
			mServiceAdapter.notifyDataSetChanged();
		}
		  
		if (mGattCharacteristicsList != null && mCharacteristicAdapter != null) {
			mGattCharacteristicsList.clear();
			mCharacteristicAdapter.notifyDataSetChanged();
		}
	}
	
	// Activity result handling
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case REQ_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					Toast.makeText(this, R.string.bt_on, Toast.LENGTH_SHORT).show();
				}
				else {
					// User did not enable Bluetooth or an error occurred
					Toast.makeText(this, R.string.bt_not_on, Toast.LENGTH_SHORT).show();
			        finish();
				}
				break;
		    default:
		    	Log.e(TAG, "Unknown request code");
		    	break;
		}
	}
	
	/*********** INITIALISE THE LISTENERS ***********/
    private OnClickListener scanDevicesButtonListener = new OnClickListener() {
    	public void onClick(View v) {
    		scanLeDevice(true);
    	}
    };
    
    private OnClickListener connectKnownMACDeviceButtonListener = new OnClickListener() {
    	public void onClick(View v) {
    		if (etMacAddress.getText().toString().length() == 17) {
    			connectKnownDeviceToGatt(etMacAddress.getText().toString());
    		}
    	}
    };
    
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    	
    	@Override
    	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    		if (newState == BluetoothProfile.STATE_CONNECTED) {
    			mBluetoothGatt.discoverServices();
	    		Log.e(TAG, "Connected to GATT server.");
    		}
    		else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
    			Log.i("BTGATT", "Disconnected from GATT server.");
            }
    	}
    	
    	@Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	final Intent services_discovered_intent = new Intent("com.neonode.SimpleSmartTagInfo.SERVICES_DISCOVERED");
                sendBroadcast(services_discovered_intent);
            }
            else {
                Log.w("BTGATT", "onServicesDiscovered received: " + status);
            }
        }
    	
    	
    	@Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	//broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
    	
    	@Override
    	// Characteristic notification
    	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    		before_last_notification_time_ms = last_notification_time_ms;
    		last_notification_time_ms = System.currentTimeMillis();
    		
    		notification_delay_ms = last_notification_time_ms - before_last_notification_time_ms;
    		
    		if (notification_delay_ms > 1000) {
    			min_notification_delay_ms = 999999999;
    		}
    		
    		if ((last_notification_time_ms - before_last_notification_time_ms) < min_notification_delay_ms) {
    			min_notification_delay_ms = last_notification_time_ms - before_last_notification_time_ms;
    		}
    		
    		runOnUiThread(new Runnable() {
    			public void run() {
		    		TextView tv_current_notification_delay = (TextView)findViewById(R.id.tv_current_notification_delay);
		    		tv_current_notification_delay.setText(notification_delay_ms + "ms");
		    		
		    		TextView tv_min_notification_delay = (TextView)findViewById(R.id.tv_min_notification_delay);
		    		tv_min_notification_delay.setText(min_notification_delay_ms + "ms");
    			}
    		});
    		
    		byte[] receivedValue = characteristic.getValue();

    		boolean[] receivedValueBool = byteArray2BitArray(receivedValue);
    		
    		//true = right  0 bottom 7 top
    		//false = left    0 bottom 7 top
    		
    		/*
    		if (receivedValueBool[0]) {
				Log.e(TAG, "OUT: " + HexToBinary(bytesToHexString(receivedValue)));
			}
			*/
    		
    		for (int i=1; i<receivedValueBool.length; i++) {
    			toggleSensorSymbol(receivedValueBool[0], i, receivedValueBool[i]);
    		}
    	}
    };
    
    private void toggleSensorSymbol(boolean sensorSide, int sensorNumber, final boolean sensorState) {
    	String sensorIDName = "sensor" + sensorSide + "" + sensorNumber;
    	
    	final int sensorResID = getResources().getIdentifier(sensorIDName, "id", "com.neonode.simplesmarttaginfo");
    	
		runOnUiThread(new Runnable() {
			public void run() {
				TextView tvSensor = (TextView)findViewById(sensorResID);
				
				if (sensorState) {
					tvSensor.setText("X");
				}
				else {
					tvSensor.setText(" ");
				}
		    }
		});
	}
    
    private static boolean[] byteArray2BitArray(byte[] bytes) {
	    boolean[] bits = new boolean[bytes.length * 8];
	    
	    for (int i = 0; i < bytes.length * 8; i++) {
	      if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0)
	        bits[i] = true;
	    }
	    return bits;
	}
    
    @SuppressWarnings("unused")
	private static String bytesToHexString(byte[] bytes) {  
        StringBuilder sb = new StringBuilder(bytes.length * 2);  
      
        Formatter formatter = new Formatter(sb);  
        for (byte b : bytes) {  
            formatter.format("%02x", b);  
        }  
        
        formatter.close();
        
        return sb.toString();  
    }
    
    @SuppressWarnings("unused")
	private String HexToBinary(String Hex) {
        int i = Integer.parseInt(Hex, 16);
        String Bin = Integer.toBinaryString(i);
        return Bin;
    }
    
    
    private void createIntentFilters() {
    	IntentFilter device_selected_filter = new IntentFilter("com.neonode.SimpleSmartTagInfo.DEVICE_SELECTED");
		registerReceiver(generalBroadcastReceiver, device_selected_filter);
		
		IntentFilter services_discovered_filter = new IntentFilter("com.neonode.SimpleSmartTagInfo.SERVICES_DISCOVERED");
		registerReceiver(generalBroadcastReceiver, services_discovered_filter);
		
		IntentFilter service_selected_filter = new IntentFilter("com.neonode.SimpleSmartTagInfo.SERVICE_SELECTED");
		registerReceiver(generalBroadcastReceiver, service_selected_filter);
		
		IntentFilter characteristic_selected_filter = new IntentFilter("com.neonode.SimpleSmartTagInfo.CHARACTERISTIC_SELECTED");
		registerReceiver(generalBroadcastReceiver, characteristic_selected_filter);
	}
    
    /*********** GENERAL PURPOSE BROADCAST RECEIVER ***********/
	private final BroadcastReceiver generalBroadcastReceiver = new BroadcastReceiver() {
		@Override
    	public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
           
            if (intentAction.contentEquals("com.neonode.SimpleSmartTagInfo.DEVICE_SELECTED")) {
            	int devicePosition = intent.getIntExtra("device_listview_position", -1);
            	Log.e(TAG, "DEVICE SELECTED: " + devicePosition);
            	
            	if (devicePosition >= 0) {
            		connectToGatt(devicePosition);
            	}
            }
            else if (intentAction.contentEquals("com.neonode.SimpleSmartTagInfo.SERVICES_DISCOVERED")) {
            	Log.e(TAG, "SERVICES DISCOVERED");
            	
        	    if (mBluetoothGatt != null) {
        	    	notifyServiceInfoDataSetChanged();
        	    }
            }
            else if (intentAction.contentEquals("com.neonode.SimpleSmartTagInfo.SERVICE_SELECTED")) {
            	String serviceItemUUID = intent.getStringExtra("service_item_uuid");
            	Log.e(TAG, "SERVICE SELECTED: " + serviceItemUUID);
            	
            	notifyServiceSelectionSetChanged(serviceItemUUID);
            }
            else if (intentAction.contentEquals("com.neonode.SimpleSmartTagInfo.CHARACTERISTIC_SELECTED")) {
            	String serviceCharacteristicItemUUID = intent.getStringExtra("characteristic_item_uuid");
            	Log.e(TAG, "CHARACTERISTIC SELECTED: " + serviceCharacteristicItemUUID);
            	
            	notifyCharacteristicSelectionSetChanged(serviceCharacteristicItemUUID);
            }
		}
	};
	
	@Override
    public void onDestroy() {
        super.onStop();

        if (mBluetoothGatt == null) {
        	return;
        }
        
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}