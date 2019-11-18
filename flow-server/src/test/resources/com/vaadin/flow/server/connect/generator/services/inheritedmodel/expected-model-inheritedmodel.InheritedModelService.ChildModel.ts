import Version from '../../../../../../../../fasterxml/jackson/core/Version';
import ParentModel from './ParentModel';
import ArraySchema from '../../../../../../../../../io/swagger/v3/oas/models/media/ArraySchema';

/**
 * This module has been generated from com.vaadin.flow.server.connect.generator.services.inheritedmodel.InheritedModelService.ChildModel.
 * All changes to this file are overridden. Please consider to make changes in the corresponding Java file instead.
 */
export default interface ChildModel extends ParentModel {
  abc: Array<{ [key: string]: Version; }>;
  def: Array<{ [key: string]: { [key: string]: Version; }; }>;
  name: string;
  testObject: ArraySchema;
}