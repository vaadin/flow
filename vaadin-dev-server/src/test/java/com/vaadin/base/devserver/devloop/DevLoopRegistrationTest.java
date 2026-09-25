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
package com.vaadin.base.devserver.devloop;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * An application the daemon did not launch must be left entirely alone: the
 * ownership model says the daemon aggregates state, it never competes for it.
 */
class DevLoopRegistrationTest {

    @AfterEach
    void clearHandshakeProperties() {
        System.clearProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY);
        System.clearProperty(DevLoopRegistration.TOKEN_PROPERTY);
        // The entries are a static set, so one test's marking would otherwise
        // be the next one's starting state.
        UsageStatistics.resetEntries();
        clearRegisteredService();
    }

    /**
     * Forgets the service a test left registered.
     * <p>
     * {@code start} assigns the static service before anything that can fail,
     * so a test that lets the listener reach it leaves a mock behind - and
     * every later test in the JVM then reads that mock instead of "no
     * application registered", which is what {@code DevLoopRedefinerTest}
     * asserts about. Reflection because the field is production state that one
     * registration owns for the life of the JVM, not a test hook.
     */
    private static void clearRegisteredService() {
        try {
            Field field = DevLoopRegistration.class.getDeclaredField("service");
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "could not forget the registered service; later tests in "
                            + "this JVM would read a mock",
                    e);
        }
    }

    @Test
    void noHandshakeProperties_isNotDaemonLaunched() {
        assertFalse(DevLoopRegistration.isDaemonLaunched());
    }

    @Test
    void portWithoutToken_isNotDaemonLaunched() {
        // Half a handshake is not a handshake: connecting without the token
        // would only ever get "unauthorized" back.
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");

        assertFalse(DevLoopRegistration.isDaemonLaunched());
    }

    @Test
    void bothProperties_isDaemonLaunched() {
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");
        System.setProperty(DevLoopRegistration.TOKEN_PROPERTY, "s3cr3t");

        assertTrue(DevLoopRegistration.isDaemonLaunched());
    }

    @Test
    void hotswapper_withoutADaemon_neverBecomesActive() {
        // A hotswap agent creates Flow's hotswapper in applications the daemon
        // never launched, and every VaadinHotswapper on the classpath joins
        // that
        // chain. This one has to stay out of the way: no active instance, so
        // every callback is a no-op and nothing walks a component tree on its
        // behalf.
        DevLoopHotswapper hotswapper = new DevLoopHotswapper();

        hotswapper.onInit(Mockito.mock(VaadinService.class));

        assertNull(DevLoopHotswapper.getActive());
        assertFalse(hotswapper.isCompleted());
    }

    @Test
    void hotswapper_daemonLaunched_becomesActive() {
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");
        System.setProperty(DevLoopRegistration.TOKEN_PROPERTY, "s3cr3t");
        DevLoopHotswapper hotswapper = new DevLoopHotswapper();

        hotswapper.onInit(Mockito.mock(VaadinService.class));

        assertSame(hotswapper, DevLoopHotswapper.getActive());
    }

    @Test
    void modeOf_distinguishesADevBundleFromAViteApplication() {
        // The regression this guards: isDevModeLiveReloadEnabled() is
        // isDevToolsEnabled() && devmode.liveReload, both true by default, so
        // it reported DEVELOPMENT_FRONTEND_LIVERELOAD for a dev-bundle
        // application too. The daemon decides whether a frontend edit was
        // already applied by Vite or needs the bundle rebuilt from this, and
        // those are opposite answers.
        assertEquals("DEVELOPMENT_BUNDLE", DevLoopRegistration
                .modeOf(serviceInMode(Mode.DEVELOPMENT_BUNDLE)));
        assertEquals("DEVELOPMENT_FRONTEND_LIVERELOAD", DevLoopRegistration
                .modeOf(serviceInMode(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD)));
    }

    private static VaadinService serviceInMode(Mode mode) {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.getMode()).thenReturn(mode);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        return service;
    }

    @Test
    void daemonLaunched_devloopMarkedAsUsed() {
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");
        System.setProperty(DevLoopRegistration.TOKEN_PROPERTY, "s3cr3t");
        ServiceInitEvent event = eventInProductionMode(false);

        new DevLoopInitListener().serviceInit(event);

        // The mocked service makes the registration itself fail, which the
        // listener catches - and the entry is still there. That is the point
        // of marking before the try: what is reported is that the daemon
        // launched this application, not that the loop came up.
        assertTrue(marked(DevLoopStatistics.STATISTIC_DEVLOOP));
    }

    @Test
    void noDaemon_devloopNotMarkedAsUsed() {
        new DevLoopInitListener().serviceInit(eventInProductionMode(false));

        assertFalse(marked(DevLoopStatistics.STATISTIC_DEVLOOP));
    }

    @Test
    void productionMode_devloopNotMarkedAsUsed() {
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");
        System.setProperty(DevLoopRegistration.TOKEN_PROPERTY, "s3cr3t");

        new DevLoopInitListener().serviceInit(eventInProductionMode(true));

        assertFalse(marked(DevLoopStatistics.STATISTIC_DEVLOOP));
    }

    @Test
    void redefineWithoutAnAgent_marksNothing() {
        // No Instrumentation, so no redefine happened. The apply entry says
        // the loop hot-swapped something, and it must not claim that here.
        assertTrue(DevLoopRedefiner.redefine("com.example.Nothing")
                .startsWith("ERR kind=no-agent"));

        assertFalse(marked(DevLoopStatistics.STATISTIC_DEVLOOP_APPLY));
    }

    private static ServiceInitEvent eventInProductionMode(
            boolean productionMode) {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode())
                .thenReturn(productionMode);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        ServiceInitEvent event = Mockito.mock(ServiceInitEvent.class);
        Mockito.when(event.getSource()).thenReturn(service);
        return event;
    }

    private static boolean marked(String name) {
        return UsageStatistics.getEntries()
                .anyMatch(entry -> name.equals(entry.getName()));
    }

    @Test
    void initListener_withoutADaemon_touchesNothing() {
        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = Mockito.mock(ServiceInitEvent.class);
        Mockito.when(event.getSource()).thenReturn(service);

        new DevLoopInitListener().serviceInit(event);

        // Not even the deployment configuration is asked for: an application
        // running from an IDE must see no trace of the dev loop.
        Mockito.verify(service, Mockito.never()).getDeploymentConfiguration();
    }

    @Test
    void initListener_productionMode_doesNotRegister() {
        System.setProperty(DevLoopRegistration.DAEMON_PORT_PROPERTY, "51234");
        System.setProperty(DevLoopRegistration.TOKEN_PROPERTY, "s3cr3t");
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        ServiceInitEvent event = Mockito.mock(ServiceInitEvent.class);
        Mockito.when(event.getSource()).thenReturn(service);

        new DevLoopInitListener().serviceInit(event);

        // The launch and the configuration disagree; the dev loop stays out
        // rather than opening a socket it could never redefine anything over.
        Mockito.verify(service, Mockito.never()).getContext();
    }

    /**
     * The daemon and the application are different JVMs, and
     * {@code getLoopbackAddress()} does not mean the same address in both: a
     * container can leave its own JVM preferring IPv6 while the daemon listens
     * on IPv4. Measured against Payara Micro 7.2026.9, that refused the
     * connection and the application ran unregistered - serving pages, but
     * invisible to the loop, with every apply reporting there was nothing to
     * apply to. So both families are tried.
     */
    @Test
    void bothLoopbackFamiliesAreTried() {
        List<InetAddress> candidates = DevLoopRegistration.loopbackAddresses();

        assertTrue(candidates.contains(InetAddress.getLoopbackAddress()),
                "the JVM's own preference has to be among them: " + candidates);
        assertTrue(candidates.stream().anyMatch(Inet4Address.class::isInstance),
                "no IPv4 loopback among " + candidates);
        assertTrue(candidates.stream().anyMatch(Inet6Address.class::isInstance),
                "no IPv6 loopback among " + candidates);
        assertTrue(candidates.stream().allMatch(InetAddress::isLoopbackAddress),
                "nothing here may be reachable from off the machine: "
                        + candidates);
    }

    /**
     * The preferred address goes first, so the ordinary case connects on the
     * first attempt and pays nothing for the fallback.
     */
    @Test
    void theJvmsOwnPreferenceIsTriedFirst() {
        List<InetAddress> candidates = DevLoopRegistration.loopbackAddresses();

        assertEquals(InetAddress.getLoopbackAddress(), candidates.get(0));
        // A duplicate would mean dialling the same refused address twice
        // before reaching the one that answers.
        assertEquals(candidates.size(), Set.copyOf(candidates).size(),
                "duplicate candidates: " + candidates);
    }

    /**
     * The connect loop keeps the first failure and throws it once every
     * candidate has been tried, so an empty list would throw a
     * {@code NullPointerException} out of a method that promises an
     * {@code IOException}. It cannot be empty - {@code getLoopbackAddress}
     * always yields one - and this is what holds that true.
     */
    @Test
    void thereIsAlwaysAtLeastOneCandidate() {
        assertFalse(DevLoopRegistration.loopbackAddresses().isEmpty());
    }

    /**
     * A daemon listening on the family this JVM does not prefer is still
     * reached, after the preferred address has refused.
     */
    @Test
    void aDaemonOnTheOtherFamilyIsStillReached() throws IOException {
        InetAddress other = DevLoopRegistration.loopbackAddresses().stream()
                .filter(address -> !address
                        .equals(InetAddress.getLoopbackAddress()))
                .findFirst().orElseThrow();
        try (ServerSocket daemon = bind(other);
                Socket socket = DevLoopRegistration
                        .connectToDaemon(daemon.getLocalPort())) {
            assertEquals(other, socket.getInetAddress());
        }
    }

    /**
     * With nothing listening on any loopback address the failure is thrown, so
     * the application can say it runs unregistered.
     */
    @Test
    void noDaemonListening_throws() throws IOException {
        int port;
        try (ServerSocket released = new ServerSocket(0, 1,
                InetAddress.getLoopbackAddress())) {
            port = released.getLocalPort();
        }

        assertThrows(IOException.class,
                () -> DevLoopRegistration.connectToDaemon(port));
    }

    private static ServerSocket bind(InetAddress address) {
        try {
            return new ServerSocket(0, 1, address);
        } catch (IOException e) {
            // A stack without that family has nothing to fall back to.
            return abort("cannot listen on " + address + ": " + e);
        }
    }
}
