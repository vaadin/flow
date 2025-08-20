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
package com.vaadin.base.devserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.Capabilities;
import com.vaadin.pro.licensechecker.Capability;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.LicenseException;
import com.vaadin.pro.licensechecker.PreTrial;
import com.vaadin.pro.licensechecker.PreTrialCreationException;
import com.vaadin.pro.licensechecker.PreTrialLicenseValidationException;
import com.vaadin.pro.licensechecker.Product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

public class DebugWindowConnectionLicenseCheckTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Product TEST_PRODUCT = new Product(
            "commercial-component", "1.0.0");
    public static final String INVALID_LICENSE_ERROR = "Invalid license";
    public static final String MISSING_KEY_ERROR = "Pre trial not started yet.";
    public static final PreTrial TEST_PRE_TRIAL = new PreTrial("Test trial",
            PreTrial.PreTrialState.START_ALLOWED, 5, 22);

    private final DebugWindowConnection reload = new DebugWindowConnection(
            getMockContext());
    private final ClientMessageReceiver receiver = new ClientMessageReceiver();

    @Test
    public void checkLicense_validLicense_sendLicenseOk() {
        DebugWindowMessage message = doLicenseCheck(LicenseCheckResult.VALID);
        Assert.assertEquals("license-check-ok", message.getCommand());
        Assert.assertTrue("Expected a Product object in response message",
                message.getData() instanceof Product);
        Assert.assertEquals(TEST_PRODUCT.toString(),
                message.getData().toString());
    }

    @Test
    public void checkLicense_invalidLicense_sendLicenseCheckFailed() {
        DebugWindowMessage message = doLicenseCheck(LicenseCheckResult.INVALID);
        Assert.assertEquals("license-check-failed", message.getCommand());
        Assert.assertTrue(
                "Expected a ProductAndMessage object in response message",
                message.getData() instanceof ProductAndMessage);
        ProductAndMessage productAndMessage = (ProductAndMessage) message
                .getData();
        Assert.assertEquals(INVALID_LICENSE_ERROR,
                productAndMessage.getMessage());
        Assert.assertEquals(TEST_PRODUCT.toString(),
                productAndMessage.getProduct().toString());
        Assert.assertNull("Expected pre-trial info to be absent",
                productAndMessage.getPreTrial());
    }

    @Test
    public void checkLicense_noLicenseKeys_sendLicenseCheckFailed() {
        DebugWindowMessage message = doLicenseCheck(
                LicenseCheckResult.MISSING_KEYS);

        Assert.assertEquals("license-check-nokey", message.getCommand());
        Assert.assertTrue(
                "Expected a ProductAndMessage object in response message",
                message.getData() instanceof ProductAndMessage);
        ProductAndMessage productAndMessage = (ProductAndMessage) message
                .getData();
        Assert.assertTrue(
                productAndMessage.getMessage().contains(MISSING_KEY_ERROR));
        Assert.assertEquals(TEST_PRODUCT.toString(),
                productAndMessage.getProduct().toString());
        Assert.assertEquals(TEST_PRODUCT.toString(),
                productAndMessage.getProduct().toString());
        Assert.assertNotNull("Expected pre-trial info to be present",
                productAndMessage.getPreTrial());
        Assert.assertEquals(TEST_PRE_TRIAL.toString(),
                productAndMessage.getPreTrial().toString());
    }

    @Test
    public void startPreTrial_preTrialAllowed_sendPreTrialInfo() {
        DebugWindowMessage message = doStartPreTrial(
                PreTrial.PreTrialState.START_ALLOWED);
        Assert.assertEquals("license-pretrial-started", message.getCommand());
        Assert.assertTrue("Expected a PreTrial object in response message",
                message.getData() instanceof PreTrial);
        PreTrial preTrial = (PreTrial) message.getData();
        Assert.assertEquals(TEST_PRE_TRIAL.toString(), preTrial.toString());
    }

    @Test
    public void startPreTrial_preTrialRunning_sendPreTrialInfo() {
        DebugWindowMessage message = doStartPreTrial(
                PreTrial.PreTrialState.RUNNING);
        Assert.assertEquals("license-pretrial-started", message.getCommand());
        Assert.assertTrue("Expected a PreTrial object in response message",
                message.getData() instanceof PreTrial);
        PreTrial preTrial = (PreTrial) message.getData();
        Assert.assertEquals(TEST_PRE_TRIAL.toString(), preTrial.toString());
    }

    @Test
    public void startPreTrial_preTrialExpired_sendPreTrialExpiredFailure() {
        DebugWindowMessage message = doStartPreTrial(
                PreTrial.PreTrialState.EXPIRED);
        Assert.assertEquals("license-pretrial-expired", message.getCommand());
    }

    @Test
    public void startPreTrial_preTrialNotAllowed_sendPreTrialNotAllowedFailure() {
        DebugWindowMessage message = doStartPreTrial(null);
        Assert.assertEquals("license-pretrial-failed", message.getCommand());
    }

    @Test
    public void startPreTrial_genericError_sendPreTrialNotAllowedFailure() {
        DebugWindowMessage message = doStartPreTrial(
                PreTrial.PreTrialState.ACCESS_DENIED);
        Assert.assertEquals("license-pretrial-failed", message.getCommand());
    }

    @Test
    public void downloadLicenseKey_licenseKeyAvailable_notifiesDownloadStartAndSuccess() {
        AtomicReference<Runnable> callbackHolder = new AtomicReference<>();
        DebugWindowMessage message = doDownloadKey(true, callbackHolder);
        Assert.assertEquals("license-download-started", message.getCommand());
        Assert.assertEquals(TEST_PRODUCT.toString(),
                message.getData().toString());
        callbackHolder.get().run();
        Assert.assertEquals(1, receiver.messages.size());
        Assert.assertEquals("license-download-completed",
                receiver.messages.get(0).getCommand());
        Assert.assertEquals(TEST_PRODUCT.toString(),
                receiver.messages.get(0).getData().toString());
    }

    @Test
    public void downloadLicenseKey_licenseKeyNotAvailable_notifiesDownloadStartAndFailure() {
        AtomicReference<Runnable> callbackHolder = new AtomicReference<>();
        DebugWindowMessage message = doDownloadKey(false, callbackHolder);
        Assert.assertEquals("license-download-started", message.getCommand());
        Assert.assertEquals(TEST_PRODUCT.toString(),
                message.getData().toString());
        callbackHolder.get().run();
        Assert.assertEquals(1, receiver.messages.size());
        Assert.assertEquals("license-download-failed",
                receiver.messages.get(0).getCommand());
        Assert.assertEquals(TEST_PRODUCT.toString(),
                receiver.messages.get(0).getData().toString());
    }

    private enum LicenseCheckResult {
        VALID, INVALID, MISSING_KEYS
    }

    private DebugWindowMessage doLicenseCheck(
            LicenseCheckResult licenseCheckResult) {
        ObjectNode command = OBJECT_MAPPER.createObjectNode();
        command.put("command", "checkLicense");
        command.putPOJO("data", TEST_PRODUCT);
        return sendAndReceive(command, mockCheckLicense(licenseCheckResult));
    }

    private DebugWindowMessage doStartPreTrial(
            PreTrial.PreTrialState preTrialState) {
        ObjectNode command = OBJECT_MAPPER.createObjectNode();
        command.put("command", "startPreTrialLicense");
        return sendAndReceive(command, licenseChecker -> licenseChecker
                .when(LicenseChecker::startPreTrial).then(i -> {
                    if (preTrialState == null) {
                        throw new PreTrialCreationException("BOOM!!!!");
                    }
                    return switch (preTrialState) {
                    case START_ALLOWED -> TEST_PRE_TRIAL;
                    case RUNNING -> TEST_PRE_TRIAL;
                    case EXPIRED -> throw new PreTrialCreationException.Expired(
                            "Trial expired");
                    case ACCESS_DENIED -> throw new PreTrialCreationException(
                            "Pre trial not allowed");
                    };
                }));
    }

    private DebugWindowMessage doDownloadKey(boolean success,
            AtomicReference<Runnable> callbackHolder) {
        ObjectNode command = OBJECT_MAPPER.createObjectNode();
        command.put("command", "downloadLicense");
        command.putPOJO("data", TEST_PRODUCT);
        return sendAndReceive(command,
                licenseChecker -> licenseChecker
                        .when(() -> LicenseChecker.checkLicenseAsync(
                                eq(TEST_PRODUCT.getName()),
                                eq(TEST_PRODUCT.getVersion()),
                                eq(BuildType.DEVELOPMENT),
                                any(LicenseChecker.Callback.class),
                                eq(Capabilities.of(Capability.PRE_TRIAL))))
                        .then(i -> {
                            LicenseChecker.Callback callback = i.getArgument(3,
                                    LicenseChecker.Callback.class);
                            callbackHolder.set(() -> {
                                if (success) {
                                    callback.ok();
                                } else {
                                    callback.failed(
                                            new LicenseException("BOOM"));
                                }
                            });
                            return null;
                        }));
    }

    private DebugWindowMessage sendAndReceive(ObjectNode command,
            Consumer<MockedStatic<LicenseChecker>> instrumenter) {
        receiver.messages.clear();
        withMockedLicenseChecker(instrumenter,
                () -> reload.onMessage(receiver.resource, command.toString()));
        int responses = receiver.messages.size();
        Assert.assertEquals("Expected messages", 1, responses);
        DebugWindowMessage message = receiver.messages.get(0);
        receiver.messages.clear();
        return message;
    }

    private void withMockedLicenseChecker(
            Consumer<MockedStatic<LicenseChecker>> instrumenter,
            Runnable test) {
        try (MockedStatic<LicenseChecker> licenseChecker = Mockito
                .mockStatic(LicenseChecker.class, Answers.RETURNS_MOCKS)) {
            instrumenter.accept(licenseChecker);
            test.run();
        }
    }

    private Consumer<MockedStatic<LicenseChecker>> mockCheckLicense(
            LicenseCheckResult licenseCheckResult) {
        return licenseChecker -> {
            licenseChecker.when(
                    () -> LicenseChecker.checkLicense(anyString(), anyString(),
                            Mockito.any(BuildType.class), Mockito.isNull()))
                    .then(i -> {
                        switch (licenseCheckResult) {
                        case INVALID ->
                            throw new LicenseException(INVALID_LICENSE_ERROR);
                        case MISSING_KEYS -> {
                            throw new PreTrialLicenseValidationException(
                                    TEST_PRE_TRIAL);
                        }
                        default -> {
                        }
                        }
                        return null;
                    });
        };
    }

    private VaadinContext getMockContext() {
        VaadinContext context = new MockVaadinContext();
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenReturn(false);
        context.setAttribute(ApplicationConfiguration.class, appConfig);
        context.setAttribute(Lookup.class,
                Lookup.of(appConfig, ApplicationConfiguration.class));
        return context;
    }

    private static class ClientMessageReceiver {
        private final List<DebugWindowMessage> messages = new ArrayList<>();
        private final AtmosphereResource resource;

        private ClientMessageReceiver() {
            this.resource = Mockito.mock(AtmosphereResource.class);
            Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
            Mockito.when(resource.getBroadcaster()).thenReturn(broadcaster);
            Mockito.when(broadcaster.broadcast(anyString(), same(resource)))
                    .then(i -> {
                        messages.add(deserializeMessage(
                                i.getArgument(0, String.class)));
                        return null;
                    });
        }

        private DebugWindowMessage deserializeMessage(String message)
                throws JsonProcessingException {
            JsonNode json = OBJECT_MAPPER.readTree(message);
            String command = json.get("command").textValue();
            JsonNode data = json.get("data");
            if (command.startsWith("license-check-")
                    || command.startsWith("license-download-")) {
                if (data.has("message") && data.has("product")) {
                    PreTrial preTrial = null;
                    if (data.hasNonNull("preTrial")) {
                        preTrial = deserializePreTrial(data.get("preTrial"));
                    }
                    return new DebugWindowMessage(command,
                            new ProductAndMessage(new Product(
                                    data.get("product").get("name").textValue(),
                                    data.get("product").get("version")
                                            .textValue()),
                                    preTrial, data.get("message").textValue()));
                } else if (data.has("name") && data.has("version")) {
                    return new DebugWindowMessage(command,
                            new Product(data.get("name").textValue(),
                                    data.get("version").textValue()));
                }
            } else if (command.startsWith("license-pretrial-")) {
                PreTrial preTrial = deserializePreTrial(data);
                return new DebugWindowMessage(command, preTrial);
            }
            throw new UnsupportedOperationException(
                    "Unknown message: " + message);
        }

        private static PreTrial deserializePreTrial(JsonNode data) {
            if (data.has("trialName")) {
                return new PreTrial(data.get("trialName").textValue(),
                        PreTrial.PreTrialState
                                .valueOf(data.get("trialState").textValue()),
                        data.get("daysRemaining").intValue(),
                        data.get("daysRemainingUntilRenewal").intValue());
            }
            return null;
        }
    }

}
