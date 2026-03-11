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
package com.vaadin.flow.component.page;

import java.io.Serializable;

/**
 * Represents the current screen orientation state, including the orientation
 * type and the angle of rotation.
 *
 * @param type
 *            the screen orientation type
 * @param angle
 *            the screen orientation angle in degrees
 *
 * @author Vaadin Ltd
 */
public record ScreenOrientationData(ScreenOrientation type,
        int angle) implements Serializable {
}
