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

import org.junit.After;
import org.junit.Before;

import com.vaadin.flow.server.VaadinService;

import static org.junit.Assert.assertNull;

/**
 * Helper for test classes that need to have {@code VaadinService.getCurrent()}
 * populated.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class HasCurrentService {
    // Store the service to prevent it from being garbage collected while the
    // test is running
    private VaadinService service;

    @Before
    public void setUpCurrentService() {
        clearCurrentService();
        assertNull(VaadinService.getCurrent());

        service = createService();
        VaadinService.setCurrent(service);
    }

    protected abstract VaadinService createService();

    @After
    public void clearCurrentService() {
        VaadinService.setCurrent(null);
        service = null;
    }
}
