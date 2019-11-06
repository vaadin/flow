import Version from '../../../../../../../../fasterxml/jackson/core/Version';
import ParentModel from './ParentModel';
import ArraySchema from '../../../../../../../../../io/swagger/v3/oas/models/media/ArraySchema';

export default interface ChildModel extends ParentModel {
  abc: Array<{ [key: string]: Version; }>;
  def: Array<{ [key: string]: { [key: string]: Version; }; }>;
  name: string;
  testObject: ArraySchema;
}