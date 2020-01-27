/**
 * This module is generated from DateTimeEndpoint.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module DateTimeEndpoint
 */

// @ts-ignore
import client from './connect-client.default';

export function echoDate(
  date: string
): Promise<string> {
  return client.call('DateTimeEndpoint', 'echoDate', {date});
}

export function echoInstant(
  instant: string
): Promise<string> {
  return client.call('DateTimeEndpoint', 'echoInstant', {instant});
}

export function echoListLocalDateTime(
  localDateTimeList: Array<string>
): Promise<Array<string>> {
  return client.call('DateTimeEndpoint', 'echoListLocalDateTime', {localDateTimeList});
}

export function echoLocalDate(
  localDate: string
): Promise<string> {
  return client.call('DateTimeEndpoint', 'echoLocalDate', {localDate});
}

export function echoLocalDateTime(
  localDateTime: string
): Promise<string> {
  return client.call('DateTimeEndpoint', 'echoLocalDateTime', {localDateTime});
}

export function echoMapInstant(
  mapInstant: { [key: string]: string; }
): Promise<{ [key: string]: string; }> {
  return client.call('DateTimeEndpoint', 'echoMapInstant', {mapInstant});
}