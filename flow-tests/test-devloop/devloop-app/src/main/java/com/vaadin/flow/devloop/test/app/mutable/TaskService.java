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
package com.vaadin.flow.devloop.test.app.mutable;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * A Spring bean, so a structural change to it has to escalate to a restart.
 * <p>
 * Both routes to that verdict go through this class, and which one is taken
 * depends on the JVM:
 * <ul>
 * <li>On a stock JVM, adding a member is rejected outright by
 * {@code redefineClasses}, and the apply escalates with the JVM's own
 * reason.</li>
 * <li>On a JVM with enhanced class redefinition the redefine is accepted, and
 * then the {@code @Service} annotation is what says the change cannot be live:
 * whatever Spring built from the old shape - a proxy, an injection point, a
 * derived query - was built once, at startup.</li>
 * </ul>
 * It is injected into the view so that it is certainly loaded in the running
 * JVM: a class nothing loaded has nothing to redefine, and an apply over it
 * would honestly report success while proving nothing.
 */
@Service
public class TaskService {

    public List<String> list() {
        return List.of("Write the plan", "Land the plan");
    }
}
