import GrandParentModel from './GrandParentModel';

export default interface ParentModel extends GrandParentModel {
  id: string;
}