// @ts-ignore
import client from './connect-client.default';

export function echoDate(
  date: string | null
): Promise<string | null> {
  return client.call('DateTimeService', 'echoDate', {date});
}

export function echoInstant(
  instant: string | null
): Promise<string | null> {
  return client.call('DateTimeService', 'echoInstant', {instant});
}

export function echoListLocalDateTime(
  localDateTimeList: Array<string | null> | null
): Promise<Array<string | null> | null> {
  return client.call('DateTimeService', 'echoListLocalDateTime', {localDateTimeList});
}

export function echoLocalDate(
  localDate: string | null
): Promise<string | null> {
  return client.call('DateTimeService', 'echoLocalDate', {localDate});
}

export function echoLocalDateTime(
  localDateTime: string | null
): Promise<string | null> {
  return client.call('DateTimeService', 'echoLocalDateTime', {localDateTime});
}

export function echoMapInstant(
  mapInstant: { [key: string]: string | null; } | null
): Promise<{ [key: string]: string | null; } | null> {
  return client.call('DateTimeService', 'echoMapInstant', {mapInstant});
}
