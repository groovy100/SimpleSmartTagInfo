package com.neonode.simplesmarttaginfo;

import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LeDeviceListAdapter extends BaseAdapter {
    List<BleDeviceInfo> mDevices;
    LayoutInflater mInflater;
    
    private int list_item_position = 0;
    private String device_address;

    public LeDeviceListAdapter(Context context, List<BleDeviceInfo> devices) {
      mInflater = LayoutInflater.from(context);
      mDevices = devices;
    }

    public int getCount() {
      return mDevices.size();
    }
    
    private void setItemPosition(int position) {
    	list_item_position = position;
    }
    
    @SuppressWarnings("unused")
	private int getItemPosition() {
    	return list_item_position;
    }

    public Object getItem(int position) {
      return mDevices.get(position);
    }

    public long getItemId(int position) {
      return position;
    }
    
    private void setItemDeviceAddress(String selectedDeviceAddress) {
    	device_address = selectedDeviceAddress;
    }
    
    @SuppressWarnings("unused")
	private String getItemDeviceAddress() {
    	return device_address;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      ViewGroup vg;
      
      setItemPosition(position);


      BleDeviceInfo deviceInfo = mDevices.get(position);
      BluetoothDevice device = deviceInfo.getBluetoothDevice();
      
      setItemDeviceAddress(device.getAddress());
      
      int rssi = deviceInfo.getRssi();
      

      if (convertView != null) {
        vg = (ViewGroup) convertView;
      } else {
        vg = (ViewGroup) mInflater.inflate(R.layout.list_element_device_info, null);
      }

      TextView dnView = ((TextView) vg.findViewById(R.id.name));
      dnView.setText(device.getName());

      ((TextView) vg.findViewById(R.id.address)).setText(device.getAddress());
      ((TextView) vg.findViewById(R.id.rssi)).setText("RSSI: " + rssi);

      return vg;
    }
}