import Role from './Role';

export default interface User {
  name?: string | null;
  optionalField?: string | null;
  password?: string | null;
  roles?: { [key: string]: Role | null; } | null;
}
