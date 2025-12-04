import UIKit
import CortexDecoder

class CortexScannerViewController: UIViewController {

    var options: [String: Any] = [:]
    var resultCallback: (([[String: Any]]) -> Void)?
    var errorCallback: ((String) -> Void)?

    private var previewView = UIView()
    private var resultsMap: [String: [String: Any]] = [:]
    private var lastResultTime: Date?
    private let decodeForMs: Double = 250
    private var scanMultiple: Bool = false
    private var inputBuffering: Bool = false
    private var inputBufferingItemCount: Int = 0

    lazy var decoderModule: CDDecoder = {
        let decoder = CDDecoder.shared
        decoder.decodeResultProtocol = self
        return decoder
    }()

    lazy var cameraModule: CDCamera = {
        let camera = CDCamera.shared
        return camera
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .black
        setupUI()

        // Parse options
        let customerID = options["customerID"] as? String ?? ""
        let licenseKey = options["licenseKey"] as? String ?? ""
        let decoderTimeLimit = options["decoderTimeLimit"] as? Int ?? 0
        let numberOfBarcodesToDecode = options["numberOfBarcodesToDecode"] as? Int ?? 1
        let exactlyNBarcodes = options["exactlyNBarcodes"] as? Bool ?? false

        scanMultiple = options["scanMultiple"] as? Bool ?? false
        inputBuffering = options["inputBuffering"] as? Bool ?? false
        inputBufferingItemCount = options["inputBufferingItemCount"] as? Int ?? 0

        // Activate license
        let license = CDLicense.shared
        license.setCustomerID(customerID: customerID)
        license.activateLicense(key: licenseKey) { [weak self] result in
            if result.status == .activated {
                self?.configureScanSettings(
                    decoderTimeLimit: decoderTimeLimit,
                    numberOfBarcodesToDecode: numberOfBarcodesToDecode,
                    exactlyNBarcodes: exactlyNBarcodes
                )
            } else {
                self?.errorCallback?("License activation failed")
                self?.dismiss(animated: true, completion: nil)
            }
        }
    }

    private func setupUI() {
        // Add close button
        let closeButton = UIButton(type: .system)
        closeButton.setTitle("✕", for: .normal)
        closeButton.titleLabel?.font = .systemFont(ofSize: 32, weight: .light)
        closeButton.tintColor = .white
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        closeButton.addTarget(self, action: #selector(closeTapped), for: .touchUpInside)

        view.addSubview(closeButton)

        NSLayoutConstraint.activate([
            closeButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
            closeButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            closeButton.widthAnchor.constraint(equalToConstant: 44),
            closeButton.heightAnchor.constraint(equalToConstant: 44)
        ])
    }

    @objc private func closeTapped() {
        resultsMap.removeAll()
        errorCallback?("User cancelled scan")
    }

    private func configureScanSettings(decoderTimeLimit: Int, numberOfBarcodesToDecode: Int, exactlyNBarcodes: Bool) {
        // Configure decoder
        decoderModule.setPreprocessType(setting: .lowPass2)
        decoderModule.setPreprocessType(setting: .deblur1dMethod1)
        decoderModule.setTimeLimit(timeLimit: decoderTimeLimit)
        decoderModule.setBarcodesToDecode(number: numberOfBarcodesToDecode, decodeExactly: exactlyNBarcodes)

        if scanMultiple {
            decoderModule.setMultiFrameDecoding(enable: true)
        }

        // Configure camera
        cameraModule.setTorch(torch: .off)
        cameraModule.setHighlightBarcodes(enable: true)

        // Handle camera selection
        if let cameraNumber = options["cameraNumber"] as? Int {
            // Use camera number if specified
            switch cameraNumber {
            case 0:
                // Back camera with wide angle
                cameraModule.setCameraPosition(position: .back)
                cameraModule.setCamera(type: .wideAngle)
            case 1:
                // Front camera
                cameraModule.setCameraPosition(position: .front)
            case 2:
                // Ultra-wide camera
                cameraModule.setCameraPosition(position: .back)
                cameraModule.setCamera(type: .ultraWide)
            default:
                // Default to back camera
                cameraModule.setCameraPosition(position: .back)
            }
        } else if let cameraPosition = options["cameraPosition"] as? String {
            // Fall back to camera position
            if cameraPosition.lowercased() == "front" {
                cameraModule.setCameraPosition(position: .front)
            } else if cameraPosition.lowercased() == "back" {
                cameraModule.setCameraPosition(position: .back)
            }
        }

        // Start camera
        startPreview()
        decoderModule.setDecoding(enable: true)
        cameraModule.setVideoCapturing(value: true)
    }

    private func startPreview() {
        previewView.removeFromSuperview()

        let pFrame = view.bounds
        previewView = cameraModule.startPreview(frame: pFrame)
        view.addSubview(previewView)
        view.sendSubviewToBack(previewView)

        previewView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            previewView.leftAnchor.constraint(equalTo: view.leftAnchor),
            previewView.rightAnchor.constraint(equalTo: view.rightAnchor),
            previewView.topAnchor.constraint(equalTo: view.topAnchor),
            previewView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    private func stopPreview() {
        decoderModule.setDecoding(enable: false)
        cameraModule.setHighlightBarcodes(enable: false)
        cameraModule.setVideoCapturing(value: false)
        cameraModule.stopPreview()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        stopPreview()
    }

    private func byteArrayToHexString(data: String) -> String {
        return data.data(using: .isoLatin1)?
            .map { String(format: "%02X", $0) }
            .joined() ?? ""
    }

    private func stopDecodingAndReturn() {
        var results: [[String: Any]] = []
        for (_, value) in resultsMap {
            results.append(value)
        }

        stopPreview()
        resultCallback?(results)
    }
}

extension CortexScannerViewController: CDDecodeResultDelegate {
    func barcodeData(decodeResult: [CDResult]) {
        guard !decodeResult.isEmpty else { return }

        let now = Date()

        // Handle buffering logic
        if inputBuffering && inputBufferingItemCount > 0 {
            if resultsMap.count >= inputBufferingItemCount {
                stopDecodingAndReturn()
                return
            }
        } else if let lastTime = lastResultTime, now.timeIntervalSince(lastTime) >= (decodeForMs / 1000.0) {
            if inputBuffering || resultsMap.count == 1 {
                stopDecodingAndReturn()
                return
            }
        }

        if lastResultTime == nil {
            lastResultTime = now
            DispatchQueue.main.asyncAfter(deadline: .now() + decodeForMs / 1000.0) { [weak self] in
                self?.stopDecodingAndReturn()
            }
        }

        for cdResult in decodeResult {
            guard cdResult.status == .success else { continue }

            let data = cdResult.barcodeData
            guard !resultsMap.keys.contains(data) else { continue }

            lastResultTime = now

            var result: [String: Any] = [
                "barcodeData": data,
                "barcodeDataHEX": byteArrayToHexString(data: data),
                "symbologyName": cdResult.symbology
            ]

            resultsMap[data] = result
        }

        if scanMultiple {
            stopDecodingAndReturn()
        }
    }
}
