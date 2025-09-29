package com.vaadin.signals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.signals.impl.SignalTree;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class SignalUtilsTest {

    @Test
    void treeOf_returnsSignalsUnderlyingTree() {
        SignalTree tree = Mockito.mock(SignalTree.class);
        AbstractSignal<?> signal = Mockito.mock(AbstractSignal.class);
        Mockito.when(signal.tree()).thenReturn(tree);
        assertSame(tree, SignalUtils.treeOf(signal));
        Mockito.verify(signal, Mockito.times(1)).tree();

    }

    @Test
    void isValid_callsSignalsIsValid() {
        AbstractSignal<?> signal = Mockito.mock(AbstractSignal.class);
        SignalCommand command = TestUtil.writeRootValueCommand();
        Mockito.when(signal.isValid(any())).thenReturn(true);
        assertTrue(SignalUtils.isValid(signal, command));
        Mockito.verify(signal, Mockito.times(1)).isValid(command);
    }
}
