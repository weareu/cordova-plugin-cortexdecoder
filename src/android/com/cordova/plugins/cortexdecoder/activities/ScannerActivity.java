package com.cordova.plugins.cortexdecoder;

import android.app.Activity;
import android.os.Bundle;
import com.codecorp.camera.Focus;
import com.codecorp.cortex_scan.BuildConfig;
import com.codecorp.cortex_scan.MyApplication;
import com.codecorp.cortex_scan.R;
import com.codecorp.cortex_scan.util.PermissionUtils;
import com.codecorp.cortex_scan.util.RegionOfInterestRect;
import com.codecorp.cortex_scan.view.BarcodeFinderView;
import com.codecorp.cortex_scan.view.CropFrameView;
import com.codecorp.cortex_scan.view.CrosshairsView;
import com.codecorp.cortex_scan.view.PicklistView;
import com.codecorp.decoder.CortexDecoderLibrary;
import com.codecorp.decoder.CortexDecoderLibraryCallback;
import com.codecorp.internal.Debug;
import com.codecorp.licensing.LicenseCallback;
import com.codecorp.licensing.LicenseStatusCode;
import com.codecorp.symbology.SymbologyType;
import com.codecorp.util.Codewords;

public class ScannerActivity extends Activity implements CortexDecoderLibraryCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String packageName = getApplication().getPackageName();
        setContentView(getApplication().getResources().getIdentifier("scanner_activity", "layout", packageName));
    }
}