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
package com.vaadin.cdi.itest.routecontext;

import com.vaadin.cdi.itest.Counter;
import com.vaadin.cdi.util.BeanProvider;
import com.vaadin.flow.component.UI;

public interface CountedPerUI {

    default int getUiId() {
        return UI.getCurrent().getUIId();
    }

    default Counter getCounter() {
        return BeanProvider.getContextualReference(Counter.class);
    }

    default void countConstruct() {
        getCounter().increment(getClass().getSimpleName() + "C" + getUiId());
    }

    default void countDestroy() {
        if (UI.getCurrent() != null) {
            getCounter()
                    .increment(getClass().getSimpleName() + "D" + getUiId());
        }
    }

}
