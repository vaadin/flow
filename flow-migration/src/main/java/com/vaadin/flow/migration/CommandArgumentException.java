/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.migration;

import java.util.Optional;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * An exception is thrown if there is an error in command line arguments.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
class CommandArgumentException extends Exception {

    private final Options options;

    /**
     * Creates a new exception instance using provided command line
     * {@code options} and the parse exception {@code cause}.
     *
     * @param options
     *            command line options
     * @param cause
     *            the parse exception cause
     */
    CommandArgumentException(Options options, ParseException cause) {
        super(cause);
        this.options = options;
    }

    /**
     * Creates a new exception instance using a {@code cause}.
     *
     * @param cause
     *            the intitial exception cause
     */
    public CommandArgumentException(Exception cause) {
        super(cause);
        options = null;
    }

    /**
     * Returns the command line options.
     *
     * @return an optional options, or an empty optional if the instance is
     *         created with a cause only
     */
    Optional<Options> getOptions() {
        return Optional.ofNullable(options);
    }
}
