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

// TypeScript port of com.vaadin.client.communication.RequestStartingEvent. The GWT
// Event/EventBus plumbing it inherits (`getType`, `getAssociatedType`,
// `dispatch`) has no counterpart here: RequestResponseTracker notifies its
// handler lists directly, so only the event and its handler are ported.

/**
 * Event fired when a request starts.
 */
// eslint-disable-next-line @typescript-eslint/no-extraneous-class -- the Java event carries no data either, and the class exists so each fire allocates its own
export class RequestStartingEvent {}

/**
 * Handler for {@link RequestStartingEvent}s.
 *
 * @param requestStartingEvent - the event object
 */
export type RequestStartingEventHandler = (requestStartingEvent: RequestStartingEvent) => void;
