/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow.server.communication;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import com.vaadin.pro.licensechecker.MachineId;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Representation of the security token exchanged with the Dev Tools client to
 * validate websocket connections.
 */
public class DevToolsToken {

    /**
     * Random token to ensure dev-tools websocket connections.
     */
    private static final String RANDOM_DEV_TOOLS_TOKEN = UUID.randomUUID()
            .toString();

    /**
     * Signed token, composed by the random token and its signature, joined by
     * one underscore character.
     */
    private static final String SIGNED_DEV_TOOLS_TOKEN = RANDOM_DEV_TOOLS_TOKEN
            + "_" + singValue(RANDOM_DEV_TOOLS_TOKEN);

    private static final Pattern TOKEN_DELIMITER = Pattern.compile("_");

    /**
     * Gets the signed token exchanged between Dev Tools client and server.
     *
     * @return signed token, never {@literal null}.
     */
    public static String token() {
        return SIGNED_DEV_TOOLS_TOKEN;
    }

    private static String singValue(String value) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            String machineId = MachineId.get();
            if (machineId.isEmpty()) {
                machineId = RANDOM_DEV_TOOLS_TOKEN;
            }
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    machineId.getBytes(UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(value.getBytes(UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            LoggerFactory.getLogger(DevToolsToken.class)
                    .debug("Cannot sign dev-tools token", ex);
        }
        return value;
    }

    /**
     * The result of dev-tools token validation.
     */
    public enum TokenValidation {
        /**
         * The client token is correctly signed and matches the server token.
         */
        OK,
        /**
         * The client token has a valid signature, but it does not match the
         * server token.
         */
        EXPIRED,
        /**
         * The token is not signed correctly.
         */
        INVALID
    }

    /**
     * Verifies the validity of the given token.
     *
     * The token may be valid ({@link TokenValidation#OK}, correctly signed but
     * expired because of a server restart ({@link TokenValidation#EXPIRED} or
     * invalid ({@link TokenValidation#INVALID}.
     *
     * @param token
     *            the token to validate
     * @return the validation result, never {@literal null}.
     */
    public static TokenValidation validateToken(String token) {
        if (token == null || token.isBlank()) {
            return TokenValidation.INVALID;
        }
        String[] parts = TOKEN_DELIMITER.split(token);
        if (parts.length == 2) {
            String clientToken = parts[0];
            String clientSignedValue = parts[1];
            String serverSignedValue = singValue(clientToken);
            if (clientSignedValue.equals(serverSignedValue)) {
                return RANDOM_DEV_TOOLS_TOKEN.equals(clientToken)
                        ? TokenValidation.OK
                        : TokenValidation.EXPIRED;
            }
        }
        return TokenValidation.INVALID;
    }

}
