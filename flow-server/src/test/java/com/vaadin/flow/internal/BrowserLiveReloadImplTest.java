/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class BrowserLiveReloadImplTest {

    private BrowserLiveReloadImpl reload = new BrowserLiveReloadImpl();

    @Test
    public void onConnect_suspend_sayHello() {
        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
        Mockito.when(resource.getBroadcaster()).thenReturn(broadcaster);

        reload.onConnect(resource);

        Assert.assertTrue(reload.isLiveReload(resource));
        Mockito.verify(resource).suspend(-1);
        Mockito.verify(broadcaster).broadcast("{\"command\": \"hello\"}",
                resource);
    }

    @Test
    public void reload_twoConnections_sendReloadCommand() {
        AtmosphereResource resource1 = Mockito.mock(AtmosphereResource.class);
        AtmosphereResource resource2 = Mockito.mock(AtmosphereResource.class);
        Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
        Mockito.when(resource1.getBroadcaster()).thenReturn(broadcaster);
        Mockito.when(resource2.getBroadcaster()).thenReturn(broadcaster);
        reload.onConnect(resource1);
        reload.onConnect(resource2);
        Assert.assertTrue(reload.isLiveReload(resource1));
        Assert.assertTrue(reload.isLiveReload(resource2));

        reload.reload();

        Mockito.verify(broadcaster).broadcast("{\"command\": \"reload\"}",
                resource1);
        Mockito.verify(broadcaster).broadcast("{\"command\": \"reload\"}",
                resource2);
    }

    @Test
    public void reload_resourceIsNotSet_reloadCommandIsNotSent() {
        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
        Mockito.when(resource.getBroadcaster()).thenReturn(broadcaster);
        Assert.assertFalse(reload.isLiveReload(resource));

        reload.reload();

        Mockito.verifyZeroInteractions(broadcaster);
    }

    @Test
    public void reload_resourceIsDisconnected_reloadCommandIsNotSent() {
        AtmosphereResource resource = Mockito.mock(AtmosphereResource.class);
        Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
        Mockito.when(resource.getBroadcaster()).thenReturn(broadcaster);
        reload.onConnect(resource);
        Assert.assertTrue(reload.isLiveReload(resource));
        Mockito.reset(broadcaster);
        reload.onDisconnect(resource);
        Assert.assertFalse(reload.isLiveReload(resource));

        reload.reload();

        Mockito.verifyZeroInteractions(broadcaster);
    }

    @Test
    public void getBackend_JRebelInitializerClassLoaded_returnsJREBEL() {
        class JRebelInitializer {
        }
        BrowserLiveReloadImpl reload = new BrowserLiveReloadImpl(
                new ClassLoader(getClass().getClassLoader()) {
                    @Override
                    protected Class<?> findClass(String name)
                            throws ClassNotFoundException {
                        switch (name) {
                        case "com.vaadin.flow.server.jrebel.JRebelInitializer":
                            return JRebelInitializer.class;
                        default:
                            throw new ClassNotFoundException();
                        }
                    }
                });
        Assert.assertEquals(BrowserLiveReload.Backend.JREBEL,
                reload.getBackend());
    }

    @Test
    public void getBackend_HotSwapVaadinIntegrationClassLoaded_returnsHOTSWAP_AGENT() {
        class VaadinIntegration {
        }
        BrowserLiveReloadImpl reload = new BrowserLiveReloadImpl(
                new ClassLoader(getClass().getClassLoader()) {
                    @Override
                    protected Class<?> findClass(String name)
                            throws ClassNotFoundException {
                        switch (name) {
                        case "org.hotswap.agent.plugin.vaadin.VaadinIntegration":
                            return VaadinIntegration.class;
                        default:
                            throw new ClassNotFoundException();
                        }
                    }
                });
        Assert.assertEquals(BrowserLiveReload.Backend.HOTSWAP_AGENT,
                reload.getBackend());
    }

    @Test
    public void getBackend_SpringBootDevtoolsClassesLoaded_returnsSPRING_BOOT_DEVTOOLS() {
        class SpringServlet {
        }
        class LiveReloadServer {
        }
        BrowserLiveReloadImpl reload = new BrowserLiveReloadImpl(
                new ClassLoader(getClass().getClassLoader()) {
                    @Override
                    protected Class<?> findClass(String name)
                            throws ClassNotFoundException {
                        switch (name) {
                        case "com.vaadin.flow.spring.SpringServlet":
                            return SpringServlet.class;
                        case "org.springframework.boot.devtools.livereload.LiveReloadServer":
                            return LiveReloadServer.class;
                        default:
                            throw new ClassNotFoundException();
                        }
                    }
                });
        Assert.assertEquals(BrowserLiveReload.Backend.SPRING_BOOT_DEVTOOLS,
                reload.getBackend());
    }

}
