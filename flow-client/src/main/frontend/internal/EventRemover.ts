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

// TypeScript port of elemental.events.EventRemover, the GWT elemental primitive
// returned by the reactive listener-registration APIs. It is not part of the
// com.vaadin.client.flow.reactive package, so it lives here as a standalone
// elemental primitive next to the other elemental-style ports (e.g. JsArray).

/** Mirrors elemental.events.EventRemover. */
export interface EventRemover {
  remove(): void;
}
