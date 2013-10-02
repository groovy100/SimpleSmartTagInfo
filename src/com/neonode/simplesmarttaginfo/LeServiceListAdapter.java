package com.neonode.simplesmarttaginfo;

import java.util.List;

import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LeServiceListAdapter extends BaseAdapter {
    List<BluetoothGattService> mServices;
    LayoutInflater mInflater;
    
    private int list_item_position = 0;
    private String service_uuid; 

    public LeServiceListAdapter(Context context, List<BluetoothGattService> services) {
      mInflater = LayoutInflater.from(context);
      mServices = services;
    }

    public int getCount() {
      return mServices.size();
    }
    
    private void setItemPosition(int position) {
    	list_item_position = position;
    }
    
    @SuppressWarnings("unused")
	private int getItemPosition() {
    	return list_item_position;
    }

    public Object getItem(int position) {
      return mServices.get(position);
    }

    public long getItemId(int position) {
      return position;
    }
    
    public String getItemServiceUUID() {
	    return service_uuid;
	  }

      public void setItemServiceUUID(String discovered_service_uuid) {
    	  service_uuid = discovered_service_uuid;
      }

    public View getView(int position, View convertView, ViewGroup parent) {
      ViewGroup vg;
      
      setItemPosition(position);

      if (convertView != null) {
        vg = (ViewGroup) convertView;
      } else {
        vg = (ViewGroup) mInflater.inflate(R.layout.list_element_service_info, null);
      }

      BluetoothGattService serviceInfo = mServices.get(position);
      String serviceUUID = serviceInfo.getUuid().toString();
      String serviceName = GattInfo.uuidToName(serviceInfo.getUuid());
      
      setItemServiceUUID(serviceUUID);
      
      TextView snView = ((TextView) vg.findViewById(R.id.name));
      snView.setText(serviceName);

      ((TextView) vg.findViewById(R.id.service_uuid)).setText(serviceUUID);

      return vg;
    }
}