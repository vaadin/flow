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
package com.vaadin.flow.component.screenorientation;

import java.io.Serializable;

/**
 * The current screen orientation, as reported by
 * {@link ScreenOrientation#orientationSignal()}: both its
 * {@link ScreenOrientationType type} (portrait or landscape, primary or
 * secondary) and its rotation angle.
 *
 * @param type
 *            the screen orientation type; never {@code null}
 * @param angle
 *            how far the screen is rotated from its natural orientation,
 *            clockwise in degrees (typically {@code 0}, {@code 90},
 *            {@code 180}, or {@code 270}). The natural orientation is
 *            device-dependent: on a phone it is usually portrait, on many
 *            tablets and desktops landscape, so the same angle can mean
 *            different orientations on different devices — use {@link #type()}
 *            to reason about the orientation itself.
 */
public record ScreenOrientationData(ScreenOrientationType type,
        int angle) implements Serializable {
}
