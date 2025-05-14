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
package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.misc.ui.NpmThemedComponentView")
@Tag("npm-themed-component")
// `src/` in component should be replaced by `legacyTheme/my-theme`
@JsModule("./src/npm-themed-component.js")
/*
 * Note that this is using component instead of polymer template, because
 * otherwise the themed module would have to import the original /src module,
 * and that would make testing the actual feature here (theme rewrite) more
 * complex.
 */
public class NpmThemedComponentView extends Component {

}
