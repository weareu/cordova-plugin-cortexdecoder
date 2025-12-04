import Foundation
import CortexDecoder

@objc(CortexDecoder)
class CortexDecoder: CDVPlugin {

    var scanCallbackId: String?

    @objc(scan:)
    func scan(command: CDVInvokedUrlCommand) {
        self.scanCallbackId = command.callbackId

        let options = command.arguments.first as? [String: Any] ?? [:]

        DispatchQueue.main.async {
            let scannerVC = CortexScannerViewController()
            scannerVC.modalPresentationStyle = .fullScreen
            scannerVC.options = options
            scannerVC.resultCallback = { [weak self] results in
                guard let self = self else { return }

                if let callbackId = self.scanCallbackId {
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: results)
                    self.commandDelegate?.send(pluginResult, callbackId: callbackId)
                }

                self.viewController?.dismiss(animated: true, completion: nil)
            }

            scannerVC.errorCallback = { [weak self] error in
                guard let self = self else { return }

                if let callbackId = self.scanCallbackId {
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error)
                    self.commandDelegate?.send(pluginResult, callbackId: callbackId)
                }

                self.viewController?.dismiss(animated: true, completion: nil)
            }

            self.viewController?.present(scannerVC, animated: true, completion: nil)
        }
    }
}
