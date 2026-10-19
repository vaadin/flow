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

/**
 * A plain class, and deliberately not a bean of any kind.
 * <p>
 * This is the half of the dev loop that the Spring fixture cannot test. There,
 * a structural change to a service escalates to a restart because something
 * built a proxy from the old shape at startup. Here there is no container to
 * have done that, so the only thing standing between a structural edit and a
 * live application is what the JVM itself will accept - which is the honest
 * baseline, and the one a servlet-container project actually runs on.
 * <p>
 * It is constructed by the view rather than injected, because a plain servlet
 * application has no dependency injection: Flow instantiates a route target
 * through its no-argument constructor.
 */
public class TaskService {

    public List<String> list() {
        return List.of("Write the plan", "Land the plan");
    }
}
