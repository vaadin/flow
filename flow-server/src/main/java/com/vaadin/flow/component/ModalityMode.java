/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component;

import com.vaadin.flow.dom.DomListenerRegistration;

/**
 * Dialog modality mode.
 */
public enum ModalityMode {

    /**
     * Doesn’t use modality at all. No modality curtain, focus trap, pointer
     * event blocking, and request blocking.
     */
    MODELESS,
    /**
     * A visual curtain dims the background behind the dialog, trapping focus
     * and blocking pointer events. Background components can't receive requests
     * unless explicitly enabled via low-level APIs like
     * {@link DomListenerRegistration#allowInert()}.
     */
    STRICT,
    /**
     * Behaves like {@link #STRICT} except that request are not blocked.
     */
    VISUAL
}
