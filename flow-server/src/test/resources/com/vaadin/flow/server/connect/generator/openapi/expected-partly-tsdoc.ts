/**
 * This class is used for OpenApi generator test
 *
 * This module has been generated from GeneratorTestClass.java
 * @module GeneratorTestClass
 */

// @ts-ignore
import * as connect from './connect-client.default';
import ComplexRequest from './ComplexRequest';
import ComplexResponse from './ComplexResponse';

export function complexEntitiesTest(
  request?: ComplexRequest
): Promise<ComplexResponse> {
  return connect.client.call('GeneratorTestClass', 'complexEntitiesTest', {request: connect.nullIfUndefined(request)});
}