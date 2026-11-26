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
package com.vaadin.flow.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows the annotated method or class to call APIs that the forbiddenapis
 * Maven plugin reports, such as the implementation of a deprecated API that has
 * to keep sending JavaScript as a string. The signatures are in the
 * <code>forbidden-apis</code> directory of the Flow repository.
 * <p>
 * The annotation does not cover the anonymous and nested classes of the
 * annotated element, nor its serializable lambdas, such as listeners. Move such
 * a call into a small annotated method instead.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface SuppressForbidden {
}
