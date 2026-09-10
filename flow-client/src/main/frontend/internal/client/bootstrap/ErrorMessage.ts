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

// TypeScript port of com.vaadin.client.bootstrap.ErrorMessage.
//
// The Java class is a JavaScriptObject overlay whose four native accessors read
// the fields of the object the bootstrap wrote into the page. In TypeScript that
// object is the value itself, so the accessors are property reads at the call
// site and only the type is ported, as for ValueMap.

/**
 * Wraps a native javascript object containing fields for an error message
 */
export interface ErrorMessage {
  /** The error caption, as written by the server bootstrap. */
  caption?: string;

  /** The error message. */
  message?: string;

  /** The URL to navigate to when the message is dismissed. */
  url?: string;

  /** The selector of the element the message is shown in. */
  querySelector?: string;
}
