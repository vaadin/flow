"use strict";
/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("mz/fs");
const pem = require("pem");
/**
 * Gets the current TLS certificate (from current directory)
 * or generates one if needed
 * @param {string} keyPath path to TLS service key
 * @param {string} certPath path to TLS certificate
 * @returns {Promise<{}>} Promise of {key: string, cert: string}
 */
function getTLSCertificate(keyPath, certPath) {
    return __awaiter(this, void 0, void 0, function* () {
        let certObj = yield _readKeyAndCert(keyPath, certPath);
        if (!certObj) {
            certObj = yield createTLSCertificate();
            if (keyPath && certPath) {
                yield Promise.all([
                    fs.writeFile(certPath, certObj.cert),
                    fs.writeFile(keyPath, certObj.key)
                ]);
            }
        }
        return certObj;
    });
}
exports.getTLSCertificate = getTLSCertificate;
function _readKeyAndCert(keyPath, certPath) {
    return __awaiter(this, void 0, void 0, function* () {
        if (!keyPath || !certPath) {
            return;
        }
        try {
            const results = (yield Promise.all([fs.readFile(certPath), fs.readFile(keyPath)]))
                .map((buffer) => buffer.toString().trim());
            const cert = results[0];
            const key = results[1];
            if (key && cert) {
                return { cert, key };
            }
        }
        catch (err) {
            // If the cert/key file doesn't exist, generate new TLS certificate
            if (err.code !== 'ENOENT') {
                throw new Error(`cannot read certificate ${err}`);
            }
        }
    });
}
/**
 * Generates a TLS certificate for HTTPS
 * @returns {Promise<{}>} Promise of {key: string, cert: string}
 */
function createTLSCertificate() {
    return __awaiter(this, void 0, void 0, function* () {
        const keys = yield new Promise((resolve, reject) => {
            pem.createCertificate({ days: 365, selfSigned: true }, (err, keys) => {
                if (err) {
                    reject(err);
                }
                else {
                    resolve(keys);
                }
            });
        });
        return {
            cert: keys.certificate,
            key: keys.serviceKey,
        };
    });
}
//# sourceMappingURL=tls.js.map