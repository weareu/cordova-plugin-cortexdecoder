# Change Log

## v2.0.0

- **iOS Support Added!** Now supports both Android and iOS platforms
  - iOS SDK v4.14.0 integration
  - Native Swift implementation with full feature parity
  - Unified JavaScript interface across platforms

- **Android Updates:**
  - Updated to CortexDecoder SDK v4.9.0
  - Added 16KB page size alignment support for broader Android device compatibility
  - Major API changes to match SDK v4.x:
    - Updated to new singleton pattern (CDCamera.shared, CDDecoder.shared, CDLicense.shared)
    - Updated to new listener-based callbacks
    - Removed deprecated CortexDecoderLibrary wrapper class
    - Updated method signatures for compatibility
  - Updated all imports to match new SDK package structure
  - Fixed lifecycle management for camera and decoder

- **Cross-Platform Features:**
  - Added `cameraNumber` option for selecting specific camera by index (0, 1, 2, etc.)
  - Enhanced camera selection with support for multiple cameras (ultra-wide, telephoto, etc.)
  - `cameraNumber` takes precedence over legacy `cameraPosition` (front/back)
  - Consistent API across Android and iOS

## v1.0.0

- Initial Version
