import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from "./vaadin-text-field";
import { iconProperties, shapeProperties, textProperties } from "./defaults";

export default {
  tagName: 'vaadin-date-picker',
  displayName: 'DatePicker',
  elements: [
    {
      selector: 'vaadin-date-picker::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
  ]
} as ComponentMetadata;