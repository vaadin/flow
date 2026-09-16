/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.cdi.context;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.inject.spi.BeanManager;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.cdi.annotation.VaadinSessionScoped;
import com.vaadin.cdi.context.VaadinSessionScopedContext.ContextualStorageManager;
import com.vaadin.cdi.util.BeanProvider;
import com.vaadin.cdi.util.ContextualStorage;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SessionContextTest
        extends AbstractContextTest<SessionContextTest.SessionScopedTestBean> {

    @Override
    protected UnderTestContext newContextUnderTest() {
        return new SessionUnderTestContext();
    }

    @Override
    protected boolean isNormalScoped() {
        return true;
    }

    @Override
    protected Class<SessionScopedTestBean> getBeanType() {
        return SessionScopedTestBean.class;
    }

    @Test
    public void get_sessionExistsButNotLocked_contextNotActive() {
        SessionUnderTestContext context = new SessionUnderTestContext();
        context.activate();

        VaadinSession session = context.getSession();
        when(session.hasLock()).thenReturn(false);

        VaadinSessionScopedContext sessionContext = newContext();

        assertFalse(sessionContext.isActive());

        assertThrows(ContextNotActiveException.class, () -> {
            SessionScopedTestBean ref = BeanProvider
                    .getContextualReference(SessionScopedTestBean.class);
            ref.getState();
        });
    }

    @Test
    public void defaultManager_sessionLocked_isActive() {
        SessionUnderTestContext context = new SessionUnderTestContext();
        context.activate();
        VaadinSession session = context.getSession();
        when(session.hasLock()).thenReturn(true);

        ContextualStorageManager manager = lookupManager();
        assertTrue(manager.isActive());
    }

    @Test
    public void context_nullSession_notActive() {
        VaadinSession.setCurrent(null);
        VaadinSessionScopedContext sessionContext = newContext();
        assertFalse(sessionContext.isActive());
    }

    @Test
    public void defaultManager_storageAccess_returnsStorage() {
        SessionUnderTestContext context = new SessionUnderTestContext();
        context.activate();
        VaadinSession session = context.getSession();
        when(session.hasLock()).thenReturn(true);

        VaadinSessionScopedContext sessionContext = newContext();
        ContextualStorage storage = sessionContext.getContextualStorage(null,
                true);
        assertNotNull(storage);
        verify(session, never()).accessSynchronously(any(Command.class));
    }

    @Test
    public void lenientSubclass_isActiveWithoutLock() {
        SessionUnderTestContext context = new SessionUnderTestContext();
        context.activate();
        VaadinSession session = context.getSession();
        when(session.hasLock()).thenReturn(false);

        ContextualStorageManager lenient = injectBeanManager(
                new ContextualStorageManager() {
                    @Override
                    protected boolean isActive() {
                        return VaadinSession.getCurrent() != null;
                    }
                });

        VaadinSessionScopedContext sessionContext = newContext(lenient);
        assertTrue(sessionContext.isActive());
    }

    @Test
    public void lenientSubclass_storageAccess_wrappedInAccessSynchronously() {
        SessionUnderTestContext context = new SessionUnderTestContext();
        context.activate();
        VaadinSession session = context.getSession();
        when(session.hasLock()).thenReturn(false);
        doAnswer(inv -> {
            ((Command) inv.getArgument(0)).execute();
            return null;
        }).when(session).accessSynchronously(any(Command.class));

        ContextualStorageManager lenient = injectBeanManager(
                new ContextualStorageManager() {
                    @Override
                    protected boolean isActive() {
                        return VaadinSession.getCurrent() != null;
                    }

                    @Override
                    protected ContextualStorage getContextualStorage(
                            Contextual<?> c, boolean create) {
                        VaadinSession s = VaadinSession.getCurrent();
                        if (s.hasLock()) {
                            return super.getContextualStorage(c, create);
                        }
                        AtomicReference<ContextualStorage> ref = new AtomicReference<>();
                        s.accessSynchronously(() -> ref
                                .set(super.getContextualStorage(c, create)));
                        return ref.get();
                    }
                });

        VaadinSessionScopedContext sessionContext = newContext(lenient);
        ContextualStorage storage = sessionContext.getContextualStorage(null,
                true);

        assertNotNull(storage);
        verify(session).accessSynchronously(any(Command.class));
    }

    @Test
    public void lenientSubclass_storageAccess_lockHeldFastPath() {
        SessionUnderTestContext context = new SessionUnderTestContext();
        context.activate();
        VaadinSession session = context.getSession();
        when(session.hasLock()).thenReturn(true);

        ContextualStorageManager lenient = injectBeanManager(
                new ContextualStorageManager() {
                    @Override
                    protected boolean isActive() {
                        return VaadinSession.getCurrent() != null;
                    }

                    @Override
                    protected ContextualStorage getContextualStorage(
                            Contextual<?> c, boolean create) {
                        VaadinSession s = VaadinSession.getCurrent();
                        if (s.hasLock()) {
                            return super.getContextualStorage(c, create);
                        }
                        AtomicReference<ContextualStorage> ref = new AtomicReference<>();
                        s.accessSynchronously(() -> ref
                                .set(super.getContextualStorage(c, create)));
                        return ref.get();
                    }
                });

        VaadinSessionScopedContext sessionContext = newContext(lenient);
        ContextualStorage storage = sessionContext.getContextualStorage(null,
                true);

        assertNotNull(storage);
        // With the lock held, lenient strategy avoids the extra wrap.
        verify(session, never()).accessSynchronously(any(Command.class));
        // A second lookup must return the same storage instance.
        assertSame(storage, sessionContext.getContextualStorage(null, false));
    }

    @Test
    public void contextIsActive_whenInitNotCalled_returnsFalse() {
        // Guards against NPE if isActive() is somehow invoked before
        // VaadinExtension.initializeContexts has run.
        SessionUnderTestContext context = new SessionUnderTestContext();
        context.activate();
        BeanManager beanManager = weld.select().select(BeanManager.class).get();
        VaadinSessionScopedContext bare = new VaadinSessionScopedContext(
                beanManager);
        assertFalse(bare.isActive());
    }

    private VaadinSessionScopedContext newContext() {
        BeanManager beanManager = weld.select().select(BeanManager.class).get();
        VaadinSessionScopedContext sessionContext = new VaadinSessionScopedContext(
                beanManager);
        sessionContext.init(beanManager);
        return sessionContext;
    }

    private VaadinSessionScopedContext newContext(
            ContextualStorageManager manager) {
        BeanManager beanManager = weld.select().select(BeanManager.class).get();
        VaadinSessionScopedContext sessionContext = new VaadinSessionScopedContext(
                beanManager);
        try {
            Field f = VaadinSessionScopedContext.class
                    .getDeclaredField("contextManager");
            f.setAccessible(true);
            f.set(sessionContext, manager);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to install test manager", e);
        }
        return sessionContext;
    }

    private ContextualStorageManager lookupManager() {
        return BeanProvider.getContextualReference(
                weld.select().select(BeanManager.class).get(),
                ContextualStorageManager.class, false);
    }

    private static <M extends ContextualStorageManager> M injectBeanManager(
            M target) {
        try {
            Field f = ContextualStorageManager.class
                    .getDeclaredField("beanManager");
            f.setAccessible(true);
            f.set(target, Mockito.mock(BeanManager.class));
            return target;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to inject BeanManager", e);
        }
    }

    @VaadinSessionScoped
    public static class SessionScopedTestBean extends TestBean {
    }
}
