package com.vaadin.flow.templatemodel;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.HasCurrentService;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModelTest.EmptyDivTemplate;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class TemplateModelWithConvertersTest extends HasCurrentService {

    public static class TemplateWithConverters extends
            EmptyDivTemplate<TemplateWithConverters.TemplateModelWithConverters> {

        public interface TemplateModelWithConverters extends TemplateModel {
            @Encode(value = LongToStringConverter.class)
            void setLongValue(long longValue);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            long getLongValue();

            @Encode(value = DateToStringConverter.class)
            void setDate(Date date);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            Date getDate();

            @Encode(value = DateToBeanWithStringConverter.class)
            void setDateString(Date date);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            Date getDateString();

            @Encode(value = StringToBeanWithStringConverter.class)
            void setString(String string);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            String getString();

            @Encode(value = LongToStringConverter.class, path = "longValue")
            @Encode(value = DateToStringConverter.class, path = "date")
            void setTestBean(TestBean bean);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            TestBean getTestBean();
        }

        @Override
        protected TemplateModelWithConverters getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithIncompatibleConverter extends
            EmptyDivTemplate<TemplateWithIncompatibleConverter.TemplateModelWithIncompatibleConverter> {

        public interface TemplateModelWithIncompatibleConverter
                extends TemplateModel {

            @Encode(value = LongToStringConverter.class)
            void setIntValue(int intValue);
        }

        @Override
        protected TemplateModelWithIncompatibleConverter getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithConverterOnParameterizedType extends
            EmptyDivTemplate<TemplateWithConverterOnParameterizedType.TemplateModelWithConverterOnParameterizedType> {

        public interface TemplateModelWithConverterOnParameterizedType
                extends TemplateModel {

            @Encode(value = LongToStringConverter.class)
            void setList(List<String> list);
        }

        @Override
        protected TemplateModelWithConverterOnParameterizedType getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithSamePathInConverters extends
            EmptyDivTemplate<TemplateWithSamePathInConverters.TemplateModelWithSamePathInConverters> {

        public interface TemplateModelWithSamePathInConverters
                extends TemplateModel {

            @Encode(value = LongToStringConverter.class, path = "same")
            @Encode(value = LongToStringConverter.class, path = "same")
            void setLongValue(long longValue);
        }

        @Override
        protected TemplateModelWithSamePathInConverters getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithConverterOnConvertedType extends
            EmptyDivTemplate<TemplateWithConverterOnConvertedType.TemplateModelWithConverterOnConvertedType> {
        public interface TemplateModelWithConverterOnConvertedType
                extends TemplateModel {

            @Encode(value = LongToBeanWithLongConverter.class)
            @Encode(value = LongToStringConverter.class, path = "longValue")
            void setLongValue(long longValue);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            long getLongValue();
        }

        @Override
        protected TemplateModelWithConverterOnConvertedType getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithUnsupportedConverterModel extends
            EmptyDivTemplate<TemplateWithUnsupportedConverterModel.TemplateModelWithUnsupportedConverterModel> {
        public interface TemplateModelWithUnsupportedConverterModel
                extends TemplateModel {

            @Encode(value = UnsupportedModelConverter.class)
            void setString(String string);
        }

        @Override
        protected TemplateModelWithUnsupportedConverterModel getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithConvertedReadOnlyBean extends
            EmptyDivTemplate<TemplateWithConvertedReadOnlyBean.TemplateModelWithConvertedReadOnlyBean> {
        public interface TemplateModelWithConvertedReadOnlyBean
                extends TemplateModel {

            @Encode(value = LongToStringConverter.class, path = "id")
            void setReadOnlyBean(ReadOnlyBean readOnlyBean);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            ReadOnlyBean getReadOnlyBean();
        }

        @Override
        protected TemplateModelWithConvertedReadOnlyBean getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithDate
            extends EmptyDivTemplate<TemplateWithDate.TemplateModelWithDate> {
        public interface TemplateModelWithDate extends TemplateModel {

            @Encode(value = DateToDateBeanConverter.class)
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

            @Encode(value = LongToStringConverter.class, path = "longValue")
            @Encode(value = DateToStringConverter.class, path = "date")
            void setTestBeans(List<TestBean> testBeans);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            List<TestBean> getTestBeans();
        }

        @Override
        protected TemplateModelWithListOfBeans getModel() {
            return super.getModel();
        }
    }

    public static class LongToStringConverter
            implements ModelEncoder<Long, String> {

        @Override
        public Class<Long> getDecodedType() {
            return long.class;
        }

        @Override
        public String encode(Long applicationValue) {
            return applicationValue.toString();
        }

        @Override
        public Long decode(String modelValue) {
            return Long.parseLong(modelValue);
        }
    }

    public static class DateToStringConverter
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

    public static class StringToBeanWithStringConverter
            implements ModelEncoder<String, BeanWithString> {

        @Override
        public BeanWithString encode(String applicationValue) {
            return new BeanWithString(applicationValue);
        }

        @Override
        public String toModel(BeanWithString modelValue) {
            return modelValue.getStringValue();
        }
    }

    public static class LongToBeanWithLongConverter
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
        public Long toModel(BeanWithLong modelValue) {
            return modelValue.getLongValue();
        }
    }

    public static class DateToBeanWithStringConverter
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
        public Date toModel(BeanWithString modelValue) {
            if (modelValue == null || modelValue.getStringValue() == null) {
                return null;
            }
            return new Date(Long.valueOf(modelValue.getStringValue()));
        }
    }

    public static class UnsupportedModelConverter
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

    public static class DateToDateBeanConverter
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
        public Date toModel(DateBean modelValue) {
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

    public static class ConvertersOnGetters
            extends EmptyDivTemplate<ConvertersOnGetters.Model> {

        public interface Model extends TemplateModel {
            void setLongValue(long longValue);

            @Encode(value = LongToStringConverter.class)
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            long getLongValue();

            void setDate(Date date);

            @Encode(value = DateToStringConverter.class)
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            Date getDate();

            void setDateString(Date date);

            @Encode(value = DateToBeanWithStringConverter.class)
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            Date getDateString();

            void setString(String string);

            @Encode(value = StringToBeanWithStringConverter.class)
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            String getString();

            void setTestBean(TestBean bean);

            @Encode(value = LongToStringConverter.class, path = "longValue")
            @Encode(value = DateToStringConverter.class, path = "date")
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            TestBean getTestBean();
        }

        @Override
        protected Model getModel() {
            return super.getModel();
        }
    }

    public static class SameConvertersOnAllMethods
            extends EmptyDivTemplate<SameConvertersOnAllMethods.Model> {

        public interface Model extends TemplateModel {
            @Encode(value = LongToStringConverter.class)
            void setLongValue(long longValue);

            @Encode(value = LongToStringConverter.class)
            long getLongValue();

            @Encode(value = DateToStringConverter.class)
            void setDate(Date date);

            @Encode(value = DateToStringConverter.class)
            Date getDate();

            @Encode(value = DateToBeanWithStringConverter.class)
            void setDateString(Date date);

            @Encode(value = DateToBeanWithStringConverter.class)
            Date getDateString();

            @Encode(value = StringToBeanWithStringConverter.class)
            void setString(String string);

            @Encode(value = StringToBeanWithStringConverter.class)
            String getString();

            @Encode(value = LongToStringConverter.class, path = "longValue")
            @Encode(value = DateToStringConverter.class, path = "date")
            void setTestBean(TestBean bean);

            @Encode(value = LongToStringConverter.class, path = "longValue")
            @Encode(value = DateToStringConverter.class, path = "date")
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
    public void unsupported_primitive_type_to_basic_type_converter() {
        TemplateWithConverters template = new TemplateWithConverters();
        template.getModel().setLongValue(10L);
        assertEquals(10L, template.getModel().getLongValue());
    }

    @Test
    public void bean_to_basic_type_converter() {
        TemplateWithConverters template = new TemplateWithConverters();
        Date date = new Date();
        template.getModel().setDate(date);
        assertEquals(date, template.getModel().getDate());
    }

    @Test
    public void bean_to_bean_converter() {
        TemplateWithConverters template = new TemplateWithConverters();
        Date date = new Date();
        template.getModel().setDateString(date);
        assertEquals(date, template.getModel().getDateString());
    }

    @Test
    public void basic_type_to_bean_converter() {
        TemplateWithConverters template = new TemplateWithConverters();
        template.getModel().setString("string to bean");
        assertEquals("string to bean", template.getModel().getString());
    }

    @Test
    public void bean_with_multiple_converters() {
        TemplateWithConverters template = new TemplateWithConverters();
        Date date = new Date();
        TestBean bean = new TestBean(10L, date);
        template.getModel().setTestBean(bean);
        assertEquals(10L, template.getModel().getTestBean().getLongValue());
        assertEquals(date, template.getModel().getTestBean().getDate());
    }

    @Test
    public void converter_on_converted_type() {
        TemplateWithConverterOnConvertedType template = new TemplateWithConverterOnConvertedType();
        template.getModel().setLongValue(10L);
        assertEquals(10L, template.getModel().getLongValue());
    }

    @Test
    public void converter_on_bean_with_read_only_property() {
        TemplateWithConvertedReadOnlyBean template = new TemplateWithConvertedReadOnlyBean();
        template.getModel().setReadOnlyBean(new ReadOnlyBean());
        assertEquals(0L, template.getModel().getReadOnlyBean().getId());
    }

    @Test
    public void convert_date_to_datebean() {
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
    public void convert_on_list_of_beans() {
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
    public void incompatible_converter_throws() {
        new TemplateWithIncompatibleConverter();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void parameterized_type_conversion_throws() {
        new TemplateWithConverterOnParameterizedType();
    }

    @Test(expected = RuntimeException.class)
    public void multiple_converters_for_same_path_throws() {
        new TemplateWithSamePathInConverters();
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void unsupported_model_type_in_converter() {
        new TemplateWithUnsupportedConverterModel();
    }

    @Test
    public void convertDateToBean_noExceptions() {
        TemplateWithConverters template = new TemplateWithConverters();

        Date date = template.getModel().getDateString();
        Assert.assertNull(date);
    }

    @Test
    public void convertersOnGetters_noExceptions() {
        ConvertersOnGetters template = new ConvertersOnGetters();
        ConvertersOnGetters.Model model = template.getModel();

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
    public void sameConvertersOnAllMethods_notAllowed() {
        new SameConvertersOnAllMethods();
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
