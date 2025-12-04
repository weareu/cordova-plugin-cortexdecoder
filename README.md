# Cordova Plugin CortexDecoder

Follows the [Cordova Plugin spec](https://cordova.apache.org/docs/en/latest/plugin_ref/spec.html), so that it works with [Plugman](https://github.com/apache/cordova-plugman).

## About

**Android:** CortexDecoder SDK v4.9.0 - 16KB page size aligned for Android compatibility.
**iOS:** CortexDecoder SDK v4.14.0

Supports both Android and iOS platforms with a unified JavaScript interface.

## Usage

### Basic Scan

```javascript
cordova.plugins.cortexDecoder.scan(options, successCallback, errorCallback);
```

### Options

- `customerID` (string): Your CortexDecoder customer ID
- `licenseKey` (string): Your CortexDecoder license key
- `cameraNumber` (integer): Camera index (0, 1, 2, etc.) - takes precedence over `cameraPosition`
- `cameraPosition` (string): "front" or "back" - legacy option
- `encodingCharsetName` (string): Character encoding (default: "ISO-8859-1")
- `decoderTimeLimit` (integer): Decoder time limit in milliseconds
- `numberOfBarcodesToDecode` (integer): Number of barcodes to decode (default: 1)
- `exactlyNBarcodes` (boolean): Decode exactly N barcodes
- `beepOnScanEnabled` (boolean): Enable beep on scan (default: true)
- `scanMultiple` (boolean): Enable multiple barcode scanning
- `dpmEnabled` (boolean): Enable DPM (Direct Part Marking) mode
- `inputBuffering` (boolean): Enable input buffering
- `inputBufferingItemCount` (integer): Number of items to buffer

### Camera Selection

The `cameraNumber` option allows you to specify any camera by its index:
- 0: Usually the back camera
- 1: Usually the front camera
- 2+: Additional cameras (e.g., ultra-wide, telephoto) on supported devices

If `cameraNumber` is specified, it takes precedence over `cameraPosition`.

### Example

```javascript
var options = {
  customerID: "YOUR_CUSTOMER_ID",
  licenseKey: "YOUR_LICENSE_KEY",
  cameraNumber: 2, // Use the 3rd camera (ultra-wide on some devices)
  numberOfBarcodesToDecode: 1,
  beepOnScanEnabled: true
};

cordova.plugins.cortexDecoder.scan(options,
  function(results) {
    // results is an array of barcode objects
    results.forEach(function(barcode) {
      console.log("Data:", barcode.barcodeData);
      console.log("Type:", barcode.symbologyName);
    });
  },
  function(error) {
    console.error("Scan failed:", error);
  }
);
```

## Licence

The MIT License

Copyright (c) 2010 Matt Kane

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
