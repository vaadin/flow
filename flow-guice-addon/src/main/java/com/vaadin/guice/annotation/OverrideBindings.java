/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.guice.annotation;

import com.google.inject.Module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Annotation to be placed on {@link com.google.inject.Module}-classes if they are to 'overwrite'
 * bindings from other modules.
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 * @see com.google.inject.util.Modules#override(Module...) <p>
 * <pre>
 * &#064;OverrideBindings
 * public class OverwritingModule extends AbstractModule {
 * }
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
public @interface OverrideBindings {
}
