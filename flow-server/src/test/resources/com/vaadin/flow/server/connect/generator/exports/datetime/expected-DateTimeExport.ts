/**
 * This module is generated from DateTimeExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module DateTimeExport
 */

// @ts-ignore
import client from './connect-client.default';

export function echoDate(
  date: string
): Promise<string> {
  return client.call('DateTimeExport', 'echoDate', {date});
}

export function echoInstant(
  instant: string
): Promise<string> {
  return client.call('DateTimeExport', 'echoInstant', {instant});
}

export function echoListLocalDateTime(
  localDateTimeList: Array<string>
): Promise<Array<string>> {
  return client.call('DateTimeExport', 'echoListLocalDateTime', {localDateTimeList});
}

export function echoLocalDate(
  localDate: string
): Promise<string> {
  return client.call('DateTimeExport', 'echoLocalDate', {localDate});
}

export function echoLocalDateTime(
  localDateTime: string
): Promise<string> {
  return client.call('DateTimeExport', 'echoLocalDateTime', {localDateTime});
}

export function echoMapInstant(
  mapInstant: { [key: string]: string; }
): Promise<{ [key: string]: string; }> {
  return client.call('DateTimeExport', 'echoMapInstant', {mapInstant});
}