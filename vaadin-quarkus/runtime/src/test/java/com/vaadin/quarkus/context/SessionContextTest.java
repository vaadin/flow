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
package com.vaadin.quarkus.context;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.VaadinSession;

@QuarkusTest
public class SessionContextTest
        extends AbstractContextTest<VaadinSessionScopedContext> {

    @Test
    public void getContextualStorage_noCurrentSession_answersNull() {
        // Arc destroys a context outside any Vaadin session thread, and that
        // goes through destroyAllActive() rather than through the get methods,
        // so it runs without their checkActive(). With no session there is no
        // storage and none can be created either, which has to be answered
        // rather than read off the absent session.
        createContext().activate();
        getContext().get(contextual, creationalContext);

        VaadinSession.setCurrent(null);

        Assertions.assertNull(
                getContext().getContextualStorage(contextual, false));
        Assertions.assertNull(
                getContext().getContextualStorage(contextual, true));
        getContext().destroyAllActive();
        // getState() is the sibling path Arc reaches the same way, and it
        // walks the same storages, so it has to tolerate the absent one too.
        Assertions.assertTrue(
                getContext().getState().getContextualInstances().isEmpty());
    }

    @Override
    protected UnderTestContext newContextUnderTest() {
        return new SessionUnderTestContext();
    }

    @Override
    protected Class<VaadinSessionScopedContext> getContextType() {
        return VaadinSessionScopedContext.class;
    }

}
