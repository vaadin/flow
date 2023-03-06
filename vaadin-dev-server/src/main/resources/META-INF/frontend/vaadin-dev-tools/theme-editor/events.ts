import { ComponentPartMetadata, CssPropertyMetadata } from './metadata/model';

export class ThemePropertyValueChangeEvent extends CustomEvent<{
  part: ComponentPartMetadata | null;
  property: CssPropertyMetadata;
  value: string;
}> {
  constructor(part: ComponentPartMetadata | null, property: CssPropertyMetadata, value: string) {
    super('theme-property-value-change', {
      bubbles: true,
      composed: true,
      detail: { part, property, value }
    });
  }
}
