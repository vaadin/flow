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

package com.vaadin.flow.webcomponent;

import com.vaadin.testbench.HasElementQuery;
import com.vaadin.testbench.TestBenchElement;

public interface HasById {
    default TestBenchElement byId(String id, String... childIds) {
        HasElementQuery _this = (HasElementQuery) this;
        return byId(_this, id, childIds);
    }

    default TestBenchElement byId(HasElementQuery elementQuery, String id,
            String... childIds) {
        TestBenchElement testBenchElement = elementQuery
                .$(TestBenchElement.class).id(id);
        if (testBenchElement != null && childIds.length > 0) {
            for (String childId : childIds) {
                testBenchElement = testBenchElement.$(TestBenchElement.class)
                        .id(childId);
                if (testBenchElement == null) {
                    return null;
                }
            }
        }
        return testBenchElement;
    }
}
