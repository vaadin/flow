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
package com.vaadin.flow.server;

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectTools;

public class MockInstantiator implements Instantiator {

    private VaadinServiceInitListener[] serviceInitListeners;

    public MockInstantiator(VaadinServiceInitListener... serviceInitListeners) {
        this.serviceInitListeners = serviceInitListeners;
    }

    @Override
    public boolean init(VaadinService service) {
        return true;
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        return Stream.of(serviceInitListeners);
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        return ReflectTools.createInstance(type);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return ReflectTools.createInstance(componentClass);
    }
}
