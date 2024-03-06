/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.page;

import java.io.Serializable;

/**
 * Provides method for configuring the loading indicator.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface LoadingIndicatorConfiguration extends Serializable {
    /**
     * Sets the delay before the loading indicator is shown. The default is
     * 300ms.
     *
     * @param firstDelay
     *            The first delay (in ms)
     */
    void setFirstDelay(int firstDelay);

    /**
     * Returns the delay before the loading indicator is shown.
     *
     * @return The first delay (in ms)
     */
    int getFirstDelay();

    /**
     * Sets the delay before the loading indicator goes into the "second" state.
     * The delay is calculated from the time when the loading indicator was
     * triggered. The default is 1500ms.
     *
     * @param secondDelay
     *            The delay before going into the "second" state (in ms)
     */
    void setSecondDelay(int secondDelay);

    /**
     * Returns the delay before the loading indicator goes into the "second"
     * state. The delay is calculated from the time when the loading indicator
     * was triggered.
     *
     * @return The delay before going into the "second" state (in ms)
     */
    int getSecondDelay();

    /**
     * Sets the delay before the loading indicator goes into the "third" state.
     * The delay is calculated from the time when the loading indicator was
     * triggered. The default is 5000ms.
     *
     * @param thirdDelay
     *            The delay before going into the "third" state (in ms)
     */
    void setThirdDelay(int thirdDelay);

    /**
     * Returns the delay before the loading indicator goes into the "third"
     * state. The delay is calculated from the time when the loading indicator
     * was triggered.
     *
     * @return The delay before going into the "third" state (in ms)
     */
    int getThirdDelay();

    /**
     * Returns whether the default theming is applied for the loading indicator,
     * making it visible for users.
     * <p>
     * By default, it is shown ({@code true}) and there is a progress bar on top
     * of the viewport shown after a delay to the users while there is an active
     * server request in process.
     *
     * @return {@code true} for applying default theme, {@code false} for not
     */
    boolean isApplyDefaultTheme();

    /**
     * Sets whether the default theming is applied for the loading indicator.
     * <p>
     * By default, it is shown ({@code true}) and there is a progress bar on top
     * of the viewport shown after a delay to the users while there is an active
     * server request in process.
     *
     * @param applyDefaultTheme
     *            {@code true} to apply default theming, {@code false} for not
     */
    void setApplyDefaultTheme(boolean applyDefaultTheme);
}
