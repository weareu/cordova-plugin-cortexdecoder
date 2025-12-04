package com.cordova.plugins.cortexdecoder.activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.codecorp.CDCamera;
import com.codecorp.CDDecoder;
import com.codecorp.CDLicense;
import com.codecorp.CDResult;
import com.cordova.plugins.cortexdecoder.view.BarcodeFinderView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


public class ScannerActivity extends Activity {
  public static final String TAG = ScannerActivity.class.getSimpleName();

  private static String DEFAULT_ENCODING = "ISO-8859-1";

  private static boolean sTorchState = false;

  private RelativeLayout mCameraFrame;
  private View mCameraPreview;
  private DisplayMetrics mDisplayMetrics;
  private boolean mInputBuffering;
  private int mInputBufferingItemCount;
  private long mLastFrameTick;
  private long mLastResultTick;
  private Handler mMainHandler;
  private HashMap<String, JSONObject> mResultsMap;

  private ArrayList<BarcodeFinderView> bfArr = new ArrayList<BarcodeFinderView>();
  private int decodeForXMs = 250;
  private boolean mScanMultiple = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Application app = this.getApplication();
    String package_name = app.getPackageName();
    Resources resources = app.getResources();

    setContentView(resources.getIdentifier("scanner_activity", "layout", package_name));

    Context context = getApplicationContext();
    int scanner_activity = context.getResources()
            .getIdentifier("scanner_activity", "layout", context.getPackageName());

    setContentView(scanner_activity);

    mCameraPreview = CDCamera.shared.startPreview();
    mCameraFrame = findViewById(resources.getIdentifier("cortex_scanner_view", "id", package_name));

    if (mCameraPreview.getParent() != null) ((RelativeLayout) mCameraPreview.getParent()).removeView(mCameraPreview);
    mCameraFrame.addView(mCameraPreview, 0);

    mMainHandler = new Handler(Looper.getMainLooper());

    mDisplayMetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

    CDLicense.shared.setLicenseResultListener((statusCode) -> {
      Log.d(TAG, "onActivationResult:" + statusCode);
      switch (statusCode) {
        case licenseValid:
          // Toast.makeText(getApplicationContext(), "License Valid", Toast.LENGTH_SHORT).show();
          break;
        case licenseExpired:
          Date date = CDLicense.shared.getExpirationDate();
          Toast.makeText(getApplicationContext(), "License Expired: "+formatExpireDate(date), Toast.LENGTH_LONG).show();
          break;
        default:
          Toast.makeText(getApplicationContext(), "License Invalid", Toast.LENGTH_SHORT).show();
          break;
      }
    });

    Intent intent = getIntent();
    mInputBuffering = intent.getBooleanExtra("inputBuffering", false);
    mInputBufferingItemCount = intent.getIntExtra("inputBufferingItemCount", 0);

    String customerID = intent.getStringExtra("customerID");
    String licenseKey = intent.getStringExtra("licenseKey");

    String encoding = intent.getStringExtra("encodingCharsetName");

    int decoderTimeLimit = intent.getIntExtra("decoderTimeLimit", 0);
    int numberOfBarcodesToDecode = intent.getIntExtra("numberOfBarcodesToDecode", 1);
    boolean exactlyNBarcodes = intent.getBooleanExtra("exactlyNBarcodes", false);
    boolean beepOnScanEnabled = intent.getBooleanExtra("beepOnScanEnabled", true);

    mScanMultiple = intent.getBooleanExtra("scanMultiple", false);

    CDLicense.shared.setCustomerID(customerID);
    CDLicense.shared.activateLicense(licenseKey);

    CDDecoder.shared.setPreprocessType(CDDecoder.CDPreProcessType.lowPass2);
    CDDecoder.shared.setPreprocessType(CDDecoder.CDPreProcessType.deblur1dMethod1);

    CDDecoder.shared.setTimeLimit(decoderTimeLimit);
    CDDecoder.shared.setBarcodesToDecode(numberOfBarcodesToDecode, exactlyNBarcodes);

    CDCamera.CDTorch torchMode = sTorchState ? CDCamera.CDTorch.on : CDCamera.CDTorch.off;
    CDCamera.shared.setTorch(torchMode);

    if(mScanMultiple)
      CDDecoder.shared.setMultiFrameDecoding(true);

    if(encoding != null && !encoding.isEmpty()) {
      CDDecoder.shared.setEncodingCharsetName(encoding);
    }
    else {
      CDDecoder.shared.setEncodingCharsetName(DEFAULT_ENCODING);
    }

    // Enable beep
    if (beepOnScanEnabled) {
      // Beep is enabled by default in v4.0+
    }

    // DPM support
    boolean dpmEnabled = intent.getBooleanExtra("dpmEnabled", false);
    if(dpmEnabled) {
      // DPM configurations would go here if needed
    }

    // Handle cameraNumber parameter - takes precedence over cameraPosition
    if (intent.hasExtra("cameraNumber")) {
      int cameraNumber = intent.getIntExtra("cameraNumber", 0);
      try {
        CDCamera.shared.setCamera(cameraNumber);
        Log.d(TAG, "Set camera to number: " + cameraNumber);
      } catch (Exception e) {
        Log.e(TAG, "Failed to set camera number " + cameraNumber + ": " + e.getMessage());
      }
    } else {
      // Legacy camera position support (front/back)
      String cameraPosition = intent.getStringExtra("cameraPosition");
      if (cameraPosition != null) {
        try {
          if ("front".equalsIgnoreCase(cameraPosition)) {
            CDCamera.shared.setCameraPosition(CDCamera.CDPosition.front);
          } else if ("back".equalsIgnoreCase(cameraPosition)) {
            CDCamera.shared.setCameraPosition(CDCamera.CDPosition.back);
          }
        } catch (Exception e) {
          Log.e(TAG, "Failed to set camera position: " + e.getMessage());
        }
      }
    }

    // Tablets more than likely are going to have a screen dp >= 600
    if (getResources().getConfiguration().smallestScreenWidthDp < 600) {
      // Lock phone form factor to portrait.
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
  }

  @Override
  protected void onStart() {
    Log.d(TAG, "onStart()");
    super.onStart();
  }

  @Override
  public void onResume() {
    Log.d(TAG, "onResume()");
    super.onResume();

    removeLocatorOverlays();

    startScanningAndDecoding();
  }

  private void startScanningAndDecoding() {
    if (CDLicense.shared.isActivated()) {
      if (!CDLicense.shared.isExpired()) {
        startScanning();
      }
    }
  }

  private void removeLocatorOverlays() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        for (Iterator<BarcodeFinderView> iterator = bfArr.listIterator(); iterator.hasNext(); ) {
          BarcodeFinderView b = iterator.next();
          iterator.remove();
          mCameraFrame.removeView(b);
        }
      }
    });
  }

  public static String byteArrayToHexString(final byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for(byte b : bytes){
      sb.append(String.format("%02x", b&0xff));
    }
    return sb.toString().toUpperCase(Locale.ROOT);
  }

  private void onDecode(CDResult[] cdResults) {
    if (cdResults == null || cdResults.length == 0) {
      return;
    }

    long now = (new Date()).getTime();
    if (now - mLastFrameTick >= 20) {
      removeLocatorOverlays();
    }
    mLastFrameTick = now;

    if (mInputBuffering && mInputBufferingItemCount > 0) {
      if (mResultsMap.size() == mInputBufferingItemCount) {
        stopDecodingAndReturn(mResultsMap);
        return;
      }
    } else if (mLastResultTick > 0 && now - mLastResultTick >= this.decodeForXMs) {
      if (mInputBuffering) {
        stopDecodingAndReturn(mResultsMap);
        return;
      } else if (mResultsMap.size() == 1) {
        stopDecodingAndReturn(mResultsMap);
        return;
      } else {
        CDDecoder.shared.setDecoding(false);
        mCameraFrame.setOnClickListener(tapListener);
        CDCamera.shared.stopPreview();
        return;
      }
    }

    if (mLastResultTick == 0) {
      new android.os.Handler(Looper.getMainLooper()).postDelayed(
        new Runnable() {
          public void run() {
            onDecode(cdResults);
          }
        },
        this.decodeForXMs
      );
    }

    for (CDResult cdResult : cdResults) {
      if (cdResult.getStatus() != CDResult.CDDecodeStatus.success) {
        continue;
      }

      String data = cdResult.getBarcodeData();
      if (!mResultsMap.containsKey(data)) mLastResultTick = now;

      JSONObject result = new JSONObject();
      try {
        byte[] dataBytes = new byte[data.length()];
        char[] chars = new char[data.length()];
        data.getChars(0, data.length(), chars, 0);
        for (int si = 0; si < data.length(); si++) {
            dataBytes[si] = (byte)chars[si];
        }

        result.put("barcodeData", data);
        result.put("barcodeDataHEX", byteArrayToHexString(dataBytes));
        result.put("symbologyName", cdResult.getSymbology());

        // Get preview coordinates if available
        if (cdResult.getPreviewCoordinates() != null) {
          int[] coords = cdResult.getPreviewCoordinates().getCorners();
          result.put("corners", coords);
        }
      } catch (JSONException e) {
        Log.d(TAG, "Error creating result JSON: " + e.getMessage());
      }

      mResultsMap.put(data, result);
    }

    if(mScanMultiple) {
      stopDecodingAndReturn(mResultsMap);
    }
  }

  public void stopDecodingAndReturn(final HashMap<String, JSONObject> barcodes) {
    JSONArray jsonArray = new JSONArray();
    for (String key: barcodes.keySet()) {
      jsonArray.put(barcodes.get(key));
    }

    stopScanning();

    Intent intent = new Intent();
    intent.putExtra("results", jsonArray.toString());
    setResult(RESULT_OK, intent);
    finish();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  void startScanning() {
    Log.d(TAG, "startScanning()");

    mLastFrameTick = 0;
    mLastResultTick = 0;
    mResultsMap = new HashMap<String, JSONObject>();
    mCameraFrame.setOnClickListener(null);

    postStartCameraAndDecodeTask();
  }

  private void postStartCameraAndDecodeTask() {
    mMainHandler.post(new Runnable() {
      @Override
      public void run() {
        CDCamera.shared.setHighLightBarcodes(true);
        CDDecoder.shared.setDecoding(true);
        CDCamera.shared.setVideoCapturing(true);
        CDCamera.shared.startCamera(ScannerActivity.this::onDecode);
      }
    });
  }

  void stopScanning() {
    Log.d(TAG, "stopScanning()");

    CDDecoder.shared.setDecoding(false);
    CDCamera.shared.setVideoCapturing(false);
    CDCamera.shared.stopPreview();
    CDCamera.shared.stopCamera();
  }

  View.OnClickListener tapListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      Log.d(TAG, "onClick()");

      removeLocatorOverlays();

      startScanning();
    }
  };

  private String formatExpireDate(Date date){
    if(date != null){
      Calendar c = Calendar.getInstance();
      c.setTime(date);

      return ""+c.get(Calendar.YEAR)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.DAY_OF_MONTH);
    }else{
      return "";
    }
  }

  public void toggleTorch(View view) {
    sTorchState = !sTorchState;
    CDCamera.CDTorch torchMode = sTorchState ? CDCamera.CDTorch.on : CDCamera.CDTorch.off;
    CDCamera.shared.setTorch(torchMode);
  }

  @Override
  protected void onPause() {
    super.onPause();
    stopScanning();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    stopScanning();
  }
}
