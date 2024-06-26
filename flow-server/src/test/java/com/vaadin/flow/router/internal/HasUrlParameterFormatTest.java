/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.router.internal;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
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

    @Test()
    public void getTemplate_urlBaseWithTemplate_throws() {
        Assert.assertThrows(
                "Cannot create an url with parameter template, because the "
                        + "given url already have that template: "
                        + "test-view-with-optional-parameter/:___url_parameter?",
                IllegalArgumentException.class,
                () -> HasUrlParameterFormat.getTemplate(
                        "test-view-with-optional-parameter/:___url_parameter?",
                        TestViewWithOptionalParameter.class));
    }

    @Test()
    public void getTemplate_notImplementsHasUrlParameter_urlNotChanged() {
        String template = HasUrlParameterFormat.getTemplate(
                "test-view-no-parameters", TestViewWithNoParameter.class);
        MatcherAssert.assertThat(template,
                CoreMatchers.equalTo("test-view-no-parameters"));
    }

    @Test
    public void hasUrlParameterTemplate_noTemplate_returnFalse() {
        Assert.assertFalse(
                HasUrlParameterFormat.hasUrlParameterTemplate("foo/bar"));
    }

    @Test
    public void hasUrlParameterTemplate_emptyUrl_returnFalse() {
        Assert.assertFalse(HasUrlParameterFormat.hasUrlParameterTemplate(null));
    }

    @Test
    public void hasUrlParameterTemplate_hasTemplate_returnTrue() {
        Assert.assertTrue(HasUrlParameterFormat
                .hasUrlParameterTemplate("foo/bar/:___url_parameter"));
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
