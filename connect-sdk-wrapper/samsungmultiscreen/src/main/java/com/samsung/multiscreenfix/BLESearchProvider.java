//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreenfix;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.Context;
import com.mega.cast.utils.log.SmartLog;

import com.samsung.multiscreenfix.ble.adparser.AdElement;
import com.samsung.multiscreenfix.ble.adparser.AdParser;
import com.samsung.multiscreenfix.ble.adparser.TypeManufacturerData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BLESearchProvider extends SearchProvider {
    private static final String TAG = "BLESearchProvider";
    private static final String BLE_NETWORK_TYPE = "BLE";
    private static final String BLE_NOT_SUPPORTED = "BLE is not supported";
    private static final String BLUETOOTH_NOT_SUPPORTED = "Bluetooth not supported";
    private static final String SAMSUNG_MANUFACTURER_ID = "0075";
    private static final int BLE_RSSI_MIMIMUM = -100;
    private static final byte SAMSUNG_DEVICE_STATUS = 20;
    private final Context context;
    private BluetoothAdapter bluetoothAdapter;
    private static final long DEFAULT_TTL = 5000L;
    private final Map<BLESearchProvider.BluetoothService, Long> devices = new ConcurrentHashMap();
    private final ArrayList<String> BT_devices = new ArrayList();
    private final LeScanCallback leScanCallback = new LeScanCallback() {
        private boolean isTV(byte[] advertisedData) {
            boolean isTV = false;
            ArrayList ads = AdParser.parseAdData(advertisedData);
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < ads.size(); ++i) {
                AdElement e = (AdElement)ads.get(i);
                if(i > 0) {
                    sb.append(" ; ");
                }

                sb.append(e.toString());
                if(e instanceof TypeManufacturerData) {
                    TypeManufacturerData em = (TypeManufacturerData)e;
                    if(em.getManufacturer().equals("0075")) {
                        byte[] data = em.getBytes();
                        byte version = data[0];
                        byte serviceId = data[1];
                        byte deviceType = data[2];
                        if(BLESearchProvider.DeviceType.TV.getValue() == deviceType && data[3] == 1) {
                            isTV = true;
                        }
                    }
                }
            }

            return isTV;
        }

        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            StringBuilder sb = new StringBuilder();
            byte[] btService = scanRecord;
            int var6 = scanRecord.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                byte b = btService[var7];
                sb.append(String.format("%02x", new Object[]{Byte.valueOf(b)}));
            }

            BLESearchProvider.BluetoothService var9 = BLESearchProvider.this.new BluetoothService(device);
            if(!BLESearchProvider.this.devices.containsKey(var9)) {
                BLESearchProvider.this.updateAlive(var9, 5000L);
                if(this.isTV(scanRecord) && !BLESearchProvider.this.BT_devices.contains(device.getAddress())) {
                    BLESearchProvider.this.BT_devices.add(device.getAddress());
                    if(rssi >= -100) {
                        BLESearchProvider.this.addTVOnlyBle(device.getName());
                    }
                }
            } else {
                BLESearchProvider.this.updateAlive(var9, 5000L);
            }

            this.reapServices();
        }

        private void reapServices() {
            long now = System.currentTimeMillis();
            Iterator var3 = BLESearchProvider.this.devices.keySet().iterator();

            while(var3.hasNext()) {
                BLESearchProvider.BluetoothService btService = (BLESearchProvider.BluetoothService)var3.next();
                long expires = ((Long)BLESearchProvider.this.devices.get(btService)).longValue();
                if(expires < now) {
                    Service service = BLESearchProvider.this.getServiceById(btService.getId());
                    BLESearchProvider.this.devices.remove(btService);
                    BLESearchProvider.this.removeServiceAndNotify(service);
                }
            }

        }
    };

    private void updateAlive(BLESearchProvider.BluetoothService btService, long ttl) {
        long now = System.currentTimeMillis();
        long expires = now + ttl;
        this.devices.put(btService, Long.valueOf(expires));
    }

    private BLESearchProvider(Context context) {
        this.context = context;
        this.setupBluetoothAdapter();
    }

    private BLESearchProvider(Context context, Search.SearchListener searchListener) {
        super(searchListener);
        this.context = context;
        this.setupBluetoothAdapter();
    }

    private void setupBluetoothAdapter() {
        if(!this.context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            throw new UnsupportedOperationException("BLE is not supported");
        } else {
            BluetoothManager bluetoothManager = (BluetoothManager)this.context.getSystemService("bluetooth");
            this.bluetoothAdapter = bluetoothManager.getAdapter();
            if(this.bluetoothAdapter == null) {
                throw new UnsupportedOperationException("Bluetooth not supported");
            }
        }
    }

    public void start() {
        SmartLog.w("BLESearchProvider", "Start BLE search");
        if(this.searching) {
            this.stop();
        }

        this.devices.clear();
        this.BT_devices.clear();
        this.TVListOnlyBle.clear();
        this.clearServices();
        this.searching = this.bluetoothAdapter.startLeScan(this.leScanCallback);
    }

    public boolean stop() {
        SmartLog.w("BLESearchProvider", "Stop BLE search");
        if(!this.searching) {
            return false;
        } else {
            this.searching = false;
            this.bluetoothAdapter.stopLeScan(this.leScanCallback);
            return true;
        }
    }

    public static SearchProvider create(Context context) {
        return new BLESearchProvider(context);
    }

    static SearchProvider create(Context context, Search.SearchListener searchListener) {
        return new BLESearchProvider(context, searchListener);
    }

    private class BluetoothService {
        private final BluetoothDevice device;
        private String id;

        public BluetoothService(BluetoothDevice device) {
            this.device = device;
        }

        public BluetoothDevice getDevice() {
            return this.device;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String toString() {
            return "BLESearchProvider.BluetoothService(device=" + this.getDevice() + ", id=" + this.getId() + ")";
        }

        public boolean equals(Object o) {
            if(o == this) {
                return true;
            } else if(!(o instanceof BLESearchProvider.BluetoothService)) {
                return false;
            } else {
                BLESearchProvider.BluetoothService other = (BLESearchProvider.BluetoothService)o;
                if(!other.canEqual(this)) {
                    return false;
                } else {
                    BluetoothDevice this$device = this.getDevice();
                    BluetoothDevice other$device = other.getDevice();
                    if(this$device == null) {
                        if(other$device != null) {
                            return false;
                        }
                    } else if(!this$device.equals(other$device)) {
                        return false;
                    }

                    return true;
                }
            }
        }

        protected boolean canEqual(Object other) {
            return other instanceof BLESearchProvider.BluetoothService;
        }

        public int hashCode() {
            boolean PRIME = true;
            byte result = 1;
            BluetoothDevice $device = this.getDevice();
            int result1 = result * 59 + ($device == null?0:$device.hashCode());
            return result1;
        }
    }

    private static enum DeviceType {
        Unknown(0),
        TV(1),
        Mobile(2),
        PXD(3),
        AVDevice(4);

        private final int deviceType;

        private DeviceType(int deviceType) {
            this.deviceType = deviceType;
        }

        public int getValue() {
            return this.deviceType;
        }
    }
}
