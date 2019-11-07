import SubProperty from './SameClassNameModel/SubProperty';
import SubpackageSubProperty from './SubProperty';

export default interface SameClassNameModel {
  bar: string;
  barbarfoo: SubpackageSubProperty;
  foofoo: SubProperty;
}
