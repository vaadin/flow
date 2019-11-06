import Account from './Account';

export default interface Group {
  creator: Account;
  groupId: string;
  groupName: string;
}
