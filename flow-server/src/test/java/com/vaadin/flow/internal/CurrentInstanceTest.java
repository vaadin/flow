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
package com.vaadin.flow.internal;

import static org.junit.Assert.assertNull;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.TestUtil;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class CurrentInstanceTest {

    @Before
    public void clearExistingThreadLocals() {
        // Ensure no previous test left some thread locals hanging
        CurrentInstance.clearAll();
    }

    @Test
    public void testInitiallyCleared() throws Exception {
        assertCleared();
    }

    @Test
    public void testClearedAfterRemove() throws Exception {
        CurrentInstance.set(CurrentInstanceTest.class, this);
        Assert.assertEquals(this,
                CurrentInstance.get(CurrentInstanceTest.class));
        CurrentInstance.set(CurrentInstanceTest.class, null);

        assertCleared();
    }

    @Test
    public void testClearedWithClearAll() throws Exception {
        CurrentInstance.set(CurrentInstanceTest.class, this);
        Assert.assertEquals(this,
                CurrentInstance.get(CurrentInstanceTest.class));
        CurrentInstance.clearAll();

        assertCleared();
    }

    private void assertCleared() throws SecurityException, NoSuchFieldException,
            IllegalAccessException {
        Assert.assertNull(getInternalCurrentInstanceVariable().get());
    }

    @SuppressWarnings("unchecked")
    private ThreadLocal<Map<Class<?>, CurrentInstance>> getInternalCurrentInstanceVariable()
            throws SecurityException, NoSuchFieldException,
            IllegalAccessException {
        Field f = CurrentInstance.class.getDeclaredField("instances");
        f.setAccessible(true);
        return (ThreadLocal<Map<Class<?>, CurrentInstance>>) f.get(null);
    }

    public void testInheritedClearedAfterRemove() {

    }

    private static class UIStoredInCurrentInstance extends UI {
        @Override
        protected void init(VaadinRequest request) {
        }
    }

    private static class SessionStoredInCurrentInstance extends VaadinSession {
        public SessionStoredInCurrentInstance(VaadinService service) {
            super(service);
        }
    }

    @Test
    public void testRestoringNullUIWorks() throws Exception {
        // First make sure current instance is empty
        CurrentInstance.clearAll();

        // Then store a new UI in there
        Map<Class<?>, CurrentInstance> old = CurrentInstance
                .setCurrent(new UIStoredInCurrentInstance());

        // Restore the old values and assert that the UI is null again
        CurrentInstance.restoreInstances(old);
        assertNull(CurrentInstance.get(UI.class));
    }

    @Test
    public void testRestoringNullSessionWorks() throws Exception {
        // First make sure current instance is empty
        CurrentInstance.clearAll();

        // Then store a new session in there
        Map<Class<?>, CurrentInstance> old = CurrentInstance
                .setCurrent(new SessionStoredInCurrentInstance(
                        new MockVaadinServletService()));

        // Restore the old values and assert that the session is null again
        CurrentInstance.restoreInstances(old);
        assertNull(CurrentInstance.get(VaadinSession.class));
        assertNull(CurrentInstance.get(VaadinService.class));
    }

    @Test
    public void testRestoreWithGarbageCollectedValue()
            throws InterruptedException {
        VaadinSession session1 = new VaadinSession(
                new MockVaadinServletService()) {
            @Override
            public String toString() {
                return "First session";
            }
        };
        VaadinSession session2 = new VaadinSession(
                new MockVaadinServletService()) {
            @Override
            public String toString() {
                return "Second session";
            }
        };

        VaadinSession.setCurrent(session1);
        Map<Class<?>, CurrentInstance> previous = CurrentInstance
                .setCurrent(session2);

        // Use weak ref to verify object is collected
        WeakReference<VaadinSession> ref = new WeakReference<>(session1);

        session1 = null;
        Assert.assertTrue(TestUtil.isGarbageCollected(ref));

        CurrentInstance.restoreInstances(previous);

        Assert.assertNull(VaadinSession.getCurrent());
    }

    @Test
    public void nonInheritableThreadLocals()
            throws InterruptedException, ExecutionException {
        CurrentInstance.clearAll();
        CurrentInstance.set(CurrentInstanceTest.class, this);

        Assert.assertNotNull(CurrentInstance.get(CurrentInstanceTest.class));

        Callable<Void> runnable = () -> {
            Assert.assertNull(CurrentInstance.get(CurrentInstanceTest.class));
            return null;
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<Void> future = service.submit(runnable);
        future.get();
    }
}
