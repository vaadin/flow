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
'use strict';
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const bodyParser = require("body-parser");
const cleankill = require("cleankill");
const express = require("express");
const http = require("http");
const multer = require("multer");
const serverDestroy = require("server-destroy");
const port_scanner_1 = require("./port-scanner");
var express_1 = require("express");
exports.Router = express_1.Router;
exports.httpbin = express.Router();
function capWords(s) {
    return s.split('-')
        .map((word) => word[0].toUpperCase() + word.slice(1))
        .join('-');
}
function formatRequest(req) {
    const headers = {};
    for (const key in req.headers) {
        headers[capWords(key)] = req.headers[key];
    }
    const formatted = {
        headers: headers,
        url: req.originalUrl,
        data: req.body,
        files: req.files,
        form: {},
        json: {},
    };
    const contentType = (headers['Content-Type'] || '').toLowerCase().split(';')[0];
    const field = {
        'application/json': 'json',
        'application/x-www-form-urlencoded': 'form',
        'multipart/form-data': 'form'
    }[contentType];
    if (field) {
        formatted[field] = req.body;
    }
    return formatted;
}
exports.httpbin.use(bodyParser.urlencoded({ extended: false }));
exports.httpbin.use(bodyParser.json());
const storage = multer.memoryStorage();
const upload = multer({ storage: storage });
exports.httpbin.use(upload.any());
exports.httpbin.use(bodyParser.text());
exports.httpbin.use(bodyParser.text({ type: 'html' }));
exports.httpbin.use(bodyParser.text({ type: 'xml' }));
exports.httpbin.get('/delay/:seconds', function (req, res) {
    setTimeout(function () {
        res.json(formatRequest(req));
    }, (req.params.seconds || 0) * 1000);
});
exports.httpbin.post('/post', function (req, res) {
    res.json(formatRequest(req));
});
// Running this script directly with `node httpbin.js` will start up a server
// that just serves out /httpbin/...
// Useful for debugging only the httpbin functionality without the rest of
// wct.
function main() {
    return __awaiter(this, void 0, void 0, function* () {
        const app = express();
        const server = http.createServer(app);
        app.use('/httpbin', exports.httpbin);
        const port = yield port_scanner_1.findPort([7777, 7000, 8000, 8080, 8888]);
        server.listen(port);
        server.port = port;
        serverDestroy(server);
        cleankill.onInterrupt(() => {
            return new Promise((resolve) => {
                server.destroy();
                server.on('close', resolve);
            });
        });
        console.log('Server running at http://localhost:' + port + '/httpbin/');
    });
}
if (require.main === module) {
    main().catch((err) => {
        console.error(err);
        process.exit(1);
    });
}
