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

import * as net from 'net';

function checkPort(port: number): Promise<boolean> {
  return new Promise<boolean>(function(resolve) {
    const server = net.createServer();
    let hasPort = false;

    // if server is listening, we have the port!
    server.on('listening', function(_err: any) {
      hasPort = true;
      server.close();
    });

    // callback on server close to free up the port before report it can be used
    server.on('close', function(_err: any) {
      resolve(hasPort);
    });

    // our port is busy, ignore it
    server.on('error', function(_err: any) {
      // docs say the server should close, this doesn't seem to be the case :(
      server.close();
    });

    server.listen(port);
  });
}

interface PromiseGetter<T> {
  (val: T): Promise<boolean>;
}

async function detectSeries<T>(
    values: T[], promiseGetter: PromiseGetter<T>): Promise<T> {
  for (const value of values) {
    if (await promiseGetter(value)) {
      return value;
    }
  }
  throw new Error('Couldn\'t find a good value in detectSeries');
}

export async function findPort(ports: number[]): Promise<number> {
  try {
    return await detectSeries(ports, checkPort);
  } catch (error) {
    throw new Error('no port found!');
  }
}
