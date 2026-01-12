/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.polymertemplate.HasCurrentService;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModelTest.EmptyDivTemplate;

import static org.junit.Assert.assertEquals;

@NotThreadSafe
public class TemplateModelWithEncodersTest extends HasCurrentService {

    public static class TemplateWithEncoders extends
            EmptyDivTemplate<TemplateWithEncoders.TemplateModelWithEncoders> {

        public interface TemplateModelWithEncoders extends TemplateModel {
            @Encode(value = LongToStringEncoder.class)
            void setLongValue(long longValue);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            long getLongValue();

            @Encode(value = DateToStringEncoder.class)
            void setDate(Date date);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            Date getDate();

            @Encode(value = DateToBeanWithStringEncoder.class)
            void setDateString(Date date);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            Date getDateString();

            @Encode(value = StringToBeanWithStringEncoder.class)
            void setString(String string);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            String getString();

            @Encode(value = LongToStringEncoder.class, path = "longValue")
            @Encode(value = DateToStringEncoder.class, path = "date")
            void setTestBean(TestBean bean);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            TestBean getTestBean();
        }

        @Override
        protected TemplateModelWithEncoders getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithIncompatibleEncoder extends
            EmptyDivTemplate<TemplateWithIncompatibleEncoder.TemplateModelWithIncompatibleEncoder> {

        public interface TemplateModelWithIncompatibleEncoder
                extends TemplateModel {

            @Encode(value = LongToStringEncoder.class)
            void setIntValue(int intValue);
        }

        @Override
        protected TemplateModelWithIncompatibleEncoder getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithEncoderOnParameterizedType extends
            EmptyDivTemplate<TemplateWithEncoderOnParameterizedType.TemplateModelWithEncoderOnParameterizedType> {

        public interface TemplateModelWithEncoderOnParameterizedType
                extends TemplateModel {

            @Encode(value = LongToStringEncoder.class)
            void setList(List<String> list);
        }

        @Override
        protected TemplateModelWithEncoderOnParameterizedType getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithSamePathInEncoders extends
            EmptyDivTemplate<TemplateWithSamePathInEncoders.TemplateModelWithSamePathInEncoders> {

        public interface TemplateModelWithSamePathInEncoders
                extends TemplateModel {

            @Encode(value = LongToStringEncoder.class, path = "same")
            @Encode(value = LongToStringEncoder.class, path = "same")
            void setLongValue(long longValue);
        }

        @Override
        protected TemplateModelWithSamePathInEncoders getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithEncoderOnEncodedType extends
            EmptyDivTemplate<TemplateWithEncoderOnEncodedType.TemplateModelWithEncoderOnEncodedType> {
        public interface TemplateModelWithEncoderOnEncodedType
                extends TemplateModel {

            @Encode(value = LongToBeanWithLongEncoder.class)
            @Encode(value = LongToStringEncoder.class, path = "longValue")
            void setLongValue(long longValue);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            long getLongValue();
        }

        @Override
        protected TemplateModelWithEncoderOnEncodedType getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithUnsupportedEncoderModel extends
            EmptyDivTemplate<TemplateWithUnsupportedEncoderModel.TemplateModelWithUnsupportedEncoderModel> {
        public interface TemplateModelWithUnsupportedEncoderModel
                extends TemplateModel {

            @Encode(value = UnsupportedModelEncoder.class)
            void setString(String string);
        }

        @Override
        protected TemplateModelWithUnsupportedEncoderModel getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithEncodedReadOnlyBean extends
            EmptyDivTemplate<TemplateWithEncodedReadOnlyBean.TemplateModelWithEncodedReadOnlyBean> {
        public interface TemplateModelWithEncodedReadOnlyBean
                extends TemplateModel {

            @Encode(value = LongToStringEncoder.class, path = "id")
            void setReadOnlyBean(ReadOnlyBean readOnlyBean);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            ReadOnlyBean getReadOnlyBean();
        }

        @Override
        protected TemplateModelWithEncodedReadOnlyBean getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithDate
            extends EmptyDivTemplate<TemplateWithDate.TemplateModelWithDate> {
        public interface TemplateModelWithDate extends TemplateModel {

            @Encode(value = DateToDateBeanEncoder.class)
            void setDate(Date date);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            Date getDate();
        }

        @Override
        protected TemplateModelWithDate getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithListOfBeans extends
            EmptyDivTemplate<TemplateWithListOfBeans.TemplateModelWithListOfBeans> {
        public interface TemplateModelWithListOfBeans extends TemplateModel {

            @Encode(value = LongToStringEncoder.class, path = "longValue")
            @Encode(value = DateToStringEncoder.class, path = "date")
            void setTestBeans(List<TestBean> testBeans);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            List<TestBean> getTestBeans();
        }

        @Override
        protected TemplateModelWithListOfBeans getModel() {
            return super.getModel();
        }
    }

    public static class LongToStringEncoder
            implements ModelEncoder<Long, String> {

        @Override
        public String encode(Long applicationValue) {
            return applicationValue.toString();
        }

        @Override
        public Long decode(String modelValue) {
            return Long.parseLong(modelValue);
        }
    }

    public static class DateToStringEncoder
            implements ModelEncoder<Date, String> {

        @Override
        public String encode(Date applicationValue) {
            return Long.toString(applicationValue.getTime());
        }

        @Override
        public Date decode(String modelValue) {
            return new Date(Long.valueOf(modelValue));
        }
    }

    public static class StringToBeanWithStringEncoder
            implements ModelEncoder<String, BeanWithString> {

        @Override
        public BeanWithString encode(String applicationValue) {
            return new BeanWithString(applicationValue);
        }

        @Override
        public String decode(BeanWithString modelValue) {
            return modelValue.getStringValue();
        }
    }

    public static class LongToBeanWithLongEncoder
            implements ModelEncoder<Long, BeanWithLong> {

        @Override
        public Class<Long> getDecodedType() {
            return long.class;
        }

        @Override
        public BeanWithLong encode(Long applicationValue) {
            return new BeanWithLong(applicationValue);
        }

        @Override
        public Long decode(BeanWithLong modelValue) {
            return modelValue.getLongValue();
        }
    }

    public static class DateToBeanWithStringEncoder
            implements ModelEncoder<Date, BeanWithString> {

        @Override
        public BeanWithString encode(Date applicationValue) {
            if (applicationValue == null) {
                return null;
            }
            return new BeanWithString(
                    Long.toString(applicationValue.getTime()));
        }

        @Override
        public Date decode(BeanWithString modelValue) {
            if (modelValue == null || modelValue.getStringValue() == null) {
                return null;
            }
            return new Date(Long.valueOf(modelValue.getStringValue()));
        }
    }

    public static class UnsupportedModelEncoder
            implements ModelEncoder<String, Long> {

        @Override
        public Class<Long> getEncodedType() {
            return long.class;
        }

        @Override
        public Long encode(String applicationValue) {
            return Long.valueOf(applicationValue);
        }

        @Override
        public String decode(Long modelValue) {
            return modelValue.toString();
        }
    }

    public static class DateToDateBeanEncoder
            implements ModelEncoder<Date, DateBean> {

        @Override
        public DateBean encode(Date applicationValue) {
            if (applicationValue == null) {
                return null;
            }

            Calendar calendar = new GregorianCalendar();
            calendar.setTime(applicationValue);

            DateBean bean = new DateBean();
            bean.setDay(calendar.get(Calendar.DATE));
            bean.setMonth(calendar.get(Calendar.MONTH));
            bean.setYear(calendar.get(Calendar.YEAR));
            return bean;
        }

        @Override
        public Date decode(DateBean modelValue) {
            if (modelValue == null) {
                return null;
            }
            int year = modelValue.getYear();
            int day = modelValue.getDay();
            int month = modelValue.getMonth();
            return new GregorianCalendar(year, month, day).getTime();
        }
    }

    public static class BeanWithString implements Serializable {
        private String stringValue;

        public BeanWithString() {

        }

        public BeanWithString(String stringValue) {
            this.stringValue = stringValue;
        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }
    }

    public static class TestBean {
        private long longValue;
        private Date date;

        public TestBean() {
        }

        public TestBean(long longValue, Date date) {
            this.longValue = longValue;
            this.date = date;
        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public long getLongValue() {
            return longValue;
        }

        public void setLongValue(long longValue) {
            this.longValue = longValue;
        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    public static class BeanWithLong implements Serializable {
        private long longValue;

        public BeanWithLong() {

        }

        public BeanWithLong(long longValue) {
            this.longValue = longValue;
        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public long getLongValue() {
            return longValue;
        }

        public void setLongValue(long longValue) {
            this.longValue = longValue;
        }
    }

    public static class ReadOnlyBean implements Serializable {

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public long getId() {
            return 0L;
        }
    }

    public static class DateBean implements Serializable {

        private int day;
        private int month;
        private int year;

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }
    }

    public static class EncodersOnGetters
            extends EmptyDivTemplate<EncodersOnGetters.Model> {

        public interface Model extends TemplateModel {
            void setLongValue(long longValue);

            @Encode(value = LongToStringEncoder.class)
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            long getLongValue();

            void setDate(Date date);

            @Encode(value = DateToStringEncoder.class)
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            Date getDate();

            void setDateString(Date date);

            @Encode(value = DateToBeanWithStringEncoder.class)
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            Date getDateString();

            void setString(String string);

            @Encode(value = StringToBeanWithStringEncoder.class)
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            String getString();

            void setTestBean(TestBean bean);

            @Encode(value = LongToStringEncoder.class, path = "longValue")
            @Encode(value = DateToStringEncoder.class, path = "date")
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            TestBean getTestBean();
        }

        @Override
        protected Model getModel() {
            return super.getModel();
        }
    }

    public static class SameEncodersOnAllMethods
            extends EmptyDivTemplate<SameEncodersOnAllMethods.Model> {

        public interface Model extends TemplateModel {
            @Encode(value = LongToStringEncoder.class)
            void setLongValue(long longValue);

            @Encode(value = LongToStringEncoder.class)
            long getLongValue();

            @Encode(value = DateToStringEncoder.class)
            void setDate(Date date);

            @Encode(value = DateToStringEncoder.class)
            Date getDate();

            @Encode(value = DateToBeanWithStringEncoder.class)
            void setDateString(Date date);

            @Encode(value = DateToBeanWithStringEncoder.class)
            Date getDateString();

            @Encode(value = StringToBeanWithStringEncoder.class)
            void setString(String string);

            @Encode(value = StringToBeanWithStringEncoder.class)
            String getString();

            @Encode(value = LongToStringEncoder.class, path = "longValue")
            @Encode(value = DateToStringEncoder.class, path = "date")
            void setTestBean(TestBean bean);

            @Encode(value = LongToStringEncoder.class, path = "longValue")
            @Encode(value = DateToStringEncoder.class, path = "date")
            TestBean getTestBean();
        }

        @Override
        protected Model getModel() {
            return super.getModel();
        }
    }

    @Override
    protected VaadinService createService() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        return service;
    }

    @Test
    public void unsupported_primitive_type_to_basic_type_encoder() {
        TemplateWithEncoders template = new TemplateWithEncoders();
        template.getModel().setLongValue(10L);
        assertEquals(10L, template.getModel().getLongValue());
    }

    @Test
    public void bean_to_basic_type_encoder() {
        TemplateWithEncoders template = new TemplateWithEncoders();
        Date date = new Date();
        template.getModel().setDate(date);
        assertEquals(date, template.getModel().getDate());
    }

    @Test
    public void bean_to_bean_encoder() {
        TemplateWithEncoders template = new TemplateWithEncoders();
        Date date = new Date();
        template.getModel().setDateString(date);
        assertEquals(date, template.getModel().getDateString());
    }

    @Test
    public void basic_type_to_bean_encoder() {
        TemplateWithEncoders template = new TemplateWithEncoders();
        template.getModel().setString("string to bean");
        assertEquals("string to bean", template.getModel().getString());
    }

    @Test
    public void bean_with_multiple_encoders() {
        TemplateWithEncoders template = new TemplateWithEncoders();
        Date date = new Date();
        TestBean bean = new TestBean(10L, date);
        template.getModel().setTestBean(bean);
        assertEquals(10L, template.getModel().getTestBean().getLongValue());
        assertEquals(date, template.getModel().getTestBean().getDate());
    }

    @Test
    public void encoder_on_encoded_type() {
        TemplateWithEncoderOnEncodedType template = new TemplateWithEncoderOnEncodedType();
        template.getModel().setLongValue(10L);
        assertEquals(10L, template.getModel().getLongValue());
    }

    @Test
    public void Encoder_on_bean_with_read_only_property() {
        TemplateWithEncodedReadOnlyBean template = new TemplateWithEncodedReadOnlyBean();
        template.getModel().setReadOnlyBean(new ReadOnlyBean());
        assertEquals(0L, template.getModel().getReadOnlyBean().getId());
    }

    @Test
    public void encode_date_to_datebean() {
        // DateBean strips time information
        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date dateWithoutTime = calendar.getTime();

        TemplateWithDate template = new TemplateWithDate();
        template.getModel().setDate(dateWithoutTime);
        assertEquals(dateWithoutTime, template.getModel().getDate());
    }

    @Test
    public void encode_on_list_of_beans() {
        Date date = new Date();
        List<TestBean> testBeans = Collections
                .singletonList(new TestBean(0L, date));
        TemplateWithListOfBeans template = new TemplateWithListOfBeans();
        template.getModel().setTestBeans(testBeans);
        assertEquals(0L,
                template.getModel().getTestBeans().get(0).getLongValue());
        assertEquals(date, template.getModel().getTestBeans().get(0).getDate());
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void incompatible_Encoder_throws() {
        new TemplateWithIncompatibleEncoder();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void parameterized_type_encoding_throws() {
        new TemplateWithEncoderOnParameterizedType();
    }

    @Test(expected = RuntimeException.class)
    public void multiple_encoders_for_same_path_throws() {
        new TemplateWithSamePathInEncoders();
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void unsupported_model_type_in_encoder() {
        new TemplateWithUnsupportedEncoderModel();
    }

    @Test
    public void encodeDateToBean_noExceptions() {
        TemplateWithEncoders template = new TemplateWithEncoders();

        Date date = template.getModel().getDateString();
        Assert.assertNull(date);
    }

    @Test
    public void encodersOnGetters_noExceptions() {
        EncodersOnGetters template = new EncodersOnGetters();
        EncodersOnGetters.Model model = template.getModel();

        model.setLongValue(1L);
        assertEquals(1L, model.getLongValue());

        model.setString("string");
        assertEquals("string", model.getString());

        Date date = new Date();
        model.setDate(date);
        assertEquals(date, model.getDate());
        model.setDateString(date);
        assertEquals(date, model.getDateString());

        TestBean bean = new TestBean(10L, date);
        template.getModel().setTestBean(bean);
        assertEquals(bean.getLongValue(), model.getTestBean().getLongValue());
        assertEquals(bean.getDate(), model.getTestBean().getDate());
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void sameEncodersOnAllMethods_notAllowed() {
        new SameEncodersOnAllMethods();
    }

    @Test(expected = IllegalArgumentException.class)
    public void brokenModelType_throws() {
        TemplateWithDate template = new TemplateWithDate();

        StateNode node = template.getElement().getNode();

        ElementPropertyMap map = node.getFeature(ElementPropertyMap.class)
                .resolveModelMap("date");

        map.setProperty("day", "foo");
        template.getModel().getDate();
    }
}
