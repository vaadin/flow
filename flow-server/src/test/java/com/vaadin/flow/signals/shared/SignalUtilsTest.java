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
package com.vaadin.flow.signals.shared;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.TestUtil;
import com.vaadin.flow.signals.shared.impl.SignalTree;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignalUtilsTest {

    @Test
    void treeOf_returnsSignalsUnderlyingTree() {
        SignalTree tree = mock(SignalTree.class);
        AbstractSharedSignal<?> signal = mock(AbstractSharedSignal.class);
        when(signal.tree()).thenReturn(tree);
        assertSame(tree, SignalUtils.treeOf(signal));
        verify(signal, times(1)).tree();

    }

    @Test
    void isValid_callsSignalsIsValid() {
        AbstractSharedSignal<?> signal = mock(AbstractSharedSignal.class);
        SignalCommand command = TestUtil.writeRootValueCommand();
        when(signal.isValid(any())).thenReturn(true);
        assertTrue(SignalUtils.isValid(signal, command));
        verify(signal, times(1)).isValid(command);
    }
}
