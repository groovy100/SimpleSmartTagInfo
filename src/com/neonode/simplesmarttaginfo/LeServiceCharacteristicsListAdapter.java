package com.neonode.simplesmarttaginfo;

import java.util.List;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LeServiceCharacteristicsListAdapter extends BaseAdapter {
    List<BluetoothGattCharacteristic> mCharacteristics;
    LayoutInflater mInflater;
    
    private int list_item_position = 0;
    private String service_characteristic_uuid; 

    public LeServiceCharacteristicsListAdapter(Context context, List<BluetoothGattCharacteristic> characteristics) {
      mInflater = LayoutInflater.from(context);
      mCharacteristics = characteristics;
    }

    public int getCount() {
      return mCharacteristics.size();
    }
    
    private void setItemPosition(int position) {
    	list_item_position = position;
    }
    
    @SuppressWarnings("unused")
	private int getItemPosition() {
    	return list_item_position;
    }

    public Object getItem(int position) {
      return mCharacteristics.get(position);
    }

    public long getItemId(int position) {
      return position;
    }
    
    public String getItemCharacteristicUUID() {
	    return service_characteristic_uuid;
	  }

      public void setItemCharacteristicUUID(String discovered_service_characteristic_uuid) {
    	  service_characteristic_uuid = discovered_service_characteristic_uuid;
      }

    public View getView(int position, View convertView, ViewGroup parent) {
      ViewGroup vg;
      
      setItemPosition(position);

      if (convertView != null) {
        vg = (ViewGroup) convertView;
      } else {
        vg = (ViewGroup) mInflater.inflate(R.layout.list_element_characteristic_info, null);
      }

      BluetoothGattCharacteristic characteristicInfo = mCharacteristics.get(position);
      String characteristicUUID = characteristicInfo.getUuid().toString();
      String characteristicName = GattInfo.uuidToName(characteristicInfo.getUuid());
      
      setItemCharacteristicUUID(characteristicUUID);
      
      TextView snView = ((TextView) vg.findViewById(R.id.name));
      snView.setText(characteristicName);

      ((TextView) vg.findViewById(R.id.characteristic_uuid)).setText(characteristicUUID);

      return vg;
    }
}