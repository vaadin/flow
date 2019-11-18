import Account from './Account';
import Group from './Group';
import ModelFromDifferentPackage from '../subpackage/ModelFromDifferentPackage';

/**
 * This module has been generated from com.vaadin.flow.server.connect.generator.services.model.ModelService.Account.
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file instead.
 */
export default interface Account {
  children: Account;
  /**
   * Multiple line description should work.This is very very very very
   * very very very very long.
   */
  groups: Array<Group>;
  modelFromDifferentPackage: ModelFromDifferentPackage;
  /**
   * Javadoc for username.
   */
  username: string;
}