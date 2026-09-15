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
/**
 * The javaagent side of the dev loop: the only code from this module that is
 * ever loaded into the application JVM, there to hand its
 * {@link java.lang.instrument.Instrumentation} handle to the in-app dev-loop
 * connector.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
package com.vaadin.flow.devloop.agent;
