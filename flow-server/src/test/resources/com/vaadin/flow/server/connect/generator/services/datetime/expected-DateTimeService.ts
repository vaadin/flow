/**
 * This module is generated from DateTimeService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module DateTimeService
 */

// @ts-ignore
import client from './connect-client.default';

export function echoDate(
  date: string
): Promise<string> {
  return client.call('DateTimeService', 'echoDate', {date});
}

export function echoInstant(
  instant: string
): Promise<string> {
  return client.call('DateTimeService', 'echoInstant', {instant});
}

export function echoListLocalDateTime(
  localDateTimeList: Array<string>
): Promise<Array<string>> {
  return client.call('DateTimeService', 'echoListLocalDateTime', {localDateTimeList});
}

export function echoLocalDate(
  localDate: string
): Promise<string> {
  return client.call('DateTimeService', 'echoLocalDate', {localDate});
}

export function echoLocalDateTime(
  localDateTime: string
): Promise<string> {
  return client.call('DateTimeService', 'echoLocalDateTime', {localDateTime});
}

export function echoMapInstant(
  mapInstant: { [key: string]: string; }
): Promise<{ [key: string]: string; }> {
  return client.call('DateTimeService', 'echoMapInstant', {mapInstant});
}