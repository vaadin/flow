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
package com.vaadin.flow.server.dau;

import com.vaadin.pro.licensechecker.dau.EnforcementException;

/**
 * A DauEnforcementException is thrown when License Server imposes enforcement
 * for the application and the EnforcementRule check is not satisfied.
 * <p>
 * Wraps License Checker exception to simplify integration with Hilla and
 * add-ons.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.5
 */
public class DauEnforcementException extends RuntimeException {

    public DauEnforcementException(EnforcementException cause) {
        super(cause);
    }
}
