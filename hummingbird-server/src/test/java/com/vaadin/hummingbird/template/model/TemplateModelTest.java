package com.vaadin.hummingbird.template.model;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.ui.Template;

public class TemplateModelTest {

    public interface MyModel extends TemplateModel {

    }

    static class TestTemplate extends Template {
        TestTemplate() {
            super(new ByteArrayInputStream(
                    "<div>foo</div>".getBytes(StandardCharsets.UTF_8)));
        }

        @Override
        protected MyModel getModel() {
            return (MyModel) super.getModel();
        }
    }

    static class NoModelTemplate extends Template {
        public NoModelTemplate() {
            super(new ByteArrayInputStream(
                    "<div>foo</div>".getBytes(StandardCharsets.UTF_8)));
        }

    }

    @Test
    public void testTemplateModelTypeReading() {
        Class<? extends TemplateModel> templateModelType = TemplateModelTypeParser
                .getType(NoModelTemplate.class);

        Assert.assertEquals(TemplateModel.class, templateModelType);

        templateModelType = TemplateModelTypeParser.getType(TestTemplate.class);

        Assert.assertEquals(MyModel.class, templateModelType);
    }

    @Test
    public void testTemplateModelTypeCache() {
        TemplateModelTypeParser.cache.clear();

        Class<? extends TemplateModel> first = TemplateModelTypeParser
                .getType(TestTemplate.class);
        Class<? extends TemplateModel> second = TemplateModelTypeParser
                .getType(TestTemplate.class);

        Assert.assertSame(first, second);
    }

    @Test
    public void testTemplateModelCreation() {
        TestTemplate testTemplate = new TestTemplate();

        TemplateModel modelProxy = testTemplate.getModel();
        TemplateModel modelProxy2 = testTemplate.getModel();

        Assert.assertTrue(Proxy.isProxyClass(modelProxy.getClass()));
        Assert.assertTrue(modelProxy == modelProxy2);

        modelProxy2 = new TestTemplate().getModel();
        Assert.assertTrue(Proxy.isProxyClass(modelProxy2.getClass()));
        Assert.assertNotSame(modelProxy, modelProxy2);
    }

}
