import SubProperty from './SameClassNameModel/SubProperty';
import SubpackageSubProperty from './SubProperty';

export default interface SameClassNameModel {
  bar?: string | null;
  barbarfoo?: SubpackageSubProperty | null;
  foofoo?: SubProperty | null;
}
