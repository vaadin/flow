/*
 * Copyright 2000-2026 Vaadin Ltd.
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

// TypeScript port of the full public API of
// com.vaadin.flow.shared.util.SharedUtil, built alongside the Java version.
// Java-private helpers are kept as non-exported module-local functions.

/**
 * Adds the given query parameters to a URI, before any fragment. Mirrors
 * SharedUtil.addGetParameters.
 *
 * @param uri The uri to which the parameters should be added.
 * @param extraParams One or more parameters in the format "a=b" or "c=d&e=f". An empty string is allowed but will not modify the url.
 * @return The modified URI with the get parameters in extraParams added.
 */
export function addGetParameters(uri: string, extraParams: string | null): string {
  if (extraParams === null || extraParams.length === 0) {
    return uri;
  }
  // RFC 3986: the query starts at the first "?" and ends at "#" or the URI end.
  let base = uri;
  let fragment: string | null = null;
  const hashPosition = base.indexOf('#');
  if (hashPosition !== -1) {
    fragment = base.substring(hashPosition);
    base = base.substring(0, hashPosition);
  }

  base += base.includes('?') ? '&' : '?';
  base += extraParams;

  if (fragment !== null) {
    base += fragment;
  }
  return base;
}

/**
 * Adds a single `parameter=value` query parameter to a URI. Mirrors
 * SharedUtil.addGetParameter.
 *
 * @param uri the URI to which the parameter should be added.
 * @param parameter the name of the parameter
 * @param value the value of the parameter
 * @return The modified URI with the parameter added
 */
export function addGetParameter(uri: string, parameter: string, value: string | number): string {
  return addGetParameters(uri, `${parameter}=${value}`);
}

/**
 * Tells whether the given single character is an upper case letter, mirroring
 * Java's Character.isUpperCase for the characters exercised by these helpers.
 * Digits, spaces and other non-cased characters are not upper case because
 * their upper and lower case forms are identical.
 */
function isUpperCase(c: string): boolean {
  return c !== c.toLowerCase() && c === c.toUpperCase();
}

/**
 * Splits a string on a literal separator, mirroring Java's
 * String.split(regex) with the default (zero) limit: trailing empty strings
 * are removed. The empty input string still yields a single empty element, as
 * it does in Java.
 */
function splitRemovingTrailingEmpty(value: string, separator: string): string[] {
  const parts = value.split(separator);
  if (value === '') {
    // Java's split returns a single empty string for empty input.
    return parts;
  }
  let end = parts.length;
  while (end > 0 && parts[end - 1] === '') {
    end--;
  }
  return parts.slice(0, end);
}

/**
 * Trims trailing slashes (if any) from a string.
 *
 * @param value The string value to be trimmed. Cannot be null.
 * @return String value without trailing slashes.
 */
export function trimTrailingSlashes(value: string): string {
  return value.replace(/\/*$/, '');
}

/**
 * Tells whether a word ends at the given upper case character while splitting
 * a camelCase string.
 */
function isWordComplete(camelCaseString: string, i: number): boolean {
  if (i === 0) {
    // Word can't end at the beginning
    return false;
  } else if (!isUpperCase(camelCaseString.charAt(i - 1))) {
    // Word ends if previous char wasn't upper case
    return true;
  } else if (i + 1 < camelCaseString.length && !isUpperCase(camelCaseString.charAt(i + 1))) {
    // Word ends if next char isn't upper case
    return true;
  } else {
    return false;
  }
}

/**
 * Splits a camelCaseString into an array of words with the casing preserved.
 *
 * @param camelCaseString The input string in camelCase format
 * @return An array with one entry per word in the input string
 */
export function splitCamelCase(camelCaseString: string): string[] {
  let sb = '';
  for (let i = 0; i < camelCaseString.length; i++) {
    const c = camelCaseString.charAt(i);
    if (isUpperCase(c) && isWordComplete(camelCaseString, i)) {
      sb += ' ';
    }
    sb += c;
  }
  return splitRemovingTrailingEmpty(sb, ' ');
}

/**
 * Joins the words in the input array together into a single string by
 * inserting the separator string between each word.
 *
 * @param parts The array of words
 * @param separator The separator string to use between words
 * @return The constructed string of words and separators
 */
export function join(parts: string[], separator: string): string {
  let sb = '';
  for (const part of parts) {
    sb += part;
    sb += separator;
  }
  return sb.substring(0, sb.length - separator.length);
}

/**
 * Capitalizes the first character in the given string in a way suitable for
 * use in code (methods, properties etc).
 *
 * @param string The string to capitalize
 * @return The capitalized string
 */
export function capitalize(string: string | null): string | null {
  if (string === null) {
    return null;
  }

  if (string.length <= 1) {
    return string.toUpperCase();
  }

  return string.substring(0, 1).toUpperCase() + string.substring(1);
}

/**
 * Changes the first character in the given string to lower case in a way
 * suitable for use in code (methods, properties etc).
 *
 * @param string The string to change
 * @return The string with initial character turned into lower case
 */
export function firstToLower(string: string | null): string | null {
  if (string === null) {
    return null;
  }

  if (string.length <= 1) {
    return string.toLowerCase();
  }

  return string.substring(0, 1).toLowerCase() + string.substring(1);
}

/**
 * Converts a camelCaseString to a human friendly format (Camel case string).
 * In general splits words when the casing changes but also handles special
 * cases such as consecutive upper case characters. Examples:
 * `MyBeanContainer` becomes `My Bean Container`, `AwesomeURLFactory` becomes
 * `Awesome URL Factory`, `SomeUriAction` becomes `Some Uri Action`.
 *
 * @param camelCaseString The input string in camelCase format
 * @return A human friendly version of the input
 */
export function camelCaseToHumanFriendly(camelCaseString: string): string {
  const parts = splitCamelCase(camelCaseString);
  for (let i = 0; i < parts.length; i++) {
    parts[i] = capitalize(parts[i]) as string;
  }
  return join(parts, ' ');
}

/**
 * Converts a property id to a human friendly format. Handles nested
 * properties by only considering the last part, e.g. "address.streetName" is
 * equal to "streetName" for this method.
 *
 * @param propertyId The propertyId to format
 * @return A human friendly version of the property id
 */
export function propertyIdToHumanFriendly(propertyId: unknown): string {
  let string = String(propertyId);
  if (string.length === 0) {
    return '';
  }

  // For nested properties, only use the last part
  const dotLocation = string.lastIndexOf('.');
  if (dotLocation > 0 && dotLocation < string.length - 1) {
    string = string.substring(dotLocation + 1);
  }

  return camelCaseToHumanFriendly(string);
}

/**
 * Converts a dash ("-") separated string into camelCase. Examples: `foo`
 * becomes `foo`, `foo-bar` becomes `fooBar`, `foo--bar` becomes `fooBar`.
 *
 * @param dashSeparated The dash separated string to convert
 * @return a camelCase version of the input string
 */
export function dashSeparatedToCamelCase(dashSeparated: string | null): string | null {
  if (dashSeparated === null) {
    return null;
  }
  const parts = splitRemovingTrailingEmpty(dashSeparated, '-');
  for (let i = 1; i < parts.length; i++) {
    parts[i] = capitalize(parts[i]) as string;
  }

  return join(parts, '');
}

/**
 * Converts a camelCase string into dash ("-") separated. Examples: `foo`
 * becomes `foo`, `fooBar` becomes `foo-bar`, `MyBeanContainer` becomes
 * `-my-bean-container`, `AwesomeURLFactory` becomes `-awesome-uRL-factory`,
 * `someUriAction` becomes `some-uri-action`.
 *
 * @param camelCaseString The input string in camelCase format
 * @return A dash separated version of the input
 */
export function camelCaseToDashSeparated(camelCaseString: string | null): string | null {
  if (camelCaseString === null) {
    return null;
  }
  const parts = splitCamelCase(camelCaseString);
  if (parts[0].length >= 1 && isUpperCase(parts[0].charAt(0))) {
    // starts with upper case
    parts[0] = `-${firstToLower(parts[0]) as string}`;
  }
  for (let i = 1; i < parts.length; i++) {
    parts[i] = firstToLower(parts[i]) as string;
  }
  return join(parts, '-');
}

/**
 * Converts a UpperCamelCase string into dash ("-") separated lowercase.
 * Examples: `foo` becomes `foo`, `fooBar` becomes `foo-bar`,
 * `MyBeanContainer` becomes `my-bean-container`, `AwesomeURLFactory` becomes
 * `awesome-url-factory`, `someUriAction` becomes `some-uri-action`.
 *
 * @param upperCamelCaseString The input string in UpperCamelCase format
 * @return A dash separated lowercase version of the input
 */
export function upperCamelCaseToDashSeparatedLowerCase(upperCamelCaseString: string | null): string | null {
  if (upperCamelCaseString === null) {
    return null;
  }
  return (camelCaseToDashSeparated(firstToLower(upperCamelCaseString)) as string).toLowerCase();
}

/**
 * Prepend the given url with the prefix if it is not absolute and doesn't have
 * a protocol.
 *
 * @param url url to check
 * @param prefix prefix to add to url
 * @return prefixed url or url if absolute or has a protocol
 */
export function prefixIfRelative(url: string, prefix: string): string {
  // Absolute
  if (url.startsWith('/')) {
    return url;
  }

  // Has a protocol
  // https://tools.ietf.org/html/rfc3986#section-3.1
  if (/^[a-zA-Z0-9.\-+]+:.*$/.test(url)) {
    return url;
  }

  return prefix + url;
}
