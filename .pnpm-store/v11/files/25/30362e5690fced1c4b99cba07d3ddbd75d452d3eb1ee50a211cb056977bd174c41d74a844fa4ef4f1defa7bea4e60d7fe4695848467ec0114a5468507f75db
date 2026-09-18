"use strict";
/**
 * @license
 * Copyright (c) 2014 The Polymer Project Authors. All rights reserved.
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
const net = require("net");
function checkPort(port) {
    return new Promise(function (resolve) {
        const server = net.createServer();
        let hasPort = false;
        // if server is listening, we have the port!
        server.on('listening', function (_err) {
            hasPort = true;
            server.close();
        });
        // callback on server close to free up the port before report it can be used
        server.on('close', function (_err) {
            resolve(hasPort);
        });
        // our port is busy, ignore it
        server.on('error', function (_err) {
            // docs say the server should close, this doesn't seem to be the case :(
            server.close();
        });
        server.listen(port);
    });
}
function detectSeries(values, promiseGetter) {
    return __awaiter(this, void 0, void 0, function* () {
        for (const value of values) {
            if (yield promiseGetter(value)) {
                return value;
            }
        }
        throw new Error('Couldn\'t find a good value in detectSeries');
    });
}
function findPort(ports) {
    return __awaiter(this, void 0, void 0, function* () {
        try {
            return yield detectSeries(ports, checkPort);
        }
        catch (error) {
            throw new Error('no port found!');
        }
    });
}
exports.findPort = findPort;
