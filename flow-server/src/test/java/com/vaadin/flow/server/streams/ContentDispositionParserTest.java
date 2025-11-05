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
package com.vaadin.flow.server.streams;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link ContentDispositionParser}.
 */
public class ContentDispositionParserTest {

    @Test
    public void extractFilename_quotedFilename_extracted() {
        String result = ContentDispositionParser
                .extractFilename("attachment; filename=\"test.txt\"");
        Assert.assertEquals("test.txt", result);
    }

    @Test
    public void extractFilename_unquotedFilename_extracted() {
        String result = ContentDispositionParser
                .extractFilename("attachment; filename=test.txt");
        Assert.assertEquals("test.txt", result);
    }

    @Test
    public void extractFilename_formData_extracted() {
        String result = ContentDispositionParser.extractFilename(
                "form-data; name=\"file\"; filename=\"document.pdf\"");
        Assert.assertEquals("document.pdf", result);
    }

    @Test
    public void extractFilename_filenameWithSpaces_extracted() {
        String result = ContentDispositionParser
                .extractFilename("attachment; filename=\"my document.txt\"");
        Assert.assertEquals("my document.txt", result);
    }

    @Test
    public void extractFilename_extendedFilename_decoded() {
        // Percent-encoded space should be decoded to actual space
        String result = ContentDispositionParser.extractFilename(
                "attachment; filename*=UTF-8''test%20file.txt");
        Assert.assertEquals("test file.txt", result);
    }

    @Test
    public void extractFilename_extendedFilenameWithLanguage_extracted() {
        String result = ContentDispositionParser
                .extractFilename("attachment; filename*=UTF-8'en'document.pdf");
        Assert.assertEquals("document.pdf", result);
    }

    @Test
    public void extractFilename_bothFilenameParameters_extendedPreferred() {
        // When both filename and filename* are present, filename* should be
        // preferred and decoded
        String result = ContentDispositionParser.extractFilename(
                "attachment; filename=\"fallback.txt\"; filename*=UTF-8''preferred.txt");
        Assert.assertEquals("preferred.txt", result);
    }

    @Test
    public void extractFilename_nullHeader_returnsNull() {
        String result = ContentDispositionParser.extractFilename(null);
        Assert.assertNull(result);
    }

    @Test
    public void extractFilename_emptyHeader_returnsNull() {
        String result = ContentDispositionParser.extractFilename("");
        Assert.assertNull(result);
    }

    @Test
    public void extractFilename_noFilename_returnsNull() {
        String result = ContentDispositionParser
                .extractFilename("attachment; size=12345");
        Assert.assertNull(result);
    }

    @Test
    public void extractFilename_emptyFilename_returnsEmpty() {
        String result = ContentDispositionParser
                .extractFilename("attachment; filename=\"\"");
        Assert.assertEquals("", result);
    }

    @Test
    public void extractFilename_filenameAtEnd_extracted() {
        String result = ContentDispositionParser
                .extractFilename("inline; size=1024; filename=\"end.txt\"");
        Assert.assertEquals("end.txt", result);
    }

    @Test
    public void extractFilename_encodedNordicCharacters_decoded() {
        // åäö.txt encoded as UTF-8 percent-encoded, should be decoded
        String result = ContentDispositionParser.extractFilename(
                "attachment; filename*=UTF-8''%C3%A5%C3%A4%C3%B6.txt");
        Assert.assertEquals("åäö.txt", result);
    }

    @Test
    public void extractFilename_encodedGermanUmlaut_decoded() {
        // über.pdf encoded as UTF-8 percent-encoded, should be decoded
        String result = ContentDispositionParser
                .extractFilename("attachment; filename*=UTF-8''%C3%BCber.pdf");
        Assert.assertEquals("über.pdf", result);
    }

    @Test
    public void extractFilename_encodedChineseCharacters_decoded() {
        // 文件.txt encoded as UTF-8 percent-encoded, should be decoded
        String result = ContentDispositionParser.extractFilename(
                "attachment; filename*=UTF-8''%E6%96%87%E4%BB%B6.txt");
        Assert.assertEquals("文件.txt", result);
    }

    @Test
    public void extractFilename_rawUnicodeInStandardFilename_passedThrough() {
        // Standard filename parameter with raw Unicode (not RFC 5987 compliant
        // but some clients may send it)
        String result = ContentDispositionParser
                .extractFilename("attachment; filename=\"åäö.txt\"");
        Assert.assertEquals("åäö.txt", result);
    }

    @Test
    public void extractParameter_quotedValue_unquoted() {
        String result = ContentDispositionParser
                .extractParameter("name=\"value\"", "name");
        Assert.assertEquals("value", result);
    }

    @Test
    public void extractParameter_unquotedValue_extracted() {
        String result = ContentDispositionParser.extractParameter("name=value",
                "name");
        Assert.assertEquals("value", result);
    }

    @Test
    public void extractParameter_valueWithSemicolon_extractedCorrectly() {
        String result = ContentDispositionParser
                .extractParameter("name=value; other=data", "name");
        Assert.assertEquals("value", result);
    }

    @Test
    public void extractParameter_quotedValueWithSemicolon_extractedCorrectly() {
        String result = ContentDispositionParser
                .extractParameter("name=\"value\"; other=data", "name");
        Assert.assertEquals("value", result);
    }

    @Test
    public void extractParameter_missingClosingQuote_takesRestOfString() {
        String result = ContentDispositionParser
                .extractParameter("name=\"value without end quote", "name");
        Assert.assertEquals("value without end quote", result);
    }

    @Test
    public void extractParameter_nullHeaderValue_returnsNull() {
        String result = ContentDispositionParser.extractParameter(null, "name");
        Assert.assertNull(result);
    }

    @Test
    public void extractParameter_nullParamName_returnsNull() {
        String result = ContentDispositionParser.extractParameter("name=value",
                null);
        Assert.assertNull(result);
    }

    @Test
    public void extractParameter_paramNotFound_returnsNull() {
        String result = ContentDispositionParser.extractParameter("name=value",
                "missing");
        Assert.assertNull(result);
    }

    @Test
    public void extractParameter_emptyValue_returnsEmpty() {
        String result = ContentDispositionParser.extractParameter("name=",
                "name");
        Assert.assertEquals("", result);
    }

    @Test
    public void extractParameter_valueWithWhitespace_trimmed() {
        String result = ContentDispositionParser
                .extractParameter("name= value ", "name");
        Assert.assertEquals("value", result);
    }
}
