import { expect } from '@open-wc/testing';
import { ComponentTheme, generateRules, Theme, ThemeEditorRule, ThemePropertyValue } from './model';
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
        { partName: null, propertyName: 'background', value: 'cornflowerblue' },
        { partName: 'label', propertyName: 'color', value: 'red' },
        { partName: null, propertyName: 'padding', value: '3px' },
        { partName: 'label', propertyName: 'font-size', value: '20px' }
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
        parts: [],
      };
      const buttonTheme = new ComponentTheme(buttonMetadata);
      const fooTheme = new ComponentTheme(fooMetadata);
      const result = ComponentTheme.combine(buttonTheme, fooTheme);

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

  describe('Theme', () => {
    let theme: Theme;

    beforeEach(() => {
      theme = new Theme();
    });

    it('should return null for non-existing component themes', () => {
      expect(theme.getComponentTheme('vaadin-button')).to.be.null;
    });

    it('should add and return component themes', () => {
      const buttonTheme = new ComponentTheme(buttonMetadata);
      buttonTheme.updatePropertyValue(null, 'background', 'cornflowerblue');
      buttonTheme.updatePropertyValue('label', 'color', 'white');

      theme.updateComponentTheme(buttonTheme);

      const storedTheme = theme.getComponentTheme('vaadin-button');
      expect(storedTheme).to.deep.equal(buttonTheme);
      expect(theme.componentThemes).to.deep.equal([buttonTheme]);
    });

    it('should update component themes', () => {
      const buttonTheme = new ComponentTheme(buttonMetadata);
      buttonTheme.updatePropertyValue(null, 'background', 'cornflowerblue');
      buttonTheme.updatePropertyValue('label', 'color', 'white');
      theme.updateComponentTheme(buttonTheme);

      const updatedTheme = new ComponentTheme(buttonMetadata);
      updatedTheme.updatePropertyValue(null, 'padding', '3px');
      updatedTheme.updatePropertyValue('label', 'color', 'red');
      updatedTheme.updatePropertyValue('label', 'font-size', '20px');
      theme.updateComponentTheme(updatedTheme);

      const expectedTheme = new ComponentTheme(buttonMetadata);
      expectedTheme.updatePropertyValue(null, 'background', 'cornflowerblue');
      expectedTheme.updatePropertyValue(null, 'padding', '3px');
      expectedTheme.updatePropertyValue('label', 'color', 'red');
      expectedTheme.updatePropertyValue('label', 'font-size', '20px');

      const storedTheme = theme.getComponentTheme('vaadin-button');
      expect(storedTheme).to.deep.equal(expectedTheme);
    });

    it('should not store references to passed component themes', () => {
      const buttonTheme = new ComponentTheme(buttonMetadata);
      theme.updateComponentTheme(buttonTheme);

      const storedTheme = theme.getComponentTheme('vaadin-button');
      expect(storedTheme).to.not.equal(buttonTheme);
    });

    it('should clone theme', () => {
      const buttonTheme = new ComponentTheme(buttonMetadata);
      buttonTheme.updatePropertyValue(null, 'background', 'cornflowerblue');
      buttonTheme.updatePropertyValue('label', 'color', 'white');
      theme.updateComponentTheme(buttonTheme);

      const clonedTheme = theme.clone();
      expect(clonedTheme).to.not.equal(theme);
      expect(clonedTheme).to.deep.equal(theme);
    })
  });
});
