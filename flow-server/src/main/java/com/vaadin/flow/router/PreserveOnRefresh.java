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
package com.vaadin.flow.router;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a previous view instance should be-reused when reloading a
 * location in the same browser window/tab.
 * <p>
 * This annotation can also be used, when exporting embeddable web components.
 * Place this annotation onto the class extending
 * {@link com.vaadin.flow.component.WebComponentExporter}. This flags all the
 * embedded components from that exporter to be preserved on refresh. Due to
 * the challenge of uniquely identifying embedded components through refresh.
 * When embedded, the component is identified by window name and a generated
 * component id. This means, that if the same component instance is embedded
 * onto two pages within the same window context, the state can be
 * transferred between locations. To avoid state leaking, provide unique id
 * for the embedded web component. The id must be unique across all the pages
 * where instances of the web component are embedded.
 *
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface PreserveOnRefresh {
}
