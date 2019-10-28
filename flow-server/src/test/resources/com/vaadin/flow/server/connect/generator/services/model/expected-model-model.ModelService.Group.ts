import Account from './Account';

export default interface Group {
  creator?: Account | null;
  groupId?: string | null;
  groupName?: string | null;
}
