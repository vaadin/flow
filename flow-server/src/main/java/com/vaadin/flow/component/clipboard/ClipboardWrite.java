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
package com.vaadin.flow.component.clipboard;

import com.vaadin.flow.shared.Registration;

/**
 * Registration returned from {@link ClipboardBinding#copyTextFrom},
 * {@link ClipboardBinding#copyHtmlFrom}, {@link ClipboardBinding#copyImageFrom}
 * and {@link ClipboardBinding#copyFrom}. {@linkplain Registration#remove()
 * Removing} the registration detaches the underlying trigger from its host
 * component.
 * <p>
 * Reserved as a distinct type so the API has room to grow (e.g. mutable
 * post-binding callbacks) without changing the return type of every
 * {@code copy*From} overload.
 */
public interface ClipboardWrite extends Registration {
}
