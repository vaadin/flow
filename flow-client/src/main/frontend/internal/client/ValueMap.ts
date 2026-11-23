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

// TypeScript port of com.vaadin.client.ValueMap.
//
// ValueMap.java is a GWT JavaScriptObject overlay: it adds five native accessors
// (getInt, getString, getJSStringArray, containsKey, getValueMap) over a parsed
// JSON object without changing its runtime representation. In TypeScript that
// object is the value itself, so those accessors are property reads and index
// checks at the call site, and only the type is ported. Adding wrappers for them
// would be API this port has no caller for.

/** Old abstraction for a UIDL JSON message. */
export type ValueMap = Record<string, unknown>;
