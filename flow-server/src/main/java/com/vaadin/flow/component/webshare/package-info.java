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
 * Server-side access to the browser's Web Share API, which opens the device's
 * native share sheet so the user can send a title, text, and/or URL to another
 * app.
 * <p>
 * Use
 * {@link com.vaadin.flow.component.webshare.WebShare#onClick(com.vaadin.flow.component.Component)
 * WebShare.onClick(component)} to share when a component is clicked, building
 * the payload with {@link com.vaadin.flow.component.webshare.ShareContent}, and
 * {@link com.vaadin.flow.component.webshare.WebShare#supportSignal()} to check
 * whether sharing is available before showing a share button.
 * <p>
 * Sharing goes through {@code onClick} rather than an ordinary server-side
 * click listener because the browser only opens the share sheet while it is
 * handling a genuine user gesture, which is no longer valid by the time a
 * server round trip completes. The Web Share API also requires a secure context
 * (HTTPS or {@code localhost}) and is not available on every desktop browser.
 */
@NullMarked
package com.vaadin.flow.component.webshare;

import org.jspecify.annotations.NullMarked;
