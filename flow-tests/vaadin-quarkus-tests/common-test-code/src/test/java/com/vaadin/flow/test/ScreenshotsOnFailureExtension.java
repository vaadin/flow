/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.test;

import java.util.logging.Logger;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.runner.Description;
import org.mockito.Mockito;

import com.vaadin.testbench.ScreenshotOnFailureRule;

public class ScreenshotsOnFailureExtension
        implements TestExecutionExceptionHandler {

    private static class ScreenshotOnFailureRuleDelegate
            extends ScreenshotOnFailureRule {
        public ScreenshotOnFailureRuleDelegate(AbstractChromeIT test) {
            super(test);
        }

        @Override
        protected void failed(Throwable throwable, Description description) {
            super.failed(throwable, description);
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context,
            Throwable throwable) throws Throwable {
        if (!context.getTestInstance().isPresent()) {
            getLogger().warning(
                    "There is no test instance in the context, can't generate a screenshot");
            throw throwable;
        }
        Object object = context.getTestInstance().get();
        AbstractChromeIT test = (AbstractChromeIT) object;

        ScreenshotOnFailureRuleDelegate delegate = new ScreenshotOnFailureRuleDelegate(
                test);
        if (!context.getTestClass().isPresent()) {
            getLogger().warning(
                    "There is no test class in the context, can't generate a screenshot");
            throw throwable;
        }

        Description description;
        if (context.getTestMethod().isPresent()) {
            description = Description.createTestDescription(
                    context.getTestClass().get(),
                    context.getTestMethod().get().getName());
        } else {
            description = Mockito.mock(Description.class);
            Mockito.when(description.getDisplayName())
                    .thenReturn(context.getDisplayName());
        }
        delegate.failed(throwable, description);
        throw throwable;
    }

    private Logger getLogger() {
        return Logger.getLogger(ScreenshotsOnFailureExtension.class.getName());
    }
}
