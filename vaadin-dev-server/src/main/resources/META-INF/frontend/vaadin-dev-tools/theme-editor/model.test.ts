import { expect } from '@open-wc/testing';
import { combineThemes, ComponentTheme, generateRules, ThemeEditorRule, ThemePropertyValue } from './model';
import buttonMetadata from './metadata/components/vaadin-button';
import { ComponentMetadata } from './metadata/model';

describe('model', () => {
  describe('ComponentTheme', () => {
    let theme: ComponentTheme;

    beforeEach(() => {
      theme = new ComponentTheme(buttonMetadata);
    });

    it('should return null for unset properties', () => {
      expect(theme.getPropertyValue(null, 'background')).to.be.null;
      expect(theme.getPropertyValue('label', 'color')).to.be.null;
    });

    it('should add and return property values', () => {
      theme.updatePropertyValue(null, 'background', 'cornflowerblue');
      theme.updatePropertyValue('label', 'color', 'white');

      const expectedHostBackgroundValue: ThemePropertyValue = {
        partName: null,
        propertyName: 'background',
        value: 'cornflowerblue'
      };
      const expectedLabelColorValue: ThemePropertyValue = {
        partName: 'label',
        propertyName: 'color',
        value: 'white'
      };

      expect(theme.properties).to.deep.equal([expectedHostBackgroundValue, expectedLabelColorValue]);

      const hostBackgroundValue = theme.getPropertyValue(null, 'background');
      expect(hostBackgroundValue).to.exist;
      expect(hostBackgroundValue).to.deep.equal(expectedHostBackgroundValue);

      const labelColorValue = theme.getPropertyValue('label', 'color');
      expect(labelColorValue).to.exist;
      expect(labelColorValue).to.deep.equal(expectedLabelColorValue);
    });

    it('should update property values', () => {
      theme.updatePropertyValue('label', 'color', 'white');
      theme.updatePropertyValue('label', 'color', 'green');
      theme.updatePropertyValue('label', 'color', 'red');

      expect(theme.properties.length).to.equal(1);

      const labelColorValue = theme.getPropertyValue('label', 'color');
      expect(labelColorValue.value).to.equal('red');
    });

    it('should merge a list of theme property values', () => {
      theme.updatePropertyValue(null, 'background', 'cornflowerblue');
      theme.updatePropertyValue('label', 'color', 'white');

      theme.addPropertyValues([
        // Add new host prop
        { partName: null, propertyName: 'padding', value: '3px' },
        // Update existing part prop
        { partName: 'label', propertyName: 'color', value: 'red' },
        // Add new part prop
        { partName: 'label', propertyName: 'font-size', value: '20px' }
      ]);

      const expectedValues = [
        { partName: null, propertyName: 'background', value: 'cornflowerblue' },
        { partName: 'label', propertyName: 'color', value: 'red' },
        { partName: null, propertyName: 'padding', value: '3px' },
        { partName: 'label', propertyName: 'font-size', value: '20px' }
      ];

      expect(theme.properties).to.deep.equal(expectedValues);
    });

    it('should return a list of properties for a part', () => {
      theme.updatePropertyValue(null, 'background', 'cornflowerblue');
      theme.updatePropertyValue(null, 'padding', '3px');
      theme.updatePropertyValue('label', 'color', 'white');
      theme.updatePropertyValue('label', 'font-size', '20px');

      const expectedHostProperties = [
        { partName: null, propertyName: 'background', value: 'cornflowerblue' },
        { partName: null, propertyName: 'padding', value: '3px' }
      ];
      const expectedLabelProperties = [
        { partName: 'label', propertyName: 'color', value: 'white' },
        { partName: 'label', propertyName: 'font-size', value: '20px' }
      ];

      expect(theme.getPropertyValuesForPart(null)).to.deep.equal(expectedHostProperties);
      expect(theme.getPropertyValuesForPart('label')).to.deep.equal(expectedLabelProperties);
      expect(theme.getPropertyValuesForPart('unknown-part')).to.deep.equal([]);
    });
  });

  describe('combineThemes', () => {
    it('should return new theme instance', () => {
      const theme1 = new ComponentTheme(buttonMetadata);
      const theme2 = new ComponentTheme(buttonMetadata);

      const result = combineThemes(theme1, theme2);
      expect(result).to.not.equal(theme1);
      expect(result).to.not.equal(theme2);
    });

    it('should merge theme properties', () => {
      const theme1 = new ComponentTheme(buttonMetadata);
      theme1.updatePropertyValue(null, 'background', 'cornflowerblue');
      theme1.updatePropertyValue('label', 'color', 'white');

      const theme2 = new ComponentTheme(buttonMetadata);
      theme2.updatePropertyValue(null, 'padding', '3px');
      theme2.updatePropertyValue('label', 'color', 'red');
      theme2.updatePropertyValue('label', 'font-size', '20px');

      const result = combineThemes(theme1, theme2);
      const expectedValues = [
        { partName: null, propertyName: 'background', value: 'cornflowerblue' },
        { partName: 'label', propertyName: 'color', value: 'red' },
        { partName: null, propertyName: 'padding', value: '3px' },
        { partName: 'label', propertyName: 'font-size', value: '20px' }
      ];

      expect(result.properties).to.deep.equal(expectedValues);
    });

    it('should throw when less than two themes are provided', () => {
      expect(() => combineThemes()).to.throw;
      expect(() => combineThemes(new ComponentTheme(buttonMetadata))).to.throw;
    });

    it('should adopt metadata from first theme', () => {
      const fooMetadata: ComponentMetadata = {
        tagName: 'foo-component',
        displayName: 'Foo',
        parts: []
      };
      const buttonTheme = new ComponentTheme(buttonMetadata);
      const fooTheme = new ComponentTheme(fooMetadata);
      const result = combineThemes(buttonTheme, fooTheme);

      expect(result.metadata).to.equal(buttonMetadata);
    });
  });

  describe('generateRules', () => {
    it('should generate zero rules for empty theme', () => {
      const theme = new ComponentTheme(buttonMetadata);
      const rules = generateRules(theme);
      expect(rules.length).to.equal(0);
    });

    it('should generate rules for theme', () => {
      const theme = new ComponentTheme(buttonMetadata);
      theme.updatePropertyValue(null, 'background', 'cornflowerblue');
      theme.updatePropertyValue(null, 'padding', '3px');
      theme.updatePropertyValue('label', 'color', 'white');
      theme.updatePropertyValue('label', 'font-size', '20px');

      const expectedRules: ThemeEditorRule[] = [
        { selector: 'vaadin-button', property: 'background', value: 'cornflowerblue' },
        { selector: 'vaadin-button', property: 'padding', value: '3px' },
        { selector: 'vaadin-button::part(label)', property: 'color', value: 'white' },
        { selector: 'vaadin-button::part(label)', property: 'font-size', value: '20px' }
      ];

      const rules = generateRules(theme);
      expect(rules).to.deep.equal(expectedRules);
    });
  });
});
