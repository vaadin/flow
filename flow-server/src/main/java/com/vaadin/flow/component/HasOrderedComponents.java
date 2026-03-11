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
package com.vaadin.flow.component;

/**
 * A component which the children components are ordered, so the index of each
 * child matters for the layout.
 * <p>
 * Note: The default methods have been moved to {@link HasComponents}, so that
 * they are available for all components, not just those implementing
 * {@link HasOrderedComponents}. This interface is left for backward
 * compatibility, but it is not needed anymore.
 *
 * @since 1.0
 * @deprecated since 24.10.0, for removal in 26.0.0.
 */
@Deprecated(since = "24.10.0", forRemoval = true)
public interface HasOrderedComponents extends HasComponents {

}
