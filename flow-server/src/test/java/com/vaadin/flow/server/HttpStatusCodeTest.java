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
package com.vaadin.flow.server;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpStatusCodeTest {

    @Test
    public void isValidStatusCode_invalidCode_returnsFalse() {
        Set<Integer> validCodes = Stream.of(HttpStatusCode.values())
                .map(HttpStatusCode::getCode).collect(Collectors.toSet());

        IntStream.rangeClosed(-1000, 1000)
                .filter(sc -> !validCodes.contains(sc))
                .forEach(sc -> assertFalse(
                        HttpStatusCode.isValidStatusCode(sc),
                        sc + " should be invalid, but was not"));
    }

    @Test
    public void isValidStatusCode_validCode_returnsTrue() {
        Stream.of(HttpStatusCode.values()).mapToInt(HttpStatusCode::getCode)
                .forEach(sc -> assertTrue(
                        HttpStatusCode.isValidStatusCode(sc),
                        sc + " should be valid, but was not"));

    }

}
