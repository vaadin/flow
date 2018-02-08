/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.component;

/**
 * Completion options for the {@code autocomplete} attribute.
 */
public enum Completion {

    /**
     * The user must explicitly enter a value into this field for every use, or
     * the document provides its own auto-completion method. The browser does
     * not automatically complete the entry.
     */
    OFF("off"),

    /**
     * The browser is allowed to automatically complete the value based on
     * values that the user has entered during previous uses, however {@code on}
     * does not provide any further information about what kind of data the user
     * might be expected to enter.
     */
    ON("on"),

    /**
     * Full name.
     */
    NAME("name"),

    /**
     * Prefix or title (e.g. "Mr.", "Ms.", "Dr.", "Mlle").
     */
    HONORIFIC_PREFIX("honorific-prefix"),

    /**
     * First name.
     */
    GIVEN_NAME("given-name"),

    /**
     * Middle name.
     */
    ADDITIONAL_NAME("additional-name"),

    /**
     * Last name.
     */
    FAMILY_NAME("family-name"),

    /**
     * Suffix (e.g. "Jr.", "B.Sc.", "MBASW", "II").
     */
    HONORIFIC_SUFFIX("honorific-suffix"),

    /**
     * Nickname.
     */
    NICKNAME("nickname"),

    /**
     * E-Mail address.
     */
    EMAIL("email"),

    /**
     * Username.
     */
    USERNAME("username"),

    /**
     * A new password (e.g. when creating an account or changing a password).
     */
    NEW_PASSWORD("new-password"),

    /**
     * Current password.
     */
    CURRENT_PASSWORD("current-password"),

    /**
     * Job title (e.g. "Software Engineer", "Senior Vice President", "Deputy
     * Managing Director").
     */
    ORGANIZATION_TITLE("organization-title"),

    /**
     * Organization.
     */
    ORGANIZATION("organization"),

    /**
     * Street address.
     */
    STREET_ADDRESS("street-address"),

    /**
     * Address line 1.
     */
    ADDRESS_LINE1("address-line1"),

    /**
     * Address line 2.
     */
    ADDRESS_LINE2("address-line2"),

    /**
     * Address line 3.
     */
    ADDRESS_LINE3("address-line3"),

    /**
     * Address level 1.
     */
    ADDRESS_LEVEL1("address-level1"),

    /**
     * Address level 2.
     */
    ADDRESS_LEVEL2("address-level2"),

    /**
     * Address level 3.
     */
    ADDRESS_LEVEL3("address-level3"),

    /**
     * Address level 4.
     */
    ADDRESS_LEVEL4("address-level4"),

    /**
     * Country.
     */
    COUNTRY("country"),

    /**
     * Country name.
     */
    COUNTRY_NAME("country-name"),

    /**
     * Postal code.
     */
    POSTAL_CODE("postal-code"),

    /**
     * Full name as given on the payment instrument.
     */
    CC_NAME("cc-name"),

    /**
     * First name as given on the payment instrument.
     */
    CC_GIVEN_NAME("cc-given-name"),

    /**
     * Middle name as given on the payment instrument.
     */
    CC_ADDITIONAL_NAME("cc-additional-name"),

    /**
     * Last name as given on the payment instrument.
     */
    CC_FAMILY_NAME("cc-family-name"),

    /**
     * Code identifying the payment instrument (e.g. the credit card number).
     */
    CC_NUMBER("cc-number"),

    /**
     * Expiration date of the payment instrument.
     */
    CC_EXP("cc-exp"),

    /**
     * Expiration month of the payment instrument.
     */
    CC_EXP_MONTH("cc-exp-month"),

    /**
     * Expiration year of the payment instrument.
     */
    CC_EXP_YEAR("cc-exp-year"),

    /**
     * Security code for the payment instrument.
     */
    CC_CSC("cc-csc"),

    /**
     * Type of payment instrument (e.g. Visa).
     */
    CC_TYPE("cc-type"),

    /**
     * Transaction currency.
     */
    TRANSACTION_CURRENCY("transaction-currency"),

    /**
     * Transaction amount.
     */
    TRANSACTION_AMOUNT("transaction-amount"),

    /**
     * Preferred language; a valid BCP 47 language tag.
     */
    LANGUAGE("language"),

    /**
     * Date of birth.
     */
    BDAY("bday"),

    /**
     * Day of birth.
     */
    BDAY_DAY("bday-day"),

    /**
     * Month of birth.
     */
    BDAY_MONTH("bday-month"),

    /**
     * Year of birth.
     */
    BDAY_YEAR("bday-year"),

    /**
     * Gender identity (e.g. Female, Fa'afafine), free-form text, no newlines.
     */
    SEX("sex"),

    /**
     * Full telephone number, including country code.
     */
    TEL("tel"),

    /**
     * Telephone number country code.
     */
    TEL_COUNTRY_CODE("tel-country-code"),

    /**
     * Telephone number, without country code.
     */
    TEL_NATIONAL("tel-national"),

    /**
     * Telephone number area code.
     */
    TEL_AREA_CODE("tel-area-code"),

    /**
     * Telephone number, local part.
     */
    TEL_LOCAL("tel-local"),

    /**
     * Telephone number, local prefix.
     */
    TEL_LOCAL_PREFIX("tel-local-prefix"),

    /**
     * Telephone number, local suffix.
     */
    TEL_LOCAL_SUFFIX("tel-local-suffix"),

    /**
     * Telephone number, extension code.
     */
    TEL_EXTENSION("tel-extension"),

    /**
     * Home page or other Web page corresponding to the company, person,
     * address, or contact information in the other fields associated with this
     * field.
     */
    URL("url"),

    /**
     * Photograph, icon, or other image corresponding to the company, person,
     * address, or contact information in the other fields associated with this
     * field.
     */
    PHOTO("photo");

    final String value;

    Completion(String value) {
        this.value = value;
    }
}
