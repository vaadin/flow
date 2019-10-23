import Version from '../../../../../../../../fasterxml/jackson/core/Version';
import ParentModel from './ParentModel';
import ArraySchema from '../../../../../../../../../io/swagger/v3/oas/models/media/ArraySchema';

export default interface ChildModel extends ParentModel {
  abc?: Array<{ [key: string]: Version | null; } | null> | null;
  def?: Array<{ [key: string]: { [key: string]: Version | null; } | null; } | null> | null;
  name?: string | null;
  testObject?: ArraySchema | null;
}