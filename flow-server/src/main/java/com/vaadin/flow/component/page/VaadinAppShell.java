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
package com.vaadin.flow.component.page;

import java.io.Serializable;

/**
 * A marker class to configure the `index.hml` page when in client-side
 * bootstrapping.
 *
 * There should be at max one class extending {@link VaadinAppShell} in the
 * application?
 *
 * No other annotations are allowed on any other classes in the application.
 *
 * <p>
 *
 * <code>
 * &#64;Meta(name = "Author", content = "Donald Duck")
 * public class AppShell extends VaadinAppShell {
 * }
 * </code>
 *
 * @since 3.0
 */
public class VaadinAppShell implements Serializable {
}
