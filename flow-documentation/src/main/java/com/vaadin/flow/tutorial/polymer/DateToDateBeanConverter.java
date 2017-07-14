package com.vaadin.flow.tutorial.polymer;

import java.util.Date;

import com.vaadin.flow.template.model.ModelConverter;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("tutorial-template-model-converters.asciidoc")
public class DateToDateBeanConverter implements ModelConverter<Date, DateBean> {

    @Override
    public Class<Date> getApplicationType() {
        return Date.class;
    }

    @Override
    public Class<DateBean> getModelType() {
        return DateBean.class;
    }

    @Override
    public DateBean toModel(Date applicationValue) {
        if (applicationValue == null) {
            return null;
        }
        DateBean bean = new DateBean();
        bean.setDay(Integer.toString(applicationValue.getDay()));
        bean.setMonth(Integer.toString(applicationValue.getMonth() + 1));
        bean.setYear(Integer.toString(applicationValue.getYear() + 1900));
        return bean;
    }

    @Override
    public Date toApplication(DateBean modelValue) {
        if (modelValue == null) {
            return null;
        }
        int year = Integer.parseInt(modelValue.getYear()) - 1900;
        int day = Integer.parseInt(modelValue.getDay());
        int month = Integer.parseInt(modelValue.getMonth()) - 1;
        return new Date(year, month, day);
    }

}