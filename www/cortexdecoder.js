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
    var initInProgress = false;
}

CortexDecoder.prototype.init = function(options, successCallback, errorCallback) {
    errorCallback = errorCallback || function () {};

    if (typeof errorCallback !== 'function') {
        console.error('CortexDecoder.init failure: failure parameter not a function');
        return;
    }
    if (typeof successCallback !== 'function') {
        console.error('CortexDecoder.init failure: success callback parameter must be a function');
        return;
    }
    if (this.initInProgress) {
        errorCallback('CortexDecoder.init is already in progress');
        return;
    }
    this.initInProgress = true;

    exec(
        function(result) {
            this.initInProgress = false;
            successCallback(result);
        },
        function(error) {
            this.initInProgress = false;
            errorCallback(error);
        },
        'CortexDecoder',
        'init',
        options,
    );
};



var cortexDecoder = new CortexDecoder();
module.exports = cortexDecoder;
