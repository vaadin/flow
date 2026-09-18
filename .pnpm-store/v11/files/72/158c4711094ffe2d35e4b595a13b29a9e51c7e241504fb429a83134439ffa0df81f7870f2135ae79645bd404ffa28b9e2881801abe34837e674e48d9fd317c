"use strict";
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
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
Object.defineProperty(exports, "__esModule", { value: true });
function transformResponse(transformer) {
    return (req, res, next) => {
        let ended = false;
        const chunks = [];
        let _shouldTransform = null;
        // Note: this function memoizes its result.
        function shouldTransform() {
            if (_shouldTransform == null) {
                const successful = res.statusCode >= 200 && res.statusCode < 300;
                _shouldTransform =
                    successful && !!transformer.shouldTransform(req, res);
            }
            return _shouldTransform;
        }
        // Cast is required because this method is overloaded, but assigning a
        // method to a variable seems to pick only one of the overload signatures.
        const _write = res.write;
        res.write = function (chunk, cbOrEncoding, cb) {
            if (ended) {
                _write.call(this, chunk, cbOrEncoding, cb);
                return false;
            }
            if (shouldTransform()) {
                const buffer = (typeof chunk === 'string') ?
                    Buffer.from(chunk, cbOrEncoding) :
                    chunk;
                chunks.push(buffer);
                return true;
            }
            else {
                return _write.call(this, chunk, cbOrEncoding, cb);
            }
        };
        // Cast is required because this method is overloaded, but assigning a
        // method to a variable seems to pick only one of the overload signatures.
        const _end = res.end;
        res.end = function (cbOrChunk, cbOrEncoding, cb) {
            if (ended) {
                return;
            }
            ended = true;
            if (shouldTransform()) {
                if (Buffer.isBuffer(cbOrChunk)) {
                    chunks.push(cbOrChunk);
                }
                else if (typeof cbOrChunk === 'string') {
                    chunks.push(Buffer.from(cbOrChunk, cbOrEncoding));
                }
                const body = Buffer.concat(chunks).toString('utf8');
                let newBody = body;
                try {
                    newBody = transformer.transform(req, res, body);
                }
                catch (e) {
                    console.warn('Error', e);
                }
                // TODO(justinfagnani): re-enable setting of content-length when we know
                // why it was causing truncated files. Could be multi-byte characters.
                // Assumes single-byte code points!
                // res.setHeader('Content-Length', `${newBody.length}`);
                res.removeHeader('Content-Length');
                // TODO(aomarks) Shouldn't we call the callbacks?
                return _end.call(this, newBody);
            }
            else {
                return _end.call(this, cbOrChunk, cbOrEncoding, cb);
            }
        };
        next();
    };
}
exports.transformResponse = transformResponse;
//# sourceMappingURL=transform-middleware.js.map