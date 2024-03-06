/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.tests.data.bean;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

public class ConvertibleValues {

    private Long dateToLong;
    private long dateToPrimitiveLong;
    private java.sql.Date dateToSqlDate;
    private Date localDateTimeToDate;
    private Date localDateToDate;
    private BigDecimal stringToBigDecimal;
    private BigInteger stringToBigInteger;
    private Boolean stringToBoolean;
    private boolean stringToPrimitiveBoolean;
    private Date stringToDate;
    private Double stringToDouble;
    private double stringToPrimitiveDouble;
    private Float stringToFloat;
    private float stringToPrimitiveFloat;
    private Integer stringToInteger;
    private int stringToPrimitiveInteger;
    private Long stringToLong;
    private long stringToPrimitiveLong;
    private UUID stringToUUID;

    public Long getDateToLong() {
        return dateToLong;
    }

    public void setDateToLong(Long dateToLong) {
        this.dateToLong = dateToLong;
    }

    public long getDateToPrimitiveLong() {
        return dateToPrimitiveLong;
    }

    public void setDateToPrimitiveLong(long dateToPrimitiveLong) {
        this.dateToPrimitiveLong = dateToPrimitiveLong;
    }

    public java.sql.Date getDateToSqlDate() {
        return dateToSqlDate;
    }

    public void setDateToSqlDate(java.sql.Date dateToSqlDate) {
        this.dateToSqlDate = dateToSqlDate;
    }

    public Date getLocalDateTimeToDate() {
        return localDateTimeToDate;
    }

    public void setLocalDateTimeToDate(Date localDateTimeToDate) {
        this.localDateTimeToDate = localDateTimeToDate;
    }

    public Date getLocalDateToDate() {
        return localDateToDate;
    }

    public void setLocalDateToDate(Date localDateToDate) {
        this.localDateToDate = localDateToDate;
    }

    public BigDecimal getStringToBigDecimal() {
        return stringToBigDecimal;
    }

    public void setStringToBigDecimal(BigDecimal stringToBigDecimal) {
        this.stringToBigDecimal = stringToBigDecimal;
    }

    public BigInteger getStringToBigInteger() {
        return stringToBigInteger;
    }

    public void setStringToBigInteger(BigInteger stringToBigInteger) {
        this.stringToBigInteger = stringToBigInteger;
    }

    public Boolean getStringToBoolean() {
        return stringToBoolean;
    }

    public void setStringToBoolean(Boolean stringToBoolean) {
        this.stringToBoolean = stringToBoolean;
    }

    public boolean isStringToPrimitiveBoolean() {
        return stringToPrimitiveBoolean;
    }

    public void setStringToPrimitiveBoolean(boolean stringToPrimitiveBoolean) {
        this.stringToPrimitiveBoolean = stringToPrimitiveBoolean;
    }

    public Date getStringToDate() {
        return stringToDate;
    }

    public void setStringToDate(Date stringToDate) {
        this.stringToDate = stringToDate;
    }

    public Double getStringToDouble() {
        return stringToDouble;
    }

    public void setStringToDouble(Double stringToDouble) {
        this.stringToDouble = stringToDouble;
    }

    public double getStringToPrimitiveDouble() {
        return stringToPrimitiveDouble;
    }

    public void setStringToPrimitiveDouble(double stringToPrimitiveDouble) {
        this.stringToPrimitiveDouble = stringToPrimitiveDouble;
    }

    public Float getStringToFloat() {
        return stringToFloat;
    }

    public void setStringToFloat(Float stringToFloat) {
        this.stringToFloat = stringToFloat;
    }

    public float getStringToPrimitiveFloat() {
        return stringToPrimitiveFloat;
    }

    public void setStringToPrimitiveFloat(float stringToPrimitiveFloat) {
        this.stringToPrimitiveFloat = stringToPrimitiveFloat;
    }

    public Integer getStringToInteger() {
        return stringToInteger;
    }

    public void setStringToInteger(Integer stringToInteger) {
        this.stringToInteger = stringToInteger;
    }

    public int getStringToPrimitiveInteger() {
        return stringToPrimitiveInteger;
    }

    public void setStringToPrimitiveInteger(int stringToPrimitiveInteger) {
        this.stringToPrimitiveInteger = stringToPrimitiveInteger;
    }

    public Long getStringToLong() {
        return stringToLong;
    }

    public void setStringToLong(Long stringToLong) {
        this.stringToLong = stringToLong;
    }

    public long getStringToPrimitiveLong() {
        return stringToPrimitiveLong;
    }

    public void setStringToPrimitiveLong(long stringToPrimitiveLong) {
        this.stringToPrimitiveLong = stringToPrimitiveLong;
    }

    public UUID getStringToUUID() {
        return stringToUUID;
    }

    public void setStringToUUID(UUID stringToUUID) {
        this.stringToUUID = stringToUUID;
    }
}
