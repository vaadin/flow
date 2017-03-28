/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.spring.internal.UIScopeImpl.UIStore;
import com.vaadin.spring.internal.UIScopeImpl.VaadinSessionBeanStoreRetrievalStrategy;
import com.vaadin.util.CurrentInstance;

public class UIScopeImplTest {

    private static final String TEST_BEAN_NAME = "TestBean";
    private static final String TEST_SESSION_ID = "TestSessionID";
    private static final int TEST_UIID = 123;
    private static final String TEST_CONVERSATION_ID = "TestConversationID";

    private UIScopeImpl uiScopeImpl;
    private Object bean = new Object();
    private ObjectFactory<Object> objFactory;

    VaadinSessionBeanStoreRetrievalStrategy vaadinBSRetrieval;

    String beanStoreName = "TestBeanStore";
    BeanStore beanStore;

    @SuppressWarnings("unchecked")
    @Before
    public void initTestCase() {
        objFactory = mock(ObjectFactory.class);
        when(objFactory.getObject()).thenReturn(bean);

        beanStore = mock(BeanStore.class);
        when(beanStore.get(TEST_BEAN_NAME, objFactory)).thenReturn(bean);
        when(beanStore.remove(TEST_BEAN_NAME)).thenReturn(bean);

        vaadinBSRetrieval = mock(VaadinSessionBeanStoreRetrievalStrategy.class);
        when(vaadinBSRetrieval.getBeanStore()).thenReturn(beanStore);
        when(vaadinBSRetrieval.getConversationId()).thenReturn(
                TEST_CONVERSATION_ID);

        uiScopeImpl = new UIScopeImpl();
        UIScopeImpl.setBeanStoreRetrievalStrategy(vaadinBSRetrieval);
    }

    @Test
    public void testSetBeanStoreRetrievalStrategy() {

        assertSame(UIScopeImpl.getBeanStoreRetrievalStrategy(),
                vaadinBSRetrieval);

        UIScopeImpl.setBeanStoreRetrievalStrategy(null);
        assertNotNull(UIScopeImpl.getBeanStoreRetrievalStrategy());
        assertNotSame(UIScopeImpl.getBeanStoreRetrievalStrategy(),
                vaadinBSRetrieval);

        UIScopeImpl.setBeanStoreRetrievalStrategy(vaadinBSRetrieval);
        assertSame(UIScopeImpl.getBeanStoreRetrievalStrategy(),
                vaadinBSRetrieval);
    }

    @Test
    public void testGet() {
        Object ret = uiScopeImpl.get(TEST_BEAN_NAME, objFactory);

        assertSame(bean, ret);

        verify(beanStore).get(TEST_BEAN_NAME, objFactory);
    }

    @Test
    public void testRemove() {
        Object ret1 = uiScopeImpl.get(TEST_BEAN_NAME, objFactory);
        Object ret = uiScopeImpl.remove(TEST_BEAN_NAME);

        assertSame(bean, ret);

        verify(beanStore).remove(TEST_BEAN_NAME);
    }

    @Test
    public void testRegisterDestructionCallback() {
        String s = "CallBack";
        Runnable runnable = mock(Runnable.class);
        uiScopeImpl.registerDestructionCallback(s, runnable);

        verify(beanStore).registerDestructionCallback(s, runnable);
    }

    @Test
    public void testResolveContextualObject() {
        assertNull(uiScopeImpl.resolveContextualObject("SomeString"));
    }

    @Test
    public void testGetConversationId() {
        String ret = uiScopeImpl.getConversationId();

        assertSame(TEST_CONVERSATION_ID, ret);

        verify(vaadinBSRetrieval).getConversationId();
    }

    // This interface allows the injection of various implementations of test
    // cases inside the beanStoreTest method.
    private interface BeanStoreRetrievalStrategyTest {

        public void test(VaadinSession session, UIStore uiStore, UIID uiid);
    }

    private void beanStoreTest(BeanStoreRetrievalStrategyTest test) {
        beanStoreTest(test, true);
    }

    // Generic method that creates necessary VaadinSessin mock environment to be
    // able to test the BeanStoreRetrievalStrategy class.
    // To use it, call this method and write your test logic inside the
    // test method implemented from BeanStoreRetrievalStrategyTest interface.
    private synchronized void beanStoreTest(
            BeanStoreRetrievalStrategyTest test, boolean openVaadinSession) {
        WrappedSession wrappedSession = mock(WrappedSession.class);
        VaadinService vaadinService = mock(VaadinService.class);

        VaadinSession session = mock(VaadinSession.class);
        if (openVaadinSession) {
            when(session.getState()).thenReturn(VaadinSession.State.OPEN);
        } else {
            when(session.getState()).thenReturn(VaadinSession.State.CLOSED);
        }
        when(session.getSession()).thenReturn(wrappedSession);
        when(session.getService()).thenReturn(vaadinService);
        when(session.getSession().getId()).thenReturn(TEST_SESSION_ID);

        UIID uiid = new UIID(TEST_UIID);
        BeanStore beanStore = new BeanStore(TEST_BEAN_NAME);

        UIStore uiStore = mock(UIStore.class);
        when(session.getAttribute(UIStore.class)).thenReturn(uiStore);
        when(uiStore.getBeanStore(uiid)).thenReturn(beanStore);

        try {
            CurrentInstance.set(VaadinSession.class, session);
            CurrentInstance.set(UIID.class, uiid);

            test.test(session, uiStore, uiid);
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void throwsVaadinBeanStoreRetrievalStrategyNullVaadinSession() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("No VaadinSession bound to current thread");

        VaadinSessionBeanStoreRetrievalStrategy vaadinBSRS = new VaadinSessionBeanStoreRetrievalStrategy();
        vaadinBSRS.getBeanStore();
    }

    @Test
    public void throwsVaadinBeanStoreRetrievalStrategyNotOpenVaadinSession() {

        beanStoreTest(new BeanStoreRetrievalStrategyTest() {
            @Override
            public void test(VaadinSession session, UIStore uiStore, UIID uiid) {
                thrown.expect(IllegalStateException.class);
                thrown.expectMessage("Current VaadinSession is not open");

                VaadinSessionBeanStoreRetrievalStrategy vaadinBSRS = new VaadinSessionBeanStoreRetrievalStrategy();
                vaadinBSRS.getBeanStore();
            }
        }, false);
    }

    @Test
    public void testVaadinBeanStoreRetrievalStrategyUIStoreInstance() {
        beanStoreTest(new BeanStoreRetrievalStrategyTest() {
            @Override
            public void test(VaadinSession session, UIStore uiStore, UIID uiid) {

                VaadinSessionBeanStoreRetrievalStrategy vaadinBSRS = new VaadinSessionBeanStoreRetrievalStrategy();

                // Make sure that UIStore is always the same instance
                assertSame(vaadinBSRS.getBeanStore(), vaadinBSRS.getBeanStore());
            }
        });
    }

    @Test
    public void testVaadinBeanStoreRetrievalStrategyGetConversationId() {
        beanStoreTest(new BeanStoreRetrievalStrategyTest() {
            @Override
            public void test(VaadinSession session, UIStore uiStore, UIID uiid) {
                VaadinSessionBeanStoreRetrievalStrategy vaadinBSRS = new VaadinSessionBeanStoreRetrievalStrategy();

                assertTrue(vaadinBSRS.getConversationId().contains(
                        TEST_SESSION_ID));
                assertTrue(vaadinBSRS.getConversationId().contains(
                        String.valueOf(TEST_UIID)));
            }
        });
    }

    // TODO: Tests for UIStore and UIBeanStore

    @After
    public void validate() {
        Mockito.validateMockitoUsage();
    }
}
