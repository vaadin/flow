// @ts-ignore
import * as connect from './connect-client.default';

export function shouldBeDisplayed1(): Promise<void> {
  return connect.client.call('DenyAllService', 'shouldBeDisplayed1');
}

export function shouldBeDisplayed2(): Promise<void> {
  return connect.client.call('DenyAllService', 'shouldBeDisplayed2');
}

export function shouldBeDisplayed3(): Promise<void> {
  return connect.client.call('DenyAllService', 'shouldBeDisplayed3', undefined, {requireCredentials: false});
}
