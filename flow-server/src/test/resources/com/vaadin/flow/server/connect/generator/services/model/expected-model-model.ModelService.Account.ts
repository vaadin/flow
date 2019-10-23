import Account from './Account';
import Group from './Group';
import ModelFromDifferentPackage from '../subpackage/ModelFromDifferentPackage';

export default interface Account {
  children?: Account | null;
  /**
   * Multiple line description should work.This is very very very very
   * very very very very long.
   */
  groups?: Array<Group | null> | null;
  modelFromDifferentPackage?: ModelFromDifferentPackage | null;
  /**
   * Javadoc for username.
   */
  username?: string | null;
}