import { expect } from '@open-wc/testing';
import { ComponentTheme, generateThemeRule, ThemePropertyValue } from './model';
import buttonMetadata from './metadata/components/vaadin-button';
import { ComponentMetadata } from './metadata/model';
import { ServerCssRule } from './api';
import { testElementMetadata } from './tests/utils';

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
        value: 'cornflowerblue',
        modified: false
      };
      const expectedLabelColorValue: ThemePropertyValue = {
        partName: 'label',
        propertyName: 'color',
        value: 'white',
        modified: false
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
        { partName: null, propertyName: 'padding', value: '3px', modified: false },
        // Update existing part prop
        { partName: 'label', propertyName: 'color', value: 'red', modified: false },
        // Add new part prop
        { partName: 'label', propertyName: 'font-size', value: '20px', modified: false }
      ]);

      const expectedValues = [
        { partName: null, propertyName: 'background', value: 'cornflowerblue', modified: false },
        { partName: 'label', propertyName: 'color', value: 'red', modified: false },
        { partName: null, propertyName: 'padding', value: '3px', modified: false },
        { partName: 'label', propertyName: 'font-size', value: '20px', modified: false }
      ];

      expect(theme.properties).to.deep.equal(expectedValues);
    });

    it('should update modified state when merging properties', () => {
      theme.updatePropertyValue(null, 'background', 'cornflowerblue');

      theme.addPropertyValues([
        { partName: null, propertyName: 'background', value: 'red', modified: true },
        { partName: null, propertyName: 'padding', value: '3px', modified: true }
      ]);

      const expectedValues = [
        { partName: null, propertyName: 'background', value: 'red', modified: true },
        { partName: null, propertyName: 'padding', value: '3px', modified: true }
      ];

      expect(theme.properties).to.deep.equal(expectedValues);
    });

    it('should return a list of properties for a part', () => {
      theme.updatePropertyValue(null, 'background', 'cornflowerblue');
      theme.updatePropertyValue(null, 'padding', '3px');
      theme.updatePropertyValue('label', 'color', 'white');
      theme.updatePropertyValue('label', 'font-size', '20px');

      const expectedHostProperties = [
        { partName: null, propertyName: 'background', value: 'cornflowerblue', modified: false },
        { partName: null, propertyName: 'padding', value: '3px', modified: false }
      ];
      const expectedLabelProperties = [
        { partName: 'label', propertyName: 'color', value: 'white', modified: false },
        { partName: 'label', propertyName: 'font-size', value: '20px', modified: false }
      ];

      expect(theme.getPropertyValuesForPart(null)).to.deep.equal(expectedHostProperties);
      expect(theme.getPropertyValuesForPart('label')).to.deep.equal(expectedLabelProperties);
      expect(theme.getPropertyValuesForPart('unknown-part')).to.deep.equal([]);
    });
  });

  describe('combine', () => {
    it('should return new theme instance', () => {
      const theme1 = new ComponentTheme(buttonMetadata);
      const theme2 = new ComponentTheme(buttonMetadata);

      const result = ComponentTheme.combine(theme1, theme2);
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

      const result = ComponentTheme.combine(theme1, theme2);
      const expectedValues = [
        { partName: null, propertyName: 'background', value: 'cornflowerblue', modified: false },
        { partName: 'label', propertyName: 'color', value: 'red', modified: false },
        { partName: null, propertyName: 'padding', value: '3px', modified: false },
        { partName: 'label', propertyName: 'font-size', value: '20px', modified: false }
      ];

      expect(result.properties).to.deep.equal(expectedValues);
    });

    it('should throw when less than two themes are provided', () => {
      expect(() => ComponentTheme.combine()).to.throw;
      expect(() => ComponentTheme.combine(new ComponentTheme(buttonMetadata))).to.throw;
    });

    it('should adopt metadata from first theme', () => {
      const fooMetadata: ComponentMetadata = {
        tagName: 'foo-component',
        displayName: 'Foo',
        properties: [],
        parts: []
      };
      const buttonTheme = new ComponentTheme(buttonMetadata);
      const fooTheme = new ComponentTheme(fooMetadata);
      const result = ComponentTheme.combine(buttonTheme, fooTheme);

      expect(result.metadata).to.equal(buttonMetadata);
    });
  });

  describe('fromServerRules', () => {
    it('should create empty theme from empty rules', () => {
      const theme = ComponentTheme.fromServerRules(buttonMetadata, []);

      expect(theme.properties.length).to.equal(0);
    });

    it('should create theme from rules', () => {
      const serverRules: ServerCssRule[] = [
        {
          tagName: 'test-element',
          partName: null,
          properties: {
            background: 'cornflowerblue',
            padding: '3px'
          }
        },
        {
          tagName: 'test-element',
          partName: 'label',
          properties: {
            color: 'red',
            'font-size': '20px'
          }
        }
      ];
      const expectedProperties = [
        { partName: null, propertyName: 'padding', value: '3px', modified: true },
        { partName: null, propertyName: 'background', value: 'cornflowerblue', modified: true },
        { partName: 'label', propertyName: 'color', value: 'red', modified: true },
        { partName: 'label', propertyName: 'font-size', value: '20px', modified: true }
      ];

      const theme = ComponentTheme.fromServerRules(testElementMetadata, serverRules);
      expect(theme.metadata).to.equal(testElementMetadata);
      expect(theme.properties).to.deep.equal(expectedProperties);
    });

    it('should ignore unknown selectors and properties', () => {
      const serverRules: ServerCssRule[] = [
        {
          tagName: 'test-element',
          partName: null,
          properties: {
            foo: 'cornflowerblue'
          }
        },
        {
          tagName: 'test-element',
          partName: 'label',
          properties: {
            bar: '20px'
          }
        },
        {
          tagName: 'test-element',
          partName: 'foo',
          properties: {
            color: 'cornflowerblue',
            background: 'cornflowerblue'
          }
        }
      ];

      const theme = ComponentTheme.fromServerRules(testElementMetadata, serverRules);
      expect(theme.properties.length).to.equal(0);
    });
  });

  describe('generateRule', () => {
    it('should generate rules for property changes', () => {
      const rules = [
        generateThemeRule('vaadin-button', null, 'background', 'cornflowerblue'),
        generateThemeRule('vaadin-button', null, 'padding', '3px'),
        generateThemeRule('vaadin-button', 'label', 'color', 'white'),
        generateThemeRule('vaadin-button', 'label', 'font-size', '20px')
      ];

      const expectedRules: ServerCssRule[] = [
        { tagName: 'vaadin-button', partName: null, properties: { background: 'cornflowerblue' } },
        { tagName: 'vaadin-button', partName: null, properties: { padding: '3px' } },
        { tagName: 'vaadin-button', partName: 'label', properties: { color: 'white' } },
        { tagName: 'vaadin-button', partName: 'label', properties: { 'font-size': '20px' } }
      ];

      expect(rules).to.deep.equal(expectedRules);
    });
  });
});
