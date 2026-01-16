/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.webpush;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;

import com.interaso.webpush.VapidKeys;

public class WebPushVapidUtil {
    /**
     * A robust replacement for VapidKeys.fromUncompressedBytes. It ensures
     * private keys are interpreted as unsigned positive integers, avoiding the
     * "must be within range [1, n-1]" error caused by sign-extension.
     */
    public static VapidKeys loadVapidKeys(String publicKeyBase64,
            String privateKeyBase64) {
        byte[] publicBytes = Base64.getUrlDecoder().decode(publicKeyBase64);
        byte[] privateBytes = Base64.getUrlDecoder().decode(privateKeyBase64);
        try {
            // 1. Setup EC Parameters for P-256 (secp256r1)
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new java.security.spec.ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecSpec = params
                    .getParameterSpec(ECParameterSpec.class);

            // 2. Parse Private Key - The CRITICAL FIX: Use signum=1 to force
            // positive
            BigInteger s = new BigInteger(1, privateBytes);
            ECPrivateKeySpec privSpec = new ECPrivateKeySpec(s, ecSpec);

            // 3. Parse Public Key (Uncompressed format: 0x04 + X + Y)
            if (publicBytes[0] != 0x04 || publicBytes.length != 65) {
                throw new IllegalArgumentException(
                        "Invalid uncompressed public key format");
            }
            byte[] xBytes = new byte[32];
            byte[] yBytes = new byte[32];
            System.arraycopy(publicBytes, 1, xBytes, 0, 32);
            System.arraycopy(publicBytes, 33, yBytes, 0, 32);

            ECPoint w = new ECPoint(new BigInteger(1, xBytes),
                    new BigInteger(1, yBytes));
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(w, ecSpec);

            // 4. Generate Key Objects
            KeyFactory kf = KeyFactory.getInstance("EC");
            ECPublicKey pubKey = (ECPublicKey) kf.generatePublic(pubSpec);
            ECPrivateKey privKey = (ECPrivateKey) kf.generatePrivate(privSpec);

            return new VapidKeys(pubKey, privKey);
        } catch (Exception e) {
            throw new WebPushException(e.getMessage(), e);
        }
    }
}
