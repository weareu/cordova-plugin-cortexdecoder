function scan(success, error) {
    var code = window.prompt('Enter barcode value (empty value will fire the error handler):');
    if(code) {
        var result = {
            text: code,
            format: 'Fake',
            cancelled: false
        };
        success(result);
    } else {
        error('No barcode');
    }
}

module.exports = {
    scan: scan
};

require('cordova/exec/proxy').add('CortexDecoder',module.exports);