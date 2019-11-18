import Role from './Role';

/**
 * This module has been generated from com.vaadin.flow.server.connect.generator.services.json.JsonTestService.User.
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file instead.
 */
export default interface User {
  name: string;
  optionalField?: string;
  password: string;
  roles: { [key: string]: Role; };
}