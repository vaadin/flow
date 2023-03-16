import { ComponentMetadata } from '../model';
import vaadinTextField from './vaadin-text-field';

export default {
    ...vaadinTextField,
    tagName: 'vaadin-number-field',
    displayName: 'NumberField'
} as ComponentMetadata;