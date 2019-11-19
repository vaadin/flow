/**
 * This module is generated from NullableService.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module NullableService
 */

// @ts-ignore
import client from './connect-client.default';
import NullableModel from './com/vaadin/flow/server/connect/generator/services/nullable/NullableService/NullableModel';
import ParameterType from './com/vaadin/flow/server/connect/generator/services/nullable/NullableService/ParameterType';
import ReturnType from './com/vaadin/flow/server/connect/generator/services/nullable/NullableService/ReturnType';

export function echoMap(
  shouldBeNotNull: boolean
): Promise<{ [key: string]: NullableModel; }> {
  return client.call('NullableService', 'echoMap', {shouldBeNotNull});
}

export function echoNonNullMode(
  nullableModels: Array<NullableModel>
): Promise<NullableModel | undefined> {
  return client.call('NullableService', 'echoNonNullMode', {nullableModels});
}

export function getNotNullReturnType(): Promise<ReturnType | undefined> {
  return client.call('NullableService', 'getNotNullReturnType');
}

export function getNullableString(
  input?: string
): Promise<string> {
  return client.call('NullableService', 'getNullableString', {input});
}

export function sendParameterType(
  parameterType?: ParameterType
): Promise<void> {
  return client.call('NullableService', 'sendParameterType', {parameterType});
}

export function stringNullable(): Promise<string | undefined> {
  return client.call('NullableService', 'stringNullable');
}