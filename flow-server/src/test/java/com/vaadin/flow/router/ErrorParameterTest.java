/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.router;

import org.junit.Assert;
import org.junit.Test;

public class ErrorParameterTest {
    @Test
    public void matchingExceptionType() {
        NullPointerException exception = new NullPointerException();

        ErrorParameter<NullPointerException> errorParameter = new ErrorParameter<>(
                NullPointerException.class, exception);

        Assert.assertSame(exception, errorParameter.getException());
        Assert.assertSame(exception, errorParameter.getCaughtException());
    }

    @Test
    public void superExceptionType() {
        NullPointerException exception = new NullPointerException();

        ErrorParameter<RuntimeException> errorParameter = new ErrorParameter<>(
                RuntimeException.class, exception);

        Assert.assertSame(exception, errorParameter.getException());
        Assert.assertSame(exception, errorParameter.getCaughtException());
    }

    @Test
    public void matchingCauseType() {
        NullPointerException cause = new NullPointerException();
        IllegalStateException exception = new IllegalStateException(cause);

        ErrorParameter<NullPointerException> errorParameter = new ErrorParameter<>(
                NullPointerException.class, exception);

        Assert.assertSame(cause, errorParameter.getException());
        Assert.assertSame(exception, errorParameter.getCaughtException());
    }

    @Test
    public void superMatchingCauseType() {
        NullPointerException cause = new NullPointerException() {

        };
        IllegalStateException exception = new IllegalStateException(cause);

        ErrorParameter<NullPointerException> errorParameter = new ErrorParameter<>(
                NullPointerException.class, exception);

        Assert.assertSame(cause, errorParameter.getException());
        Assert.assertSame(exception, errorParameter.getCaughtException());
    }

}
