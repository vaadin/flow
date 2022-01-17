import Role from './Role';

/**
 * This module is generated from dev.hilla.generator.endpoints.json.JsonTestEndpoint.User.
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file if necessary.
 */

export default interface User {
  name?: string;
  password?: string;
  roles?: Record<string, Role | undefined>;
  optionalField?: string;
}
