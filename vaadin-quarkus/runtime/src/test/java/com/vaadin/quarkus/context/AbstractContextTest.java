/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Basic tests for all custom abstract contexts.
 * 
 * @param <C>
 *            a context type
 */
public abstract class AbstractContextTest<C extends AbstractContext>
        extends InjectableContextTest<C> {

    @Test
    public void destroyAllActive_beanExistsInContext_beanDestroyed() {
        createContext().activate();

        TestBean referenceA = getContext().get(contextual, creationalContext);
        referenceA.setState("hello");

        getContext().destroyAllActive();
        assertEquals(1, getDestroyedBeans().size());
    }

    @Test
    public void destroyAllActive_severalContexts_beanDestroyed() {
        UnderTestContext contextUnderTestA = createContext();
        contextUnderTestA.activate();
        TestBean referenceA = getContext().get(contextual, creationalContext);

        ContextualStorage storageA = getContext()
                .getContextualStorage(contextual, false);

        referenceA.setState("hello");

        final UnderTestContext contextUnderTestB = createContext();
        contextUnderTestB.activate();
        TestBean referenceB = getContext().get(contextual, creationalContext);

        referenceB.setState("foo");

        ContextualStorage storageB = getContext()
                .getContextualStorage(contextual, false);

        assertEquals(2, getCreatedBeansCount());

        AbstractContext.destroyAllActive(storageA);
        assertEquals(1, getDestroyedBeans().size());

        AbstractContext.destroyAllActive(storageB);
        assertEquals(2, getDestroyedBeans().size());
    }

}
