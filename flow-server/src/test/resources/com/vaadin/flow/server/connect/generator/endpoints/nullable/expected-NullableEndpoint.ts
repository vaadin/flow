/**
 * This module is generated from NullableEndpoint.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module NullableEndpoint
 */

// @ts-ignore
import client from './connect-client.default';
import NullableModel from './com/vaadin/flow/server/connect/generator/endpoints/nullable/NullableEndpoint/NullableModel';
import ParameterType from './com/vaadin/flow/server/connect/generator/endpoints/nullable/NullableEndpoint/ParameterType';
import ReturnType from './com/vaadin/flow/server/connect/generator/endpoints/nullable/NullableEndpoint/ReturnType';

export function echoMap(
  shouldBeNotNull: boolean
): Promise<{ [key: string]: NullableModel; }> {
  return client.call('NullableEndpoint', 'echoMap', {shouldBeNotNull});
}

export function echoNonNullMode(
  nullableModels: Array<NullableModel>
): Promise<NullableModel | undefined> {
  return client.call('NullableEndpoint', 'echoNonNullMode', {nullableModels});
}

export function getNotNullReturnType(): Promise<ReturnType | undefined> {
  return client.call('NullableEndpoint', 'getNotNullReturnType');
}

export function getNullableString(
  input?: string
): Promise<string> {
  return client.call('NullableEndpoint', 'getNullableString', {input});
}

export function sendParameterType(
  parameterType?: ParameterType
): Promise<void> {
  return client.call('NullableEndpoint', 'sendParameterType', {parameterType});
}

export function stringNullable(): Promise<string | undefined> {
  return client.call('NullableEndpoint', 'stringNullable');
}