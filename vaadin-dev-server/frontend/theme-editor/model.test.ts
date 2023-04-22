import { expect } from '@open-wc/testing';
import {
  ComponentTheme,
  generateThemeRule,
  generateThemeRuleCss,
  SelectorScope,
  ThemePropertyValue,
  ThemeScope
} from './model';
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
      expect(theme.getPropertyValue('vaadin-button', 'background')).to.be.null;
      expect(theme.getPropertyValue('vaadin-button::part(label)', 'color')).to.be.null;
    });

    it('should add and return property values', () => {
      theme.updatePropertyValue('vaadin-button', 'background', 'cornflowerblue');
      theme.updatePropertyValue('vaadin-button::part(label)', 'color', 'white');

      const expectedHostBackgroundValue: ThemePropertyValue = {
        elementSelector: 'vaadin-button',
        propertyName: 'background',
        value: 'cornflowerblue',
        modified: false
      };
      const expectedLabelColorValue: ThemePropertyValue = {
        elementSelector: 'vaadin-button::part(label)',
        propertyName: 'color',
        value: 'white',
        modified: false
      };

      expect(theme.properties).to.deep.equal([expectedHostBackgroundValue, expectedLabelColorValue]);

      const hostBackgroundValue = theme.getPropertyValue('vaadin-button', 'background');
      expect(hostBackgroundValue).to.exist;
      expect(hostBackgroundValue).to.deep.equal(expectedHostBackgroundValue);

      const labelColorValue = theme.getPropertyValue('vaadin-button::part(label)', 'color');
      expect(labelColorValue).to.exist;
      expect(labelColorValue).to.deep.equal(expectedLabelColorValue);
    });

    it('should update property values', () => {
      theme.updatePropertyValue('vaadin-button::part(label)', 'color', 'white');
      theme.updatePropertyValue('vaadin-button::part(label)', 'color', 'green');
      theme.updatePropertyValue('vaadin-button::part(label)', 'color', 'red');

      expect(theme.properties.length).to.equal(1);

      const labelColorValue = theme.getPropertyValue('vaadin-button::part(label)', 'color');
      expect(labelColorValue.value).to.equal('red');
    });

    it('should remove property values if value is an empty string', () => {
      theme.updatePropertyValue('vaadin-button::part(label)', 'color', 'white');
      theme.updatePropertyValue('vaadin-button::part(label)', 'color', '');

      expect(theme.properties.length).to.equal(0);
    });

    it('should not add property value if value is an empty string', () => {
      theme.updatePropertyValue('vaadin-button::part(label)', 'color', '');

      expect(theme.properties.length).to.equal(0);
    });

    it('should merge a list of theme property values', () => {
      theme.updatePropertyValue('vaadin-button', 'background', 'cornflowerblue');
      theme.updatePropertyValue('vaadin-button::part(label)', 'color', 'white');

      theme.addPropertyValues([
        // Add new host prop
        { elementSelector: 'vaadin-button', propertyName: 'padding', value: '3px', modified: false },
        // Update existing part prop
        { elementSelector: 'vaadin-button::part(label)', propertyName: 'color', value: 'red', modified: false },
        // Add new part prop
        {
          elementSelector: 'vaadin-button::part(label)',
          propertyName: 'font-size',
          value: '20px',
          modified: false
        }
      ]);

      const expectedValues: ThemePropertyValue[] = [
        {
          elementSelector: 'vaadin-button',
          propertyName: 'background',
          value: 'cornflowerblue',
          modified: false
        },
        { elementSelector: 'vaadin-button::part(label)', propertyName: 'color', value: 'red', modified: false },
        { elementSelector: 'vaadin-button', propertyName: 'padding', value: '3px', modified: false },
        {
          elementSelector: 'vaadin-button::part(label)',
          propertyName: 'font-size',
          value: '20px',
          modified: false
        }
      ];

      expect(theme.properties).to.deep.equal(expectedValues);
    });

    it('should update modified state when merging properties', () => {
      theme.updatePropertyValue('vaadin-button', 'background', 'cornflowerblue');

      theme.addPropertyValues([
        { elementSelector: 'vaadin-button', propertyName: 'background', value: 'red', modified: true },
        { elementSelector: 'vaadin-button', propertyName: 'padding', value: '3px', modified: true }
      ]);

      const expectedValues: ThemePropertyValue[] = [
        { elementSelector: 'vaadin-button', propertyName: 'background', value: 'red', modified: true },
        { elementSelector: 'vaadin-button', propertyName: 'padding', value: '3px', modified: true }
      ];

      expect(theme.properties).to.deep.equal(expectedValues);
    });

    it('should return a list of properties for a part', () => {
      theme.updatePropertyValue('vaadin-button', 'background', 'cornflowerblue');
      theme.updatePropertyValue('vaadin-button', 'padding', '3px');
      theme.updatePropertyValue('vaadin-button::part(label)', 'color', 'white');
      theme.updatePropertyValue('vaadin-button::part(label)', 'font-size', '20px');

      const expectedHostProperties: ThemePropertyValue[] = [
        {
          elementSelector: 'vaadin-button',
          propertyName: 'background',
          value: 'cornflowerblue',
          modified: false
        },
        { elementSelector: 'vaadin-button', propertyName: 'padding', value: '3px', modified: false }
      ];
      const expectedLabelProperties: ThemePropertyValue[] = [
        { elementSelector: 'vaadin-button::part(label)', propertyName: 'color', value: 'white', modified: false },
        {
          elementSelector: 'vaadin-button::part(label)',
          propertyName: 'font-size',
          value: '20px',
          modified: false
        }
      ];

      expect(theme.getPropertyValuesForElement('vaadin-button')).to.deep.equal(expectedHostProperties);
      expect(theme.getPropertyValuesForElement('vaadin-button::part(label)')).to.deep.equal(expectedLabelProperties);
      expect(theme.getPropertyValuesForElement('vaadin-button::part(unknown-part)')).to.deep.equal([]);
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
      theme1.updatePropertyValue('vaadin-button', 'background', 'cornflowerblue');
      theme1.updatePropertyValue('vaadin-button::part(label)', 'color', 'white');

      const theme2 = new ComponentTheme(buttonMetadata);
      theme2.updatePropertyValue('vaadin-button', 'padding', '3px');
      theme2.updatePropertyValue('vaadin-button::part(label)', 'color', 'red');
      theme2.updatePropertyValue('vaadin-button::part(label)', 'font-size', '20px');

      const result = ComponentTheme.combine(theme1, theme2);
      const expectedValues: ThemePropertyValue[] = [
        {
          elementSelector: 'vaadin-button',
          propertyName: 'background',
          value: 'cornflowerblue',
          modified: false
        },
        { elementSelector: 'vaadin-button::part(label)', propertyName: 'color', value: 'red', modified: false },
        { elementSelector: 'vaadin-button', propertyName: 'padding', value: '3px', modified: false },
        {
          elementSelector: 'vaadin-button::part(label)',
          propertyName: 'font-size',
          value: '20px',
          modified: false
        }
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
        elements: []
      };
      const buttonTheme = new ComponentTheme(buttonMetadata);
      const fooTheme = new ComponentTheme(fooMetadata);
      const result = ComponentTheme.combine(buttonTheme, fooTheme);

      expect(result.metadata).to.equal(buttonMetadata);
    });
  });

  describe('fromServerRules', () => {
    const globalScope: SelectorScope = {
      themeScope: ThemeScope.global
    };

    const localScope: SelectorScope = {
      themeScope: ThemeScope.local,
      localClassName: 'foo'
    };

    it('should create empty theme from empty rules', () => {
      const theme = ComponentTheme.fromServerRules(testElementMetadata, globalScope, []);

      expect(theme.properties.length).to.equal(0);
    });

    it('should create theme from global scoped rules', () => {
      const serverRules: ServerCssRule[] = [
        {
          selector: 'test-element',
          properties: {
            background: 'cornflowerblue',
            padding: '3px'
          }
        },
        {
          selector: 'test-element::part(label)',
          properties: {
            color: 'red',
            'font-size': '20px'
          }
        },
        {
          selector: 'test-element input[slot="input"]',
          properties: {
            'border-radius': '10px'
          }
        }
      ];
      const expectedProperties: ThemePropertyValue[] = [
        { elementSelector: 'test-element', propertyName: 'padding', value: '3px', modified: true },
        { elementSelector: 'test-element', propertyName: 'background', value: 'cornflowerblue', modified: true },
        { elementSelector: 'test-element::part(label)', propertyName: 'color', value: 'red', modified: true },
        {
          elementSelector: 'test-element::part(label)',
          propertyName: 'font-size',
          value: '20px',
          modified: true
        },
        {
          elementSelector: 'test-element input[slot="input"]',
          propertyName: 'border-radius',
          value: '10px',
          modified: true
        }
      ];

      const theme = ComponentTheme.fromServerRules(testElementMetadata, globalScope, serverRules);
      expect(theme.metadata).to.equal(testElementMetadata);
      expect(theme.properties).to.deep.equal(expectedProperties);
    });

    it('should create theme from local scoped rules', () => {
      const serverRules: ServerCssRule[] = [
        {
          selector: 'test-element.foo',
          properties: {
            background: 'cornflowerblue',
            padding: '3px'
          }
        },
        {
          selector: 'test-element.foo::part(label)',
          properties: {
            color: 'red',
            'font-size': '20px'
          }
        },
        {
          selector: 'test-element.foo input[slot="input"]',
          properties: {
            'border-radius': '10px'
          }
        }
      ];
      const expectedProperties: ThemePropertyValue[] = [
        { elementSelector: 'test-element', propertyName: 'padding', value: '3px', modified: true },
        { elementSelector: 'test-element', propertyName: 'background', value: 'cornflowerblue', modified: true },
        { elementSelector: 'test-element::part(label)', propertyName: 'color', value: 'red', modified: true },
        {
          elementSelector: 'test-element::part(label)',
          propertyName: 'font-size',
          value: '20px',
          modified: true
        },
        {
          elementSelector: 'test-element input[slot="input"]',
          propertyName: 'border-radius',
          value: '10px',
          modified: true
        }
      ];

      const theme = ComponentTheme.fromServerRules(testElementMetadata, localScope, serverRules);
      expect(theme.metadata).to.equal(testElementMetadata);
      expect(theme.properties).to.deep.equal(expectedProperties);
    });

    it('should ignore unknown selectors and properties', () => {
      const serverRules: ServerCssRule[] = [
        {
          selector: 'test-element.foo',
          properties: {
            foo: 'cornflowerblue'
          }
        },
        {
          selector: 'test-element.foo::part(label)',
          properties: {
            bar: '20px'
          }
        },
        {
          selector: 'test-element.foo::part(foo)',
          properties: {
            color: 'cornflowerblue',
            background: 'cornflowerblue'
          }
        }
      ];

      const theme = ComponentTheme.fromServerRules(testElementMetadata, localScope, serverRules);
      expect(theme.properties.length).to.equal(0);
    });
  });

  describe('generateThemeRule', () => {
    const globalScope: SelectorScope = {
      themeScope: ThemeScope.global
    };
    const localScope: SelectorScope = {
      themeScope: ThemeScope.local,
      localClassName: 'foo'
    };
    const hostElement = testElementMetadata.elements.find((element) => element.selector === 'test-element')!;
    const labelElement = testElementMetadata.elements.find(
      (element) => element.selector === 'test-element::part(label)'
    )!;
    const inputElement = testElementMetadata.elements.find(
      (element) => element.selector === 'test-element input[slot="input"]'
    )!;

    it('should generate rules for global scope', () => {
      const rules = [
        generateThemeRule(hostElement, globalScope, 'background', 'cornflowerblue'),
        generateThemeRule(hostElement, globalScope, 'padding', '3px'),
        generateThemeRule(labelElement, globalScope, 'color', 'white'),
        generateThemeRule(labelElement, globalScope, 'font-size', '20px'),
        generateThemeRule(inputElement, globalScope, 'border-radius', '10px')
      ];

      const expectedRules: ServerCssRule[] = [
        { selector: 'test-element', properties: { background: 'cornflowerblue' } },
        { selector: 'test-element', properties: { padding: '3px' } },
        { selector: 'test-element::part(label)', properties: { color: 'white' } },
        { selector: 'test-element::part(label)', properties: { 'font-size': '20px' } },
        { selector: 'test-element input[slot="input"]', properties: { 'border-radius': '10px' } }
      ];

      expect(rules).to.deep.equal(expectedRules);
    });

    it('should generate rules for local scope', () => {
      const rules = [
        generateThemeRule(hostElement, localScope, 'background', 'cornflowerblue'),
        generateThemeRule(hostElement, localScope, 'padding', '3px'),
        generateThemeRule(labelElement, localScope, 'color', 'white'),
        generateThemeRule(labelElement, localScope, 'font-size', '20px'),
        generateThemeRule(inputElement, localScope, 'border-radius', '10px')
      ];

      const expectedRules: ServerCssRule[] = [
        { selector: 'test-element.foo', properties: { background: 'cornflowerblue' } },
        { selector: 'test-element.foo', properties: { padding: '3px' } },
        { selector: 'test-element.foo::part(label)', properties: { color: 'white' } },
        { selector: 'test-element.foo::part(label)', properties: { 'font-size': '20px' } },
        { selector: 'test-element.foo input[slot="input"]', properties: { 'border-radius': '10px' } }
      ];

      expect(rules).to.deep.equal(expectedRules);
    });

    describe('individual property handling', () => {
      it('should update border-style when setting border-width', () => {
        const rules = [
          generateThemeRule(hostElement, globalScope, 'border-width', '0'),
          generateThemeRule(hostElement, globalScope, 'border-width', '0px'),
          generateThemeRule(hostElement, globalScope, 'border-width', '0rem'),
          generateThemeRule(hostElement, globalScope, 'border-width', '1px'),
          generateThemeRule(hostElement, globalScope, 'border-width', '1rem')
        ];

        const expectedRules: ServerCssRule[] = [
          { selector: 'test-element', properties: { 'border-width': '0', 'border-style': '' } },
          { selector: 'test-element', properties: { 'border-width': '0px', 'border-style': '' } },
          { selector: 'test-element', properties: { 'border-width': '0rem', 'border-style': '' } },
          { selector: 'test-element', properties: { 'border-width': '1px', 'border-style': 'solid' } },
          { selector: 'test-element', properties: { 'border-width': '1rem', 'border-style': 'solid' } }
        ];

        expect(rules).to.deep.equal(expectedRules);
      });
    });
  });

  describe('generateThemeRuleCss', () => {
    it('should generate CSS for theme rules', () => {
      const rules: ServerCssRule[] = [
        { selector: 'test-element.foo', properties: { background: 'cornflowerblue' } },
        { selector: 'test-element.foo::part(label)', properties: { color: 'white' } },
        {
          selector: 'test-element.foo input[slot="input"]',
          properties: { 'border-radius': '10px', 'border-style': 'solid' }
        }
      ];

      const expectedCss = [
        'test-element.foo { background: cornflowerblue; }',
        'test-element.foo::part(label) { color: white; }',
        'test-element.foo input[slot="input"] { border-radius: 10px; border-style: solid; }'
      ];

      const css = rules.map(generateThemeRuleCss);

      expect(css).to.deep.equal(expectedCss);
    });
  });
});
