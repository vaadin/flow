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

import * as bodyParser from 'body-parser';
import * as cleankill from 'cleankill';
import * as express from 'express';
import * as http from 'http';
import * as multer from 'multer';
import * as serverDestroy from 'server-destroy';

import {findPort} from './port-scanner';
import {Router} from 'express';
export {Router} from 'express';

export const httpbin: Router = express.Router();

function capWords(s: string) {
  return s.split('-')
      .map((word) => word[0].toUpperCase() + word.slice(1))
      .join('-');
}

function formatRequest(req: express.Request) {
  const headers = {};
  for (const key in req.headers) {
    headers[capWords(key)] = req.headers[key];
  }
  const formatted = {
    headers: headers,
    url: req.originalUrl,
    data: req.body,
    files: (<any>req).files,
    form: {},
    json: {},
  };
  const contentType =
      (headers['Content-Type'] || '').toLowerCase().split(';')[0];
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

httpbin.use(bodyParser.urlencoded({extended: false}));
httpbin.use(bodyParser.json());
const storage = multer.memoryStorage();
const upload = multer({storage: storage});
httpbin.use(upload.any());
httpbin.use(bodyParser.text());
httpbin.use(bodyParser.text({type: 'html'}));
httpbin.use(bodyParser.text({type: 'xml'}));

httpbin.get('/delay/:seconds', function(req, res) {
  setTimeout(function() {
    res.json(formatRequest(req));
  }, (req.params.seconds || 0) * 1000);
});

httpbin.post('/post', function(req, res) {
  res.json(formatRequest(req));
});

// Running this script directly with `node httpbin.js` will start up a server
// that just serves out /httpbin/...
// Useful for debugging only the httpbin functionality without the rest of
// wct.
async function main() {
  const app = express();
  const server = http.createServer(app) as serverDestroy.DestroyableServer;

  app.use('/httpbin', httpbin);


  const port = await findPort([7777, 7000, 8000, 8080, 8888]);

  server.listen(port);
  (<any>server).port = port;
  serverDestroy(server);
  cleankill.onInterrupt(() => {
    return new Promise((resolve) => {
      server.destroy();
      server.on('close', resolve);
    });
  });

  console.log('Server running at http://localhost:' + port + '/httpbin/');
}

if (require.main === module) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
