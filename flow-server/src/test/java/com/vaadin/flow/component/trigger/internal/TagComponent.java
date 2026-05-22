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
package com.vaadin.flow.component.trigger.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * Minimal {@link Component} subclass with an explicit tag, for tests that need
 * a server-side component but no behaviour. {@code @Tag} can't be set
 * dynamically, so this class takes the tag as a constructor argument and builds
 * its root element directly.
 */
class TagComponent extends Component {

    TagComponent(String tag) {
        super(new Element(tag));
    }
}
