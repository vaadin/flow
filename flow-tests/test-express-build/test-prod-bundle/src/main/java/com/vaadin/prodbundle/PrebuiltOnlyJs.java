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
package com.vaadin.prodbundle;

import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * A definition that only the pre-built bundle carries: its class is removed
 * from the jar, so an application that uses this bundle does not have it. Such
 * an application should still be served by the bundle.
 */
@JsDefinition
public interface PrebuiltOnlyJs extends JsInvokers {

    // NoAppBundleIT of test-prod-bundle-no-plugin looks for this function
    @JsExpression("window.prebuiltOnly = $0")
    void mark(String value);
}
