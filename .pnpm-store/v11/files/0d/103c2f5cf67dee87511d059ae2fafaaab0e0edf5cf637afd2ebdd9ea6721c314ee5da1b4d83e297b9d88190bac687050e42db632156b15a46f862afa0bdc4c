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

import * as fs from 'mz/fs';
import * as pem from 'pem';

export interface KeyAndCert {
  key: string;
  cert: string;
}

/**
 * Gets the current TLS certificate (from current directory)
 * or generates one if needed
 * @param {string} keyPath path to TLS service key
 * @param {string} certPath path to TLS certificate
 * @returns {Promise<{}>} Promise of {key: string, cert: string}
 */
export async function getTLSCertificate(
    keyPath: string, certPath: string): Promise<KeyAndCert> {
  let certObj = await _readKeyAndCert(keyPath, certPath);

  if (!certObj) {
    certObj = await createTLSCertificate();

    if (keyPath && certPath) {
      await Promise.all([
        fs.writeFile(certPath, certObj.cert),
        fs.writeFile(keyPath, certObj.key)
      ]);
    }
  }

  return certObj;
}

async function _readKeyAndCert(
    keyPath: string, certPath: string): Promise<KeyAndCert|undefined> {
  if (!keyPath || !certPath) {
    return;
  }

  try {
    const results =
        (await Promise.all([fs.readFile(certPath), fs.readFile(keyPath)]))
            .map((buffer) => buffer.toString().trim());
    const cert = results[0];
    const key = results[1];
    if (key && cert) {
      return {cert, key};
    }
  } catch (err) {
    // If the cert/key file doesn't exist, generate new TLS certificate
    if (err.code !== 'ENOENT') {
      throw new Error(`cannot read certificate ${err}`);
    }
  }
}

/**
 * Generates a TLS certificate for HTTPS
 * @returns {Promise<{}>} Promise of {key: string, cert: string}
 */
async function createTLSCertificate(): Promise<KeyAndCert> {
  type PemCertificate = {certificate: string, serviceKey: string};
  const keys = await new Promise<PemCertificate>((resolve, reject) => {
    pem.createCertificate(
        {days: 365, selfSigned: true}, (err: {}, keys: PemCertificate) => {
          if (err) {
            reject(err);
          } else {
            resolve(keys);
          }
        });
  });

  return {
    cert: keys.certificate,
    key: keys.serviceKey,
  };
}
