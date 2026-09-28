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
package com.vaadin.flow.quarkus.it;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.test.AbstractChromeIT;
import com.vaadin.testbench.TestBenchElement;

@QuarkusIntegrationTest
public class AddonsIT extends AbstractChromeIT {

    @Override
    protected String getTestPath() {
        return "/addons";
    }

    @Test
    void addonWithoutJandexIndexedOnApplicationPropertiesShouldBeRendered() {
        open();
        TestBenchElement addon = waitUntil(driver -> $("hello-world").single());
        Assertions.assertTrue(addon.$("span").withId("without-jandex").exists(),
                "Expecting span element with id 'without-jandex' to be present in 'hello-world' shadow DOM, but was not");
        Assertions.assertTrue(
                addon.$("axa-input-text").id("npm-dep").$("input").single()
                        .hasClassName("a-input-text__input"),
                "Expecting axa-input-text element with id 'npm-dep' to be rendered in 'hello-world' shadow DOM, but was not");
    }

    @Test
    void addonWithJandexIndexedShouldBeRendered() {
        open();
        TestBenchElement addon = waitUntil(
                driver -> $("hello-world-jandex").single());
        Assertions.assertTrue(addon.$("span").withId("with-jandex").exists(),
                "Expecting span element with id 'with-jandex' to be present in 'hello-world-jandex' shadow DOM, but was not");
        Assertions.assertTrue(
                addon.$("axa-input-text").id("npm-dep-jandex").$("input")
                        .single().hasClassName("a-input-text__input"),
                "Expecting axa-input-text element with id 'npm-dep-jandex' to be rendered in 'hello-world-jandex' shadow DOM, but was not");
    }
}
