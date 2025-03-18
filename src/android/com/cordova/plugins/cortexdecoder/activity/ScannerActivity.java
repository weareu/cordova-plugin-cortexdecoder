package com.cordova.plugins.cortexdecoder.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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

import com.codecorp.decoder.CortexDecoderLibrary;
import com.codecorp.decoder.CortexDecoderLibraryCallback;
import com.codecorp.licensing.LicenseCallback;
import com.codecorp.licensing.LicenseStatusCode;
import com.codecorp.symbology.SymbologyType;
import com.codecorp.util.Codewords;
import com.cordova.plugins.cortexdecoder.view.BarcodeFinderView;

import static com.codecorp.internal.Debug.debug;
import static com.codecorp.internal.Debug.verbose;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import se.mobilelogic.logiccenter.mobile.R;


public class ScannerActivity extends Activity implements CortexDecoderLibraryCallback {
  public static final String TAG = ScannerActivity.class.getSimpleName();
  private static boolean sTorchState = false;

  private RelativeLayout mCameraFrame;
  private View mCameraPreview;
  private CortexDecoderLibrary mCortexDecoderLibrary;
  private DisplayMetrics mDisplayMetrics;
  private boolean mInputBuffering;
  private int mInputBufferingItemCount;
  private long mLastFrameTick;
  private long mLastResultTick;
  private Handler mMainHandler;
  private HashMap<String, JSONObject> mResultsMap;
  private Timer mTimer;
  private TimerTask mTimerTask;

  private ArrayList<BarcodeFinderView> bfArr = new ArrayList<BarcodeFinderView>();
  private int decodeForXMs = 250;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(android.R.layout.scanner_activity);

    Context context = getApplicationContext();
    mCortexDecoderLibrary = CortexDecoderLibrary.sharedObject(context, "");
    mCortexDecoderLibrary.setCallback(this);

    mCameraPreview = mCortexDecoderLibrary.getCameraPreview();
    mCameraFrame = findViewById(android.R.id.cortex_scanner_view);

    if (mCameraPreview.getParent() != null) ((RelativeLayout) mCameraPreview.getParent()).removeView(mCameraPreview);
    mCameraFrame.addView(mCameraPreview, 0);

    mMainHandler = new Handler(Looper.getMainLooper());

    mDisplayMetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

    mCortexDecoderLibrary.setLicenseCallback(new LicenseCallback() {
      @Override
      public void onActivationResult(LicenseStatusCode statusCode) {
        Log.d(TAG, "onActivationResult:" + statusCode);
        switch (statusCode) {
          case LicenseStatus_LicenseValid:
            // Toast.makeText(getApplicationContext(), "License Valid", Toast.LENGTH_SHORT).show();
            break;
          case LicenseStatus_LicenseExpired:
            Date date = mCortexDecoderLibrary.getLicenseExpirationDate();
            Toast.makeText(getApplicationContext(), "License Expired: "+formatExpireDate(date), Toast.LENGTH_LONG).show();
            break;
          default:
            Toast.makeText(getApplicationContext(), "License Invalid", Toast.LENGTH_SHORT).show();
            break;
        }
      }

      @Override
      public void onDeviceIDResult(int resultCode, String data) {

      }
    });

    Intent intent = getIntent();
    mInputBuffering = intent.getBooleanExtra("inputBuffering", false);
    mInputBufferingItemCount = intent.getIntExtra("inputBufferingItemCount", 0);

    String customerID = intent.getStringExtra("customerID");
    String licenseKey = intent.getStringExtra("licenseKey");
    int decoderTimeLimit = intent.getIntExtra("decoderTimeLimit", 0);
    int numberOfBarcodesToDecode = intent.getIntExtra("numberOfBarcodesToDecode", 1);
    boolean exactlyNBarcodes = intent.getBooleanExtra("exactlyNBarcodes", false);

    mCortexDecoderLibrary.setEDKCustomerID(customerID);
    mCortexDecoderLibrary.activateLicense(licenseKey);

    mCortexDecoderLibrary.decoderTimeLimitInMilliseconds(decoderTimeLimit);
    mCortexDecoderLibrary.setNumberOfBarcodesToDecode(numberOfBarcodesToDecode);
    mCortexDecoderLibrary.setExactlyNBarcodes(exactlyNBarcodes);
    mCortexDecoderLibrary.setTorch(sTorchState);

    // Tablets more than likely are going to have a screen dp >= 600
    if (getResources().getConfiguration().smallestScreenWidthDp < 600) {
      // Lock phone form factor to portrait.
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
  }

  @Override
  protected void onStart() {
    debug(TAG, "onStart()");
    super.onStart();
  }

  @Override
  public void onResume() {
    debug(TAG, "onResume()");
    super.onResume();

    removeLocatorOverlays();

    //enable get codewords
    mCortexDecoderLibrary.enableCodewordsOutput(true);

    startScanningAndDecoding();
  }

  private void startScanningAndDecoding() {
    if (mCortexDecoderLibrary.isLicenseActivated()) {
      if (!mCortexDecoderLibrary.isLicenseExpired()) {
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

  @Override
  public void receivedDecodedData(final String data, final SymbologyType type) {
    receivedMultipleDecodedData(new String[]{ data }, new SymbologyType[]{ type });
  }

  @Override
  public void receivedMultipleDecodedData(String[] datas, SymbologyType[] types) {
    List<int[]> cornersList = mCortexDecoderLibrary.getBarcodeCornersArray();
    if (cornersList != null) {
      Log.e(TAG, "getBarcodeCornersArray length:" + cornersList.size());
    }

    long now = (new Date()).getTime();
    if (now - mLastFrameTick >= 20) {
      removeLocatorOverlays();
    }
    mLastFrameTick = now;

    Set<String> keys = mResultsMap.keySet();
    if (mInputBuffering && mInputBufferingItemCount > 0) {
      if (keys.size() == mInputBufferingItemCount) {
        stopDecodingAndReturn(mResultsMap);
      }
    } else if (mLastResultTick > 0 && now - mLastResultTick >= this.decodeForXMs) {
      if (mInputBuffering) stopDecodingAndReturn(mResultsMap);
      else if (keys.size() == 1) stopDecodingAndReturn(mResultsMap);
      else {
        mCortexDecoderLibrary.stopDecoding();
        mCameraFrame.setOnClickListener(tapListener);
        mCortexDecoderLibrary.stopCameraPreview();
      }
    }

    if (mLastResultTick == 0) {
      new android.os.Handler(Looper.getMainLooper()).postDelayed(
        new Runnable() {
          public void run() {
            receivedMultipleDecodedData(datas, types);
          }
        },
        this.decodeForXMs
      );
    }

    for (int i = 0; i < datas.length; i++) {
      if (!mResultsMap.containsKey(datas[i])) mLastResultTick = now;

      JSONObject result = new JSONObject();
      try {
        result.put("barcodeData", datas[i]);
        result.put("symbologyName", CortexDecoderLibrary.stringFromSymbologyType(types[i]));
        result.put("corners", cornersList.get(i));
      } catch (JSONException e) {
        Log.d(TAG, "This should never happen");
      }

      mResultsMap.put(datas[i], result);
    }

    // Draw the rectangles (this is called by the SDK but since we erase the rectangles in this method, we need to call it again)
    receiveMultipleBarcodeCorners(cornersList);
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
  public void receiveBarcodeCorners(final int[] corners) {
    ArrayList<int[]> cornersList = new ArrayList<int[]>();
    cornersList.add(corners);

    receiveMultipleBarcodeCorners(cornersList);
  }

  @Override
  public void receiveMultipleBarcodeCorners(final List<int[]> cornersList) {
    mMainHandler.post(new Runnable() {
      @Override
      public void run() {
        for(int[] corners : cornersList) {
          String barcodeData = "";
          for (String key: mResultsMap.keySet()) {
            JSONObject result = mResultsMap.get(key);
            try {
              if (((int[])result.get("corners"))[0] == corners[0]) {
                barcodeData = key;
                break;
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
          createTargetLocator(corners, barcodeData);
        }
      }
    });
  }

  @Override
  public void receivedDecodedCodewordsData(Codewords codewords) {
    if(codewords != null){
      codewords.getNumberOfCodewords();
      codewords.getNumberOfShortCodewordsBlocks();
      codewords.getNumberOfLongCodewordsBlocks();
      codewords.getNumberOfDataCodewords();
      codewords.getNumberOfErrorCodewords();
      codewords.getCodewordsBeforeErrorCorrection();
      codewords.getCodewordsAfterErrorCorrection();
    }
  }

  @Override
  public void barcodeDecodeFailed(boolean result) {

  }

  @Override
  public void multiFrameDecodeCount(int decodeCount) {
    displayMultiFrameCount(decodeCount);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    // Checks the orientation of the screen
  }

  void startScanning() {
    debug(TAG, "startScanning()");

    mLastFrameTick = 0;
    mLastResultTick = 0;
    mResultsMap = new HashMap<String, JSONObject>();
    mCameraFrame.setOnClickListener(null);
    // This can take a while if we need to open the camera, so post
    // it after the UI update.
    postStartCameraAndDecodeTask();
  }

  private void postStartCameraAndDecodeTask() {
    mMainHandler.post(new Runnable() {
      @Override
      public void run() {
        mCortexDecoderLibrary.startCameraPreview();
        mCortexDecoderLibrary.startDecoding();
      }
    });
  }

  void stopScanning() {
    debug(TAG, "stopScanning()");

    mCortexDecoderLibrary.stopDecoding();
    mCortexDecoderLibrary.stopCameraPreview();

    mCortexDecoderLibrary.closeCamera();
  }

  View.OnClickListener tapListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      debug(TAG, "onClick()");

      removeLocatorOverlays();

      startScanning();
    }
  };

  private void displayContinuousCount() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        /*mToast.setText("Continuous Scan Count: " + Integer.toString(continuousScanCount));
        mToast.show();*/
      }
    });
  }

  private void displayMultiFrameCount(final int count) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        /*mToast.setText("No of decoded barcodes: " + Integer.toString(count));
        mToast.show();*/
      }
    });
  }

  //We are doing conversions from camera preview display to the actual size of the preview on the screen. Find the correct ratios and passing it along to the BarcodeFinderViewClass.
  private void createTargetLocator(int[] corners, String barcodeData) {
    //We will have to take these points and draw them to the screen
    int pWidth = mCameraPreview.getWidth();
    int pHeight = mCameraPreview.getHeight();

    Display display = getWindowManager().getDefaultDisplay();
    final Point point = new Point();
    try {
      display.getSize(point);
    } catch (Exception ignore) {
      point.x = display.getWidth();
      point.y = display.getHeight();
    }
    int screenH = point.y;
    int _y = mCameraFrame.getChildAt(0).getMeasuredHeight();
    int mPreviewH;

    int diffY = screenH - _y;
    mPreviewH = (screenH - diffY);
    com.codecorp.util.Size sz = mCortexDecoderLibrary.getSizeForROI();

    int screenDiff = 0;
    if (pHeight > mPreviewH) {
      screenDiff = (int) ((pHeight - mPreviewH) * 0.5);
    }
    //This checks to see if we are in portrait mode and we do a conversion taking into account that the photo is internally always viewed in landscape with
    //The origin being at the top right of the screen
    if (pWidth <= pHeight) {
      float prh = (float) pWidth / sz.height;
      float prw = (float) pHeight / sz.width;

      final BarcodeFinderView bf = new BarcodeFinderView(this, corners, pWidth, pHeight, screenDiff, prh, prw, barcodeData);
      bf.setOnTouchListener(bfvTouchListener);
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          bf.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
          bfArr.add(bf);
          mCameraFrame.addView(bf);
        }
      });
    } else {
      float prw = (float) pWidth / sz.width;
      float prh = (float) pHeight / sz.height;

      final BarcodeFinderView bf = new BarcodeFinderView(this, corners, pWidth, pHeight, screenDiff, prh, prw, barcodeData);
      bf.setOnTouchListener(bfvTouchListener);
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          bf.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
          bfArr.add(bf);
          mCameraFrame.addView(bf);
        }
      });
    }
  }

  View.OnTouchListener bfvTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      debug(TAG, "onBFVTouch()");

      // save the X,Y coordinates
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        BarcodeFinderView bfv = (BarcodeFinderView) v;

        Point point = new Point();
        point.x = (int)event.getX();
        point.y = (int)event.getY();

        if (bfv.barcodeRegion.contains((int)point.x,(int) point.y)) {
          JSONObject barcode = mResultsMap.get(bfv.barcodeData);
          HashMap<String, JSONObject> barcodes = new HashMap<String, JSONObject>();
          barcodes.put(bfv.barcodeData, barcode);

          stopDecodingAndReturn(barcodes);
          return true;
        }
      }

      // let the touch event pass on to whoever needs it
      return false;
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
    mCortexDecoderLibrary.setTorch(sTorchState);
  }

}
