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
 * Typed access to the Java classes that the GWT-compiled flow-client bundle
 * exports to JavaScript through JsInterop.
 *
 * Exported classes are published under `window.Vaadin.Flow.internal.<Name>` and
 * only exist after the bundle's `init()` (see `FlowClient.js`) has run. As the
 * client engine is migrated from Java to TypeScript (see
 * `MIGRATION_STRATEGY.md`), the new TypeScript calls *into* these exports — the
 * boundary is always TypeScript → GWT, never the reverse.
 *
 * Each exported class gets a typed interface and an accessor here so callers
 * never reach into `window` untyped. `JsInteropProbe` is the initial smoke-test
 * entry and is removed once the first real class has been migrated.
 */

/** Mirrors `com.vaadin.client.JsInteropProbe`. */
export interface JsInteropProbe {
  echo(value: string): string;
}

interface FlowInternalExports {
  JsInteropProbe: JsInteropProbe;
}

function flowInternalExports(): FlowInternalExports {
  const internal = (
    window as unknown as {
      Vaadin?: { Flow?: { internal?: FlowInternalExports } };
    }
  ).Vaadin?.Flow?.internal;
  if (!internal) {
    throw new Error(
      'flow-client JsInterop exports are not available. The FlowClient.js ' +
        'bundle init() must run before accessing window.Vaadin.Flow.internal.*'
    );
  }
  return internal;
}

/** Returns the exported `com.vaadin.client.JsInteropProbe`. */
export function jsInteropProbe(): JsInteropProbe {
  return flowInternalExports().JsInteropProbe;
}
