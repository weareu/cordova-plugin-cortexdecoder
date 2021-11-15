package com.cordova.plugins.cortexdecoder.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import capacitor.android.plugins.R;


public class ScannerActivity extends Activity implements CortexDecoderLibraryCallback {
  public static final String TAG = ScannerActivity.class.getSimpleName();

  private RelativeLayout mCameraFrame;
  private View mCameraPreview;
  private CortexDecoderLibrary mCortexDecoderLibrary;
  private DisplayMetrics mDisplayMetrics;
  private Handler mMainHandler;



  private ArrayList<BarcodeFinderView> bfArr = new ArrayList<BarcodeFinderView>();
  private boolean continuousScanMode = false;
  private int continuousScanCount = 0;
  private boolean isScanning = false;
  private boolean enableVerificationMode = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.scanner_activity);

    Intent intent = getIntent();
    String customerID = intent.getStringExtra("customerID");
    String licenseKey = intent.getStringExtra("licenseKey");

    Context context = getApplicationContext();
    mCortexDecoderLibrary = CortexDecoderLibrary.sharedObject(context, "");
    mCortexDecoderLibrary.setCallback(this);

    mCameraPreview = mCortexDecoderLibrary.getCameraPreview();
    mCameraFrame = findViewById(R.id.cortex_scanner_view);

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
            Toast.makeText(getApplicationContext(), "License Valid", Toast.LENGTH_SHORT).show();
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

    mCortexDecoderLibrary.setEDKCustomerID(customerID);
    mCortexDecoderLibrary.activateLicense(licenseKey);

    //Tablets more than likely are going to have a screen dp >= 600
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

    CortexDecoderLibrary.CD_VerificationType verificationType = mCortexDecoderLibrary.getCurrentVerificationType();
    if(verificationType == CortexDecoderLibrary.CD_VerificationType.CD_Verification_AIMDPM || verificationType == CortexDecoderLibrary.CD_VerificationType.CD_Verification_ISO15415){
      enableVerificationMode = true;
    }else if(verificationType == CortexDecoderLibrary.CD_VerificationType.CD_Verification_None){
      enableVerificationMode = false;
    }
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
    debug(TAG, "receivedDecodedData()");
    final String symString = CortexDecoderLibrary.stringFromSymbologyType(type);

    List<int[]> cornersList = mCortexDecoderLibrary.getBarcodeCornersArray();
    if(cornersList != null){
      Log.e(TAG, "getBarcodeCornersArray length:"+ cornersList.size());
    }

    if (!continuousScanMode) {
      continuousScanCount = 0;
      updateBarcodeData(data, type, true);
    } else {
      updateBarcodeData(data, type, false);
      continuousScanCount++;
      displayContinuousCount();

      if (isScanning)
        mCortexDecoderLibrary.startDecoding();
      else
        startScanning();
    }
  }

  @Override
  public void receivedMultipleDecodedData(String[] data, SymbologyType[] types) {
    List<int[]> cornersList = mCortexDecoderLibrary.getBarcodeCornersArray();
    if(cornersList != null) {
      Log.e(TAG, "getBarcodeCornersArray length:" + cornersList.size());
    }

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (!continuousScanMode) {
          continuousScanCount = 0;
          mCortexDecoderLibrary.stopDecoding();
          mCortexDecoderLibrary.stopCameraPreview();
          stopScanning(false);
        } else {
          stopScanning(true);
          continuousScanCount++;
          displayContinuousCount();

          if (isScanning) {
            mCortexDecoderLibrary.startDecoding();
          } else {
            startScanning();
          }
        }
      }
    });
  }

  @Override
  public void receiveBarcodeCorners(final int[] corners) {
    mMainHandler.post(new Runnable() {
      @Override
      public void run() {
        createTargetLocator(corners);
      }
    });
  }

  @Override
  public void receiveMultipleBarcodeCorners(final List<int[]> cornersList) {
    mMainHandler.post(new Runnable() {
      @Override
      public void run() {
        for(int[] corners : cornersList) {
          createTargetLocator(corners);
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
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
      }
    });

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

  private void updateBarcodeData(final String data, SymbologyType type, final boolean stop) {
    final String symString = CortexDecoderLibrary.stringFromSymbologyType(type);
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, data);



        if (stop) {
          stopScanning(false);

          Intent intent = new Intent();
          intent.putExtra("barcodeData", data);
          intent.putExtra("symbologyName", symString);
          setResult(RESULT_OK, intent);
          finish();
        }
      }
    });
  }

  void stopScanning(boolean launchingIntent) {
    debug(TAG, "stopScanning()");

    mCortexDecoderLibrary.stopDecoding();
    mCameraFrame.setOnClickListener(tapListener);
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
  private void createTargetLocator(int[] corners) {
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

      final BarcodeFinderView bf = new BarcodeFinderView(this, corners, pWidth, pHeight, screenDiff, prh, prw);
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

      final BarcodeFinderView bf = new BarcodeFinderView(this, corners, pWidth, pHeight, screenDiff, prh, prw);
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

  private String formatExpireDate(Date date){
    if(date != null){
      Calendar c = Calendar.getInstance();
      c.setTime(date);

      return ""+c.get(Calendar.YEAR)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.DAY_OF_MONTH);
    }else{
      return "";
    }
  }
}
