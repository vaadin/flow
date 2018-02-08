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

import org.junit.Assert;
import org.junit.Test;

public class HasCompletionTest {

    @Tag("div")
    public static class HasCompletionComponent extends Component implements HasCompletion {

    }

    @Test
    public void defaultValue() {
        HasCompletionComponent c = new HasCompletionComponent();
        Assert.assertEquals(null, c.getAutocomplete());
    }

    @Test
    public void emptyValue() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.getElement().setAttribute("autocomplete", "");
        Assert.assertEquals(Completion.OFF, c.getAutocomplete());
    }

    @Test
    public void noAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.OFF);
        Assert.assertEquals(Completion.OFF, c.getAutocomplete());
    }

    @Test
    public void onAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ON);
        Assert.assertEquals(Completion.ON, c.getAutocomplete());
    }

    @Test
    public void nameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.NAME);
        Assert.assertEquals(Completion.NAME, c.getAutocomplete());
    }

    @Test
    public void honorificPrefixAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.HONORIFIC_PREFIX);
        Assert.assertEquals(Completion.HONORIFIC_PREFIX, c.getAutocomplete());
    }

    @Test
    public void givenNameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.GIVEN_NAME);
        Assert.assertEquals(Completion.GIVEN_NAME, c.getAutocomplete());
    }

    @Test
    public void additionalNameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ADDITIONAL_NAME);
        Assert.assertEquals(Completion.ADDITIONAL_NAME, c.getAutocomplete());
    }

    @Test
    public void familyNameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.FAMILY_NAME);
        Assert.assertEquals(Completion.FAMILY_NAME, c.getAutocomplete());
    }

    @Test
    public void honorificSuffixAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.HONORIFIC_SUFFIX);
        Assert.assertEquals(Completion.HONORIFIC_SUFFIX, c.getAutocomplete());
    }

    @Test
    public void nicknameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.NICKNAME);
        Assert.assertEquals(Completion.NICKNAME, c.getAutocomplete());
    }

    @Test
    public void emailAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.EMAIL);
        Assert.assertEquals(Completion.EMAIL, c.getAutocomplete());
    }

    @Test
    public void usernameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.USERNAME);
        Assert.assertEquals(Completion.USERNAME, c.getAutocomplete());
    }

    @Test
    public void newPasswordAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.NEW_PASSWORD);
        Assert.assertEquals(Completion.NEW_PASSWORD, c.getAutocomplete());
    }

    @Test
    public void currentPasswordAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CURRENT_PASSWORD);
        Assert.assertEquals(Completion.CURRENT_PASSWORD, c.getAutocomplete());
    }

    @Test
    public void organizationTitleAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ORGANIZATION_TITLE);
        Assert.assertEquals(Completion.ORGANIZATION_TITLE, c.getAutocomplete());
    }

    @Test
    public void organizationAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ORGANIZATION);
        Assert.assertEquals(Completion.ORGANIZATION, c.getAutocomplete());
    }

    @Test
    public void streetAddressAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.STREET_ADDRESS);
        Assert.assertEquals(Completion.STREET_ADDRESS, c.getAutocomplete());
    }

    @Test
    public void addressLine1Autocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ADDRESS_LINE1);
        Assert.assertEquals(Completion.ADDRESS_LINE1, c.getAutocomplete());
    }

    @Test
    public void addressLine2Autocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ADDRESS_LINE2);
        Assert.assertEquals(Completion.ADDRESS_LINE2, c.getAutocomplete());
    }

    @Test
    public void addressLine3Autocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ADDRESS_LINE3);
        Assert.assertEquals(Completion.ADDRESS_LINE3, c.getAutocomplete());
    }

    @Test
    public void addressLevel1Autocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ADDRESS_LEVEL1);
        Assert.assertEquals(Completion.ADDRESS_LEVEL1, c.getAutocomplete());
    }

    @Test
    public void addressLevel2Autocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ADDRESS_LEVEL2);
        Assert.assertEquals(Completion.ADDRESS_LEVEL2, c.getAutocomplete());
    }

    @Test
    public void addressLevel3Autocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ADDRESS_LEVEL3);
        Assert.assertEquals(Completion.ADDRESS_LEVEL3, c.getAutocomplete());
    }

    @Test
    public void addressLevel4Autocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.ADDRESS_LEVEL4);
        Assert.assertEquals(Completion.ADDRESS_LEVEL4, c.getAutocomplete());
    }

    @Test
    public void countryAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.COUNTRY);
        Assert.assertEquals(Completion.COUNTRY, c.getAutocomplete());
    }

    @Test
    public void countryNameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.COUNTRY_NAME);
        Assert.assertEquals(Completion.COUNTRY_NAME, c.getAutocomplete());
    }

    @Test
    public void postalCodeAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.POSTAL_CODE);
        Assert.assertEquals(Completion.POSTAL_CODE, c.getAutocomplete());
    }

    @Test
    public void ccNameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_NAME);
        Assert.assertEquals(Completion.CC_NAME, c.getAutocomplete());
    }

    @Test
    public void ccGivenNameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_GIVEN_NAME);
        Assert.assertEquals(Completion.CC_GIVEN_NAME, c.getAutocomplete());
    }

    @Test
    public void ccAdditionalNameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_ADDITIONAL_NAME);
        Assert.assertEquals(Completion.CC_ADDITIONAL_NAME, c.getAutocomplete());
    }

    @Test
    public void ccFamilyNameAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_FAMILY_NAME);
        Assert.assertEquals(Completion.CC_FAMILY_NAME, c.getAutocomplete());
    }

    @Test
    public void ccNumberAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_NUMBER);
        Assert.assertEquals(Completion.CC_NUMBER, c.getAutocomplete());
    }

    @Test
    public void ccExpAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_EXP);
        Assert.assertEquals(Completion.CC_EXP, c.getAutocomplete());
    }

    @Test
    public void ccExpMonthAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_EXP_MONTH);
        Assert.assertEquals(Completion.CC_EXP_MONTH, c.getAutocomplete());
    }

    @Test
    public void ccExpYearAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_EXP_YEAR);
        Assert.assertEquals(Completion.CC_EXP_YEAR, c.getAutocomplete());
    }

    @Test
    public void ccCscAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_CSC);
        Assert.assertEquals(Completion.CC_CSC, c.getAutocomplete());
    }

    @Test
    public void ccTypeAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.CC_TYPE);
        Assert.assertEquals(Completion.CC_TYPE, c.getAutocomplete());
    }

    @Test
    public void transactionCurrencyAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TRANSACTION_CURRENCY);
        Assert.assertEquals(Completion.TRANSACTION_CURRENCY, c.getAutocomplete());
    }

    @Test
    public void transactionAmountAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TRANSACTION_AMOUNT);
        Assert.assertEquals(Completion.TRANSACTION_AMOUNT, c.getAutocomplete());
    }

    @Test
    public void languageAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.LANGUAGE);
        Assert.assertEquals(Completion.LANGUAGE, c.getAutocomplete());
    }

    @Test
    public void bdayAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.BDAY);
        Assert.assertEquals(Completion.BDAY, c.getAutocomplete());
    }

    @Test
    public void bdayDayAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.BDAY_DAY);
        Assert.assertEquals(Completion.BDAY_DAY, c.getAutocomplete());
    }

    @Test
    public void bdayMonthAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.BDAY_MONTH);
        Assert.assertEquals(Completion.BDAY_MONTH, c.getAutocomplete());
    }

    @Test
    public void bdayYearAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.BDAY_YEAR);
        Assert.assertEquals(Completion.BDAY_YEAR, c.getAutocomplete());
    }

    @Test
    public void sexAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.SEX);
        Assert.assertEquals(Completion.SEX, c.getAutocomplete());
    }

    @Test
    public void telAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TEL);
        Assert.assertEquals(Completion.TEL, c.getAutocomplete());
    }

    @Test
    public void telCountryCodeAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TEL_COUNTRY_CODE);
        Assert.assertEquals(Completion.TEL_COUNTRY_CODE, c.getAutocomplete());
    }

    @Test
    public void telNationalAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TEL_NATIONAL);
        Assert.assertEquals(Completion.TEL_NATIONAL, c.getAutocomplete());
    }

    @Test
    public void telAreaCodeAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TEL_AREA_CODE);
        Assert.assertEquals(Completion.TEL_AREA_CODE, c.getAutocomplete());
    }

    @Test
    public void telLocalAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TEL_LOCAL);
        Assert.assertEquals(Completion.TEL_LOCAL, c.getAutocomplete());
    }

    @Test
    public void telLocalPrefixAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TEL_LOCAL_PREFIX);
        Assert.assertEquals(Completion.TEL_LOCAL_PREFIX, c.getAutocomplete());
    }

    @Test
    public void telLocalSuffixAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TEL_LOCAL_SUFFIX);
        Assert.assertEquals(Completion.TEL_LOCAL_SUFFIX, c.getAutocomplete());
    }

    @Test
    public void telExtensionAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.TEL_EXTENSION);
        Assert.assertEquals(Completion.TEL_EXTENSION, c.getAutocomplete());
    }

    @Test
    public void urlAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.URL);
        Assert.assertEquals(Completion.URL, c.getAutocomplete());
    }

    @Test
    public void photoAutocomplete() {
        HasCompletionComponent c = new HasCompletionComponent();
        c.setAutocomplete(Completion.PHOTO);
        Assert.assertEquals(Completion.PHOTO, c.getAutocomplete());
    }
}
