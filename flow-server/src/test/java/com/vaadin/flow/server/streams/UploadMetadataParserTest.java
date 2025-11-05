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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link UploadMetadataParser}.
 */
public class UploadMetadataParserTest {

    @Test
    public void parse_singleParameter_parsed() {
        Map<String, String> result = UploadMetadataParser.parse("name=test.txt");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("test.txt", result.get("name"));
    }

    @Test
    public void parse_multipleParameters_parsed() {
        Map<String, String> result = UploadMetadataParser
                .parse("name=document.pdf&size=12345");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("document.pdf", result.get("name"));
        Assert.assertEquals("12345", result.get("size"));
    }

    @Test
    public void parse_percentEncodedSpace_decoded() {
        Map<String, String> result = UploadMetadataParser
                .parse("name=my%20document.txt");
        Assert.assertEquals("my document.txt", result.get("name"));
    }

    @Test
    public void parse_percentEncodedSpecialChars_decoded() {
        // Percent-encoded "&" and "=" characters in value
        Map<String, String> result = UploadMetadataParser
                .parse("name=special%26%3Dchars.txt");
        Assert.assertEquals("special&=chars.txt", result.get("name"));
    }

    @Test
    public void parse_nordicCharacters_decoded() {
        // åäö.txt encoded as UTF-8 percent-encoded
        Map<String, String> result = UploadMetadataParser
                .parse("name=%C3%A5%C3%A4%C3%B6.txt");
        Assert.assertEquals("åäö.txt", result.get("name"));
    }

    @Test
    public void parse_germanUmlaut_decoded() {
        // über.pdf encoded as UTF-8 percent-encoded
        Map<String, String> result = UploadMetadataParser
                .parse("name=%C3%BCber.pdf");
        Assert.assertEquals("über.pdf", result.get("name"));
    }

    @Test
    public void parse_chineseCharacters_decoded() {
        // 文件.txt encoded as UTF-8 percent-encoded
        Map<String, String> result = UploadMetadataParser
                .parse("name=%E6%96%87%E4%BB%B6.txt");
        Assert.assertEquals("文件.txt", result.get("name"));
    }

    @Test
    public void parse_emojiCharacters_decoded() {
        // emoji 😀.txt encoded as UTF-8 percent-encoded
        Map<String, String> result = UploadMetadataParser
                .parse("name=%F0%9F%98%80.txt");
        Assert.assertEquals("😀.txt", result.get("name"));
    }

    @Test
    public void parse_emptyValue_parsed() {
        Map<String, String> result = UploadMetadataParser.parse("name=");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("", result.get("name"));
    }

    @Test
    public void parse_nullHeader_emptyMap() {
        Map<String, String> result = UploadMetadataParser.parse(null);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void parse_emptyHeader_emptyMap() {
        Map<String, String> result = UploadMetadataParser.parse("");
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void parse_pairWithoutEquals_skipped() {
        Map<String, String> result = UploadMetadataParser
                .parse("name=file.txt&invalid&size=100");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("file.txt", result.get("name"));
        Assert.assertEquals("100", result.get("size"));
        Assert.assertNull(result.get("invalid"));
    }

    @Test
    public void parse_encodedKey_decoded() {
        Map<String, String> result = UploadMetadataParser
                .parse("custom%20key=value");
        Assert.assertEquals("value", result.get("custom key"));
    }

    @Test
    public void parse_threeParameters_parsed() {
        Map<String, String> result = UploadMetadataParser
                .parse("name=file.txt&size=1024&type=image");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("file.txt", result.get("name"));
        Assert.assertEquals("1024", result.get("size"));
        Assert.assertEquals("image", result.get("type"));
    }

    @Test
    public void parse_duplicateKeys_lastValueWins() {
        Map<String, String> result = UploadMetadataParser
                .parse("name=first.txt&name=second.txt");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("second.txt", result.get("name"));
    }

    @Test
    public void extractFilename_validHeader_extracted() {
        String result = UploadMetadataParser
                .extractFilename("name=document.pdf");
        Assert.assertEquals("document.pdf", result);
    }

    @Test
    public void extractFilename_withMultipleParams_extracted() {
        String result = UploadMetadataParser
                .extractFilename("name=file.txt&size=100");
        Assert.assertEquals("file.txt", result);
    }

    @Test
    public void extractFilename_encodedFilename_decoded() {
        String result = UploadMetadataParser
                .extractFilename("name=my%20file.txt");
        Assert.assertEquals("my file.txt", result);
    }

    @Test
    public void extractFilename_unicodeFilename_decoded() {
        String result = UploadMetadataParser
                .extractFilename("name=%C3%A5%C3%A4%C3%B6.txt");
        Assert.assertEquals("åäö.txt", result);
    }

    @Test
    public void extractFilename_noNameParameter_returnsNull() {
        String result = UploadMetadataParser.extractFilename("size=100");
        Assert.assertNull(result);
    }

    @Test
    public void extractFilename_nullHeader_returnsNull() {
        String result = UploadMetadataParser.extractFilename(null);
        Assert.assertNull(result);
    }

    @Test
    public void extractFilename_emptyHeader_returnsNull() {
        String result = UploadMetadataParser.extractFilename("");
        Assert.assertNull(result);
    }

    @Test
    public void extractFilename_emptyName_returnsEmpty() {
        String result = UploadMetadataParser.extractFilename("name=");
        Assert.assertEquals("", result);
    }
}
