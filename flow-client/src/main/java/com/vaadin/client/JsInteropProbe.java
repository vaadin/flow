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
package com.vaadin.client;

import jsinterop.annotations.JsType;

/**
 * Smoke test for the GWT-to-TypeScript JsInterop export bridge that the
 * flow-client migration relies on.
 * <p>
 * Because it is annotated with {@link JsType} and the GWT compiler is run with
 * {@code generateJsInteropExports} enabled, this class is exported to
 * {@code window.Vaadin.Flow.internal.JsInteropProbe}, where TypeScript can call
 * it. Being an export also makes it a compilation root, so it is retained even
 * though no Java code references it — the same property that keeps a migrated
 * class's exported dependencies alive once its Java callers are gone.
 * <p>
 * It carries no production logic and exists only to prove, in CI, that the
 * export mechanism works end to end. It can be removed once the first real
 * class has been migrated to TypeScript and exercises the same path.
 *
 * @author Vaadin Ltd
 */
@JsType(namespace = "Vaadin.Flow.internal", name = "JsInteropProbe")
public final class JsInteropProbe {

    private JsInteropProbe() {
        // Only the static method is exported; not meant to be instantiated.
    }

    /**
     * Returns the given value prefixed with {@code flow-client:}, letting a
     * caller verify both that the export is reachable and that the argument and
     * return value cross the JsInterop boundary intact.
     *
     * @param value
     *            the value to echo back
     * @return the value prefixed with {@code flow-client:}
     */
    public static String echo(String value) {
        return "flow-client:" + value;
    }
}
