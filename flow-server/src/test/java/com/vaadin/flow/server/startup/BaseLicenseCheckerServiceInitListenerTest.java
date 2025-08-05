/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.server.startup;

import java.util.function.Consumer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.LicenseException;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;

@SuppressWarnings("unchecked")
public class BaseLicenseCheckerServiceInitListenerTest {

    private static final String PRODUCT_NAME = "vaadin-test-commercial-addon";
    private static final String PRODUCT_VERSION = "1.2.3";

    BaseLicenseCheckerServiceInitListener listener = new BaseLicenseCheckerServiceInitListener(
            PRODUCT_NAME, PRODUCT_VERSION) {
    };
    MockDeploymentConfiguration config = new MockDeploymentConfiguration();
    VaadinService service = new MockVaadinServletService(config, false);
    ServiceInitEvent event = new ServiceInitEvent(service);

    @Test
    public void serviceInit_productionMode_licenseNotChecked() {
        config.setProductionMode(true);
        try (MockedStatic<LicenseChecker> licenseChecker = Mockito
                .mockStatic(LicenseChecker.class)) {
            listener.serviceInit(event);
            licenseChecker.verifyNoInteractions();
        }
    }

    @Test
    public void serviceInit_devToolsDisabled_validLicense_noAction() {
        config.setProductionMode(false);
        config.setDevToolsEnabled(false);
        try (MockedStatic<LicenseChecker> licenseChecker = Mockito
                .mockStatic(LicenseChecker.class)) {
            listener.serviceInit(event);
            licenseChecker
                    .verify(() -> LicenseChecker.checkLicense(eq(PRODUCT_NAME),
                            eq(PRODUCT_VERSION), isNull(BuildType.class)));
            licenseChecker
                    .verify(() -> LicenseChecker.checkLicense(eq(PRODUCT_NAME),
                            eq(PRODUCT_VERSION), isNull(BuildType.class),
                            any(Consumer.class), anyInt()), never());
        }
    }

    @Test
    public void serviceInit_devToolsDisabled_missingOrInvalid_throws() {
        config.setProductionMode(false);
        config.setDevToolsEnabled(false);
        try (MockedStatic<LicenseChecker> licenseChecker = Mockito
                .mockStatic(LicenseChecker.class)) {
            LicenseException checkerException = new LicenseException(
                    "Invalid or missing license");
            licenseChecker
                    .when(() -> LicenseChecker.checkLicense(eq(PRODUCT_NAME),
                            eq(PRODUCT_VERSION), isNull(BuildType.class)))
                    .thenThrow(checkerException);

            LicenseException exception = Assert.assertThrows(
                    LicenseException.class, () -> listener.serviceInit(event));
            Assert.assertSame(checkerException, exception);
            licenseChecker
                    .verify(() -> LicenseChecker.checkLicense(eq(PRODUCT_NAME),
                            eq(PRODUCT_VERSION), isNull(BuildType.class),
                            any(Consumer.class), anyInt()), never());

        }
    }

    @Test
    public void serviceInit_devToolsEnabled_missingLicense_delegateHandlingToDevTools() {
        config.setProductionMode(false);
        config.setDevToolsEnabled(true);
        try (MockedStatic<LicenseChecker> licenseChecker = Mockito
                .mockStatic(LicenseChecker.class)) {
            licenseChecker
                    .when(() -> LicenseChecker.checkLicense(eq(PRODUCT_NAME),
                            eq(PRODUCT_VERSION), isNull(BuildType.class),
                            any(Consumer.class), eq(0)))
                    .then(i -> {
                        i.<Consumer<String>> getArgument(3).accept("URL");
                        return null;
                    });

            listener.serviceInit(event);
            licenseChecker.verify(
                    () -> LicenseChecker.checkLicense(eq(PRODUCT_NAME),
                            eq(PRODUCT_VERSION), isNull(BuildType.class)),
                    never());

            var indexHtmlRequestListeners = event
                    .getAddedIndexHtmlRequestListeners().toList();
            Assert.assertEquals(
                    "Expected index html request listener to be installed, but was not",
                    1, indexHtmlRequestListeners.size());
            Document document = Jsoup
                    .parse("<html><head></head><body></body></html>");
            IndexHtmlResponse indexHtmlResponse = new IndexHtmlResponse(
                    Mockito.mock(VaadinRequest.class),
                    Mockito.mock(VaadinResponse.class), document);
            indexHtmlRequestListeners.get(0)
                    .modifyIndexHtmlResponse(indexHtmlResponse);

            String headHTML = document.head().html();
            Assert.assertTrue(headHTML.contains(
                    "window.Vaadin.devTools.createdCvdlElements.push(product);"));
            Assert.assertTrue(headHTML.contains("registerProduct('%s','%s');"
                    .formatted(PRODUCT_NAME, PRODUCT_VERSION)));
        }
    }

    @Test
    public void serviceInit_devToolsEnabled_missingLicense_multipleListenersCollectedIntoSingleScript() {
        config.setProductionMode(false);
        config.setDevToolsEnabled(true);
        try (MockedStatic<LicenseChecker> licenseChecker = Mockito
                .mockStatic(LicenseChecker.class)) {
            licenseChecker.when(() -> LicenseChecker.checkLicense(anyString(),
                    anyString(), isNull(BuildType.class), any(Consumer.class),
                    eq(0))).then(i -> {
                        i.<Consumer<String>> getArgument(3).accept("URL");
                        return null;
                    });

            listener.serviceInit(event);
            licenseChecker.verify(
                    () -> LicenseChecker.checkLicense(eq(PRODUCT_NAME),
                            eq(PRODUCT_VERSION), isNull(BuildType.class)),
                    never());
            new BaseLicenseCheckerServiceInitListener("productB", "1.0.0") {
            }.serviceInit(event);
            licenseChecker
                    .verify(() -> LicenseChecker.checkLicense(eq("productB"),
                            eq("1.0.0"), isNull(BuildType.class)), never());
            new BaseLicenseCheckerServiceInitListener("productC", "2.4.6") {
            }.serviceInit(event);
            licenseChecker
                    .verify(() -> LicenseChecker.checkLicense(eq("productC"),
                            eq("2.4.6"), isNull(BuildType.class)), never());

            var indexHtmlRequestListeners = event
                    .getAddedIndexHtmlRequestListeners().toList();
            Assert.assertEquals(
                    "Expected a single index html request listener to be installed",
                    1, indexHtmlRequestListeners.size());
            Document document = Jsoup
                    .parse("<html><head></head><body></body></html>");
            IndexHtmlResponse indexHtmlResponse = new IndexHtmlResponse(
                    Mockito.mock(VaadinRequest.class),
                    Mockito.mock(VaadinResponse.class), document);
            indexHtmlRequestListeners.get(0)
                    .modifyIndexHtmlResponse(indexHtmlResponse);

            String headHTML = document.head().html();
            Assert.assertTrue(headHTML.contains(
                    "window.Vaadin.devTools.createdCvdlElements.push(product);"));
            Assert.assertTrue(headHTML.contains("registerProduct('%s','%s');"
                    .formatted(PRODUCT_NAME, PRODUCT_VERSION)));
            Assert.assertTrue(headHTML.contains("registerProduct('%s','%s');"
                    .formatted("productB", "1.0.0")));
            Assert.assertTrue(headHTML.contains("registerProduct('%s','%s');"
                    .formatted("productC", "2.4.6")));
        }
    }

    @Test
    public void serviceInit_devToolsEnabled_invalidLicense_throws() {
        config.setProductionMode(false);
        config.setDevToolsEnabled(true);
        try (MockedStatic<LicenseChecker> licenseChecker = Mockito
                .mockStatic(LicenseChecker.class)) {
            LicenseException checkerException = new LicenseException(
                    "Invalid or missing license");
            licenseChecker
                    .when(() -> LicenseChecker.checkLicense(eq(PRODUCT_NAME),
                            eq(PRODUCT_VERSION), isNull(BuildType.class),
                            any(Consumer.class), eq(0)))
                    .thenThrow(checkerException);

            LicenseException exception = Assert.assertThrows(
                    LicenseException.class, () -> listener.serviceInit(event));
            Assert.assertSame(checkerException, exception);
            licenseChecker.verify(
                    () -> LicenseChecker.checkLicense(eq(PRODUCT_NAME),
                            eq(PRODUCT_VERSION), isNull(BuildType.class)),
                    never());

        }
    }

}