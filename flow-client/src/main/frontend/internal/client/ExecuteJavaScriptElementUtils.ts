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

interface CustomElementWithProperties {
  constructor?: {
    properties?: Record<string, { value?: unknown } | undefined>;
  };
}

/**
 * The single migrated JSNI helper from
 * `com.vaadin.client.ExecuteJavaScriptElementUtils`. The orchestration logic
 * (attachExistingElement, populateModelProperties, etc.) stays Java.
 */
export const ExecuteJavaScriptElementUtils = {
  isPropertyDefined(node: Node, property: string): boolean {
    const ctorProps = (node as CustomElementWithProperties).constructor?.properties;
    const descriptor = ctorProps?.[property];
    return !!descriptor && typeof descriptor.value !== 'undefined';
  }
};
