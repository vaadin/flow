package com.vaadin.signals;

import com.vaadin.signals.impl.SignalTree;

/**
 * Utility class for accessing/using signals internal API.
 * <p>
 * <strong>Note</strong>: This is internal API for Vaadin platform's internal
 * usages. It is not intended for public use and may change or be removed in
 * future releases.
 */
public class SignalUtils {

    /**
     * Returns the underlying <code>SignalTree</code> instance of the given
     * signal.
     *
     * @param signal
     *            the signal to get the tree of, not <code>null</code>
     * @return the signal tree instance, not <code>null</code>
     */
    public static SignalTree treeOf(Signal<?> signal) {
        return signal.tree();
    }

    /**
     * Checks whether the given command is considered valid by the validator
     * instance of the provided signal. In case of composite commands such as
     * transactions, this method will recursively check the validity of all
     * commands in the transaction.
     * <p>
     * <strong>Note</strong>: This only checks the validity of the commands that
     * might make changes to the data.
     *
     * @param command
     *            the command to check, not <code>null</code>
     * @param signal
     *            the signal to check the command against, not<code>null</code>
     * @return <code>true</code> if the command is valid, <code>false</code>
     *         otherwise
     */
    public static boolean isValid(SignalCommand command, Signal<?> signal) {
        return signal.isValid(command);
    }
}
