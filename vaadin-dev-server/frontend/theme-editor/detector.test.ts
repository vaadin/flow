import { expect, fixture, html } from '@open-wc/testing';
import sinon from 'sinon';
import { detectTheme } from './detector';
import { testElementMetadata } from './tests/utils';

describe('theme-detector', () => {
  let setupElementStub: sinon.SinonStub;
  let cleanupElementStub: sinon.SinonStub;

  beforeEach(() => {
    setupElementStub = sinon.stub(testElementMetadata, 'setupElement');
    cleanupElementStub = sinon.stub(testElementMetadata, 'cleanupElement');
  });

  afterEach(() => {
    setupElementStub.restore();
    cleanupElementStub.restore();
  });

  it('should include all CSS property values from component metadata', async () => {
    const theme = await detectTheme(testElementMetadata);
    let propertyCount = 0;

    testElementMetadata.elements.forEach((element) => {
      element.properties.forEach((property) => {
        propertyCount++;
        const propertyValue = theme.getPropertyValue(element.selector, property.propertyName);
        expect(propertyValue).to.exist;
      });
    });

    expect(theme.properties.length).to.equal(propertyCount);
  });

  it('should detect default CSS property values', async () => {
    const theme = await detectTheme(testElementMetadata);

    expect(theme.getPropertyValue('test-element', 'padding').value).to.equal('10px');
    expect(theme.getPropertyValue('test-element::part(label)', 'color').value).to.equal('rgb(0, 0, 0)');
    expect(theme.getPropertyValue('test-element input[slot="input"]', 'border-radius').value).to.equal('5px');
    expect(theme.getPropertyValue('test-element::part(helper-text)', 'color').value).to.equal('rgb(200, 200, 200)');
  });

  it('should use default value from metadata if property is not defined in computed styles', async () => {
    const theme = await detectTheme(testElementMetadata);

    expect(theme.getPropertyValue('test-element', '--custom-property').value).to.equal('foo');
  });

  it('should detect themed CSS property values', async () => {
    await fixture(html`
      <style>
        test-element {
          padding: 20px;
          --custom-property: bar;
        }

        test-element::part(label) {
          color: green;
        }

        test-element input[slot='input'] {
          border-radius: 10px;
        }

        test-element::part(helper-text) {
          color: blue;
        }
      </style>
    `);
    const theme = await detectTheme(testElementMetadata);

    expect(theme.getPropertyValue('test-element', 'padding').value).to.equal('20px');
    expect(theme.getPropertyValue('test-element', '--custom-property').value.trim()).to.equal('bar');
    expect(theme.getPropertyValue('test-element::part(label)', 'color').value).to.equal('rgb(0, 128, 0)');
    expect(theme.getPropertyValue('test-element input[slot="input"]', 'border-radius').value).to.equal('10px');
    expect(theme.getPropertyValue('test-element::part(helper-text)', 'color').value).to.equal('rgb(0, 0, 255)');
  });

  it('should remove test component from DOM', async () => {
    await detectTheme(testElementMetadata);

    expect(document.querySelector('test-element')).to.not.exist;
  });

  it('should call custom setup and cleanup functions', async () => {
    await detectTheme(testElementMetadata);

    expect(setupElementStub.calledOnce).to.be.true;
    expect(cleanupElementStub.calledOnce).to.be.true;
  });
});
