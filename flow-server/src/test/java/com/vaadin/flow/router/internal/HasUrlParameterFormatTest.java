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

package com.vaadin.flow.router.internal;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;

public class HasUrlParameterFormatTest {

    @Test
    public void getTemplate_urlBaseWithOptionalParameterView_templateAdded() {
        String template = HasUrlParameterFormat.getTemplate(
                "test-view-with-optional-parameter",
                TestViewWithOptionalParameter.class);
        MatcherAssert.assertThat(template, CoreMatchers.equalTo(
                "test-view-with-optional-parameter/:___url_parameter?"));
    }

    @Test
    public void getTemplate_urlBaseWithWildcardParameterView_templateAdded() {
        String template = HasUrlParameterFormat.getTemplate(
                "test-view-with-wildcard-parameter",
                TestViewWithWildcardParameter.class);
        MatcherAssert.assertThat(template, CoreMatchers.equalTo(
                "test-view-with-wildcard-parameter/:___url_parameter*(^[+-]?[0-8]?[0-9]{1,18}$)"));
    }

    @Test
    public void getTemplate_urlBaseWithMandatoryParameterView_templateAdded() {
        String template = HasUrlParameterFormat.getTemplate(
                "test-view-with-mandatory-parameter",
                TestViewWithMandatoryParameter.class);
        MatcherAssert.assertThat(template, CoreMatchers.equalTo(
                "test-view-with-mandatory-parameter/:___url_parameter(^true|false$)"));
    }

    @Test
    public void getTemplate_urlBaseWithTemplate_noExtraTemplateAdded() {
        String template = HasUrlParameterFormat.getTemplate(
                "test-view-with-optional-parameter/:___url_parameter?",
                TestViewWithOptionalParameter.class);
        MatcherAssert.assertThat(template, CoreMatchers.equalTo(
                "test-view-with-optional-parameter/:___url_parameter?"));
    }

    @Test
    public void getTemplate_urlBaseWithNoUrlParameter_noTemplateAdded() {
        String template = HasUrlParameterFormat.getTemplate(
                "test-view-no-parameters", TestViewWithNoParameter.class);
        MatcherAssert.assertThat(template,
                CoreMatchers.equalTo("test-view-no-parameters"));
    }

    @Route("test-view-with-optional-parameter")
    private static class TestViewWithOptionalParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter String parameter) {
        }
    }

    @Route("test-view-with-wildcard-parameter")
    private static class TestViewWithWildcardParameter extends Component
            implements HasUrlParameter<Long> {

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter Long parameter) {
        }
    }

    @Route("test-view-with-mandatory-parameter")
    private static class TestViewWithMandatoryParameter extends Component
            implements HasUrlParameter<Boolean> {

        @Override
        public void setParameter(BeforeEvent event, Boolean parameter) {
        }
    }

    @Route("test-view-no-parameters")
    private static class TestViewWithNoParameter extends Component {
    }

}
