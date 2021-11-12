/**
 * cordova is available under the MIT License (2008).
 * See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 * Copyright (c) 2012-2017, Adobe Systems
 */


var exec = cordova.require('cordova/exec');

/**
 * Constructor.
 *
 * @returns {CortexDecoder}
 */
function CortexDecoder() {
    var scanInProgress = false;
}

CortexDecoder.prototype.scan = function(options, successCallback, errorCallback) {
    errorCallback = errorCallback || function () {};

    if (typeof errorCallback !== 'function') {
        console.error('CortexDecoder.scan failure: failure parameter not a function');
        return;
    }
    if (typeof successCallback !== 'function') {
        console.error('CortexDecoder.scan failure: success callback parameter must be a function');
        return;
    }
    if (this.scanInProgress) {
        errorCallback('CortexDecoder.scan is already in progress');
        return;
    }
    this.scanInProgress = true;

    exec(
        function(result) {
            this.scanInProgress = false;
            successCallback(result);
        },
        function(error) {
            this.scanInProgress = false;
            errorCallback(error);
        },
        'CortexDecoder',
        'scan',
        [options],
    );
};



var cortexDecoder = new CortexDecoder();
module.exports = cortexDecoder;
