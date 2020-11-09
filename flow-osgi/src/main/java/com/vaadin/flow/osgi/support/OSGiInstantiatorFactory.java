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
package com.vaadin.flow.osgi.support;

import java.util.stream.Stream;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.osgi.service.component.annotations.Component;

/**
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(immediate = true)
public class OSGiInstantiatorFactory implements InstantiatorFactory {

    private static final class OsgiInstantiator extends DefaultInstantiator
    implements Instantiator {

        private final Lookup lookup;

        private OsgiInstantiator(VaadinService service) {
            super(service);

            lookup = service.getContext().getAttribute(Lookup.class);
        }

        @Override
        public Stream<VaadinServiceInitListener> getServiceInitListeners() {
            return lookup.lookupAll(VaadinServiceInitListener.class).stream();
        }

        @Override
        public I18NProvider getI18NProvider() {
            I18NProvider provider = super.getI18NProvider();
            if (provider == null) {
                provider = lookup.lookup(I18NProvider.class);
            }
            return provider;
        }
    }

    @Override
    public Instantiator createInstantitor(VaadinService service) {
        return new OsgiInstantiator(service);
    }

}
