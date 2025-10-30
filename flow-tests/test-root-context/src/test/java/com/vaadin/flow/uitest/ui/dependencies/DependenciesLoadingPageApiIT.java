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
package com.vaadin.flow.uitest.ui.dependencies;

import org.junit.Ignore;

/**
 * See {@link DependenciesLoadingAnnotationsIT} for more details about the test.
 * Test runs and performs the same checks as
 * {@link DependenciesLoadingAnnotationsIT}, but this test opens a different
 * page to test, that's why the class exists and needed.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 * @see DependenciesLoadingAnnotationsIT
 */
@Ignore("Doesn't work ccdm, see https://github.com/vaadin/flow/issues/7328")
public class DependenciesLoadingPageApiIT
        extends DependenciesLoadingAnnotationsIT {

    @Override
    protected String getCssSuffix() {
        return "WebRes";
    }
}
