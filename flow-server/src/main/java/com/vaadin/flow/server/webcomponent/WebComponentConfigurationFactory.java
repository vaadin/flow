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
package com.vaadin.flow.server.webcomponent;

import java.io.Serializable;
import java.util.function.Function;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Produces a web component configuration for the exporter class.
 *
 * @author Vaadin Ltd
 *
 */
public class WebComponentConfigurationFactory implements
        Function<Class<? extends WebComponentExporter<? extends Component>>, WebComponentConfiguration<? extends Component>>,
        Serializable {

    @Override
    public WebComponentConfiguration<? extends Component> apply(
            Class<? extends WebComponentExporter<? extends Component>> clazz) {
        WebComponentExporter<? extends Component> exporter = ReflectTools
                .createInstance(clazz);

        return new WebComponentConfigurationImpl<>(exporter);
    }

}
