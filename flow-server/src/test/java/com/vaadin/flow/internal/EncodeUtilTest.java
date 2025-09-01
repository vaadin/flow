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

package com.vaadin.flow.internal;

import org.junit.Assert;
import org.junit.Test;

public class EncodeUtilTest {

    @Test(expected = NullPointerException.class)
    public void rfc5987Encode_withNull_nullPointerException() {
        EncodeUtil.rfc5987Encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void rfc2047Encode_withNull_nullPointerException() {
        EncodeUtil.rfc2047Encode(null);
    }

    @Test
    public void rfc5987Encode_asciiCharacters() {
        String input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$&'*+-.^_`|~";
        Assert.assertEquals(
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$&%27%2A+-.^_`|~",
                EncodeUtil.rfc5987Encode(input));
    }

    // UTF-8 Basic Latin & Controls
    @Test
    public void rfc5987Encode_unicodeCharacters0to126() throws Exception {
        StringBuilder text = new StringBuilder();
        for (int codePoint = 0; codePoint <= 126; codePoint++) {
            text.append(new String(new int[] { codePoint }, 0, 1));
        }
        Assert.assertEquals(
                "%00%01%02%03%04%05%06%07%08%09%0A%0B%0C%0D%0E%0F%10%11%12%13%14%15%16%17%18%19%1A%1B%1C%1D%1E%1F%20!%22#$%25&%27%28%29%2A+%2C-.%2F0123456789%3A%3B%3C%3D%3E%3F%40ABCDEFGHIJKLMNOPQRSTUVWXYZ%5B%5C%5D^_`abcdefghijklmnopqrstuvwxyz%7B|%7D~",
                EncodeUtil.rfc5987Encode(text.toString()));
    }

    // UTF-8 Latin-1 Supplement
    @Test
    public void rfc5987Encode_unicodeLatin1SupplementCharacters()
            throws Exception {
        Assert.assertEquals("%E2%82%AC%20%C3%BF",
                EncodeUtil.rfc5987Encode("€ ÿ"));
    }

    // UTF-8 Latin Extended A
    @Test
    public void rfc5987Encode_unicodeLatinExtendACharacters() throws Exception {
        Assert.assertEquals("%C4%80%C4%81", EncodeUtil.rfc5987Encode("Āā"));
    }

    @Test
    public void rfc2047Encode_asciiCharacters() {
        String input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$&'*+-.^_`|~ ?=\"";
        Assert.assertEquals(
                "=?UTF-8?Q?abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$&'*+-.^=5F`|~_=3F=3D=22?=",
                EncodeUtil.rfc2047Encode(input));
    }

    @Test
    public void rfc2047Encode_nonAsciiCharacters() {
        String input = "Řřüñîçødë 1中文 € ÿĀā";
        Assert.assertEquals(
                "=?UTF-8?Q?=C5=98=C5=99=C3=BC=C3=B1=C3=AE=C3=A7=C3=B8d=C3=AB_1=E4=B8=AD=E6=96=87_=E2=82=AC_=C3=BF=C4=80=C4=81?=",
                EncodeUtil.rfc2047Encode(input));
    }

    @Test
    public void isPureUSASCII_withAsciiOnly_returnTrue() {
        String input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$&'*+-.^_`|~ ?=\"";
        Assert.assertTrue(EncodeUtil.isPureUSASCII(input));
    }

    @Test
    public void isPureUSASCII_withNonAscii_returnFalse() {
        String input = "Řřüñîçøë中文€ÿĀā";
        input.chars().forEach(c -> {
            Assert.assertFalse(
                    "Character " + (char) c + " should not be US-ASCII",
                    EncodeUtil.isPureUSASCII(Character.toString((char) c)));
        });
    }
}
