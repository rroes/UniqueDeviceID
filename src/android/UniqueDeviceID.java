package hu.dpal.phonegap.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.os.Build;

import java.lang.reflect.Method;

public class UniqueDeviceID extends CordovaPlugin {

    public static final String TAG = "UniqueDeviceID";
    public CallbackContext callbackContext;
    public static final int REQUEST_READ_PHONE_STATE = 0;

    protected final static String permission = Manifest.permission.READ_PHONE_STATE;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        try {
            if (action.equals("get")) {
                if(this.hasPermission(permission)){
                    getDeviceId();
                }else{
                    this.requestPermission(this, REQUEST_READ_PHONE_STATE, permission);
                }
            }else {
                this.callbackContext.error("Invalid action");
                return false;
            }
        }catch(Exception e ) {
            this.callbackContext.error("Exception occurred: ".concat(e.getMessage()));
            return false;
        }
        return true;

    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        if(requestCode == REQUEST_READ_PHONE_STATE){
            getDeviceId();
        }
    }

    protected void getDeviceId(){
        try {
            Context context = cordova.getActivity().getApplicationContext();
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            
            String uuid;
            String uuid1;
            String uuid2;
            // 1. Android ID
            String androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            Log.d("UniqueDeviceID","Android ID is used");
            
            // 2. Device ID
            String deviceID;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                deviceID = tm.getImei();
                Log.d("UniqueDeviceID","IMEI is used");
            } else {
                deviceID = tm.getDeviceId();
                Log.d("UniqueDeviceID","DEVICE ID is used");
            }
            
            // 3. SIM ID - not for TomTom Devices because SIM performance is poor
            //    and returns null values randomly
            String simID = "";
            String simID1 = "";
            String simID2 = "";
            if (Build.MANUFACTURER.equals("TomTom")) {
                simID = "0";
            }else{
                simID = tm.getSimSerialNumber();
                
                SubscriptionManager sManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
   
                SubscriptionInfo infoSim1 = sManager.getActiveSubscriptionInfoForSimSlotIndex(0);
                if (infoSim1 != null){
                    simID1 = infoSim1.getIccId();
                }     

                SubscriptionInfo infoSim2 = sManager.getActiveSubscriptionInfoForSimSlotIndex(1);
                if (infoSim2 != null){
                    simID2 = infoSim2.getIccId();
                }  

                Log.d("UniqueDeviceID","SIM ID is used");
            }
            
            if ("9774d56d682e549c".equals(androidID) || androidID == null) {
                androidID = "";
            }
            Log.d("UniqueDeviceID",androidID);

            if (deviceID == null) {
                deviceID = "";
            }
            Log.d("UniqueDeviceID",deviceID);

            if (simID == null) {
                simID = "";
            }

            
            Log.d("UniqueDeviceID",simID);
            
            
            uuid = androidID + deviceID + simID;
            uuid = String.format("%32s", uuid).replace(' ', '0');
            uuid = uuid.substring(0, 32);
            uuid = uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            Log.d("UniqueDeviceID",uuid);
            
            if (simID1 != "") {
            uuid1 = androidID + deviceID + simID1;
            uuid1 = String.format("%32s", uuid1).replace(' ', '0');
            uuid1 = uuid1.substring(0, 32);
            uuid1 = uuid1.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            uuid = uuid + "/" + uuid1;    
            }
            
            if (simID2 != "") {
            uuid2 = androidID + deviceID + simID2;
            uuid2 = String.format("%32s", uuid2).replace(' ', '0');
            uuid2 = uuid2.substring(0, 32);
            uuid2 = uuid2.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            uuid = uuid + "/" + uuid2;    
            }
 
            
            Log.d("UniqueDeviceID",uuid);

            this.callbackContext.success(uuid);
        }catch(Exception e ) {
            this.callbackContext.error("Exception occurred: ".concat(e.getMessage()));
        }
    }

    private boolean hasPermission(String permission) throws Exception{
        boolean hasPermission = true;
        Method method = null;
        try {
            method = cordova.getClass().getMethod("hasPermission", permission.getClass());
            Boolean bool = (Boolean) method.invoke(cordova, permission);
            hasPermission = bool.booleanValue();
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "Cordova v" + CordovaWebView.CORDOVA_VERSION + " does not support API 23 runtime permissions so defaulting to GRANTED for " + permission);
        }
        return hasPermission;
    }

    private void requestPermission(CordovaPlugin plugin, int requestCode, String permission) throws Exception{
        try {
            java.lang.reflect.Method method = cordova.getClass().getMethod("requestPermission", org.apache.cordova.CordovaPlugin.class ,int.class, java.lang.String.class);
            method.invoke(cordova, plugin, requestCode, permission);
        } catch (NoSuchMethodException e) {
            throw new Exception("requestPermission() method not found in CordovaInterface implementation of Cordova v" + CordovaWebView.CORDOVA_VERSION);
        }
    }
}
