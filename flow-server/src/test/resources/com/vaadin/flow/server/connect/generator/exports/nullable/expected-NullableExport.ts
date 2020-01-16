/**
 * This module is generated from NullableExport.java
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 * @module NullableExport
 */

// @ts-ignore
import client from './connect-client.default';
import NullableModel from './com/vaadin/flow/server/connect/generator/exports/nullable/NullableExport/NullableModel';
import ParameterType from './com/vaadin/flow/server/connect/generator/exports/nullable/NullableExport/ParameterType';
import ReturnType from './com/vaadin/flow/server/connect/generator/exports/nullable/NullableExport/ReturnType';

export function echoMap(
  shouldBeNotNull: boolean
): Promise<{ [key: string]: NullableModel; }> {
  return client.call('NullableExport', 'echoMap', {shouldBeNotNull});
}

export function echoNonNullMode(
  nullableModels: Array<NullableModel>
): Promise<NullableModel | undefined> {
  return client.call('NullableExport', 'echoNonNullMode', {nullableModels});
}

export function getNotNullReturnType(): Promise<ReturnType | undefined> {
  return client.call('NullableExport', 'getNotNullReturnType');
}

export function getNullableString(
  input?: string
): Promise<string> {
  return client.call('NullableExport', 'getNullableString', {input});
}

export function sendParameterType(
  parameterType?: ParameterType
): Promise<void> {
  return client.call('NullableExport', 'sendParameterType', {parameterType});
}

export function stringNullable(): Promise<string | undefined> {
  return client.call('NullableExport', 'stringNullable');
}