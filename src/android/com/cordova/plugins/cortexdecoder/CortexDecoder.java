/**
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 * Copyright (c) 2013, Maciej Nux Jaros
 */
package com.cordova.plugins.cortexdecoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.content.pm.PackageManager;

import com.cordova.plugins.cortexdecoder.activity.ScannerActivity;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;



/**
 * This calls out to the CortexDecoder SDK and returns the result.
 *
 * @sa https://github.com/apache/cordova-android/blob/master/framework/src/org/apache/cordova/CordovaPlugin.java
 */
public class CortexDecoder extends CordovaPlugin {
    private static final String SCAN = "scan";
    private static final int REQUEST_CODE = 0;
    private static final String TAG = CortexDecoder.class.getSimpleName();

    private String [] permissions = { Manifest.permission.CAMERA };

    private JSONArray requestArgs;
    private CallbackContext callbackContext;

    /**
     * Constructor.
     */
    public CortexDecoder() {
    }

    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     *     cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return                Whether the action was valid.
     *
     * @sa https://github.com/apache/cordova-android/blob/master/framework/src/org/apache/cordova/CordovaPlugin.java
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        this.requestArgs = args;

        if (action.equals(SCAN)) {
            //android permission auto add
            if(!hasPermisssion()) {
              requestPermissions(0);
            } else {
              scan(args);
            }
        } else {
            return false;
        }
        return true;
    }

    private void scan(final JSONArray args) {
      final CordovaPlugin that = this;

      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          Context context = cordova.getActivity().getApplicationContext();
          Intent intent = new Intent(context, ScannerActivity.class);

          if (args.length() > 0) {
            JSONObject obj;
            JSONArray names;
            String key;
            Object value;

            for (int i = 0; i < args.length(); i++) {
              try {
                obj = args.getJSONObject(i);
              } catch (JSONException e) {
                Log.i("CordovaLog", e.getLocalizedMessage());
                continue;
              }

              names = obj.names();
              for (int j = 0; j < names.length(); j++) {
                try {
                  key = names.getString(j);
                  value = obj.get(key);

                  if (value instanceof Integer) {
                    intent.putExtra(key, (Integer) value);
                  } else if (value instanceof String) {
                    intent.putExtra(key, (String) value);
                  }
                  else if (value instanceof Boolean) {
                      intent.putExtra(key, (Boolean) value);
                  }

                } catch (JSONException e) {
                  Log.i("CordovaLog", e.getLocalizedMessage());
                }
              }
            }
          }

          intent.setPackage(that.cordova.getActivity().getApplicationContext().getPackageName());
          that.cordova.startActivityForResult(that, intent, REQUEST_CODE);
        }
      });
    }

    /**
     * Called when the scanner intent completes.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                       allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE && this.callbackContext != null) {
            if (resultCode == Activity.RESULT_OK) {
              try {
                JSONArray jsonArray = new JSONArray(intent.getStringExtra("results"));
                this.callbackContext.success(jsonArray);
              } catch (JSONException e) {
                Log.d(TAG, "This should never happen");
              }
            } else if (resultCode == Activity.RESULT_CANCELED) {
              this.callbackContext.success(new JSONArray());
            } else {
              //this.error(new PluginResult(PluginResult.Status.ERROR), this.callback);
              this.callbackContext.error("Unexpected error");
            }
        }
    }

    /**
     * check application's permissions
     */
   public boolean hasPermisssion() {
       for(String p : permissions)
       {
           if(!PermissionHelper.hasPermission(this, p))
           {
               return false;
           }
       }
       return true;
   }

    /**
     * We override this so that we can access the permissions variable, which no longer exists in
     * the parent class, since we can't initialize it reliably in the constructor!
     *
     * @param requestCode The code to get request action
     */
   public void requestPermissions(int requestCode)
   {
       PermissionHelper.requestPermissions(this, requestCode, permissions);
   }

   /**
   * processes the result of permission request
   *
   * @param requestCode The code to get request action
   * @param permissions The collection of permissions
   * @param grantResults The result of grant
   */
  public void onRequestPermissionResult(int requestCode, String[] permissions,
                                         int[] grantResults) throws JSONException
   {
       PluginResult result;
       for (int r : grantResults) {
           if (r == PackageManager.PERMISSION_DENIED) {
               Log.d(TAG, "Permission Denied!");
               result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
               this.callbackContext.sendPluginResult(result);
               return;
           }
       }

       switch(requestCode)
       {
           case 0:
               scan(this.requestArgs);
               break;
       }
   }

    /**
     * This plugin launches an external Activity when the camera is opened, so we
     * need to implement the save/restore API in case the Activity gets killed
     * by the OS while it's in the background.
     */
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

}
