import { expect } from '@open-wc/testing';
import {
  updateAttribute,
  updateAttributeValue
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

function fakeConfiguration(webComponentMode: boolean, serviceUrl = '') {
  return {
    isWebComponentMode: () => webComponentMode,
    getServiceUrl: () => serviceUrl
  };
}

describe('SimpleElementBindingStrategy attribute binding', () => {
  it('updateAttributeValue sets a string value and removes it on null', () => {
    const element = document.createElement('div');
    updateAttributeValue(fakeConfiguration(false), element, 'title', 'hi');
    expect(element.getAttribute('title')).to.equal('hi');
    updateAttributeValue(fakeConfiguration(false), element, 'title', null);
    expect(element.hasAttribute('title')).to.be.false;
  });

  it('updateAttributeValue stringifies non-string, non-object values', () => {
    const element = document.createElement('div');
    updateAttributeValue(fakeConfiguration(false), element, 'tabindex', 3);
    expect(element.getAttribute('tabindex')).to.equal('3');
  });

  it('updateAttributeValue resolves a uri object as-is when not in web-component mode', () => {
    const element = document.createElement('img');
    updateAttributeValue(fakeConfiguration(false), element, 'src', { uri: 'pic.png' });
    expect(element.getAttribute('src')).to.equal('pic.png');
  });

  it('updateAttributeValue prefixes the service url for a relative uri in web-component mode', () => {
    const element = document.createElement('img');
    updateAttributeValue(fakeConfiguration(true, 'http://host/app'), element, 'src', { uri: 'pic.png' });
    expect(element.getAttribute('src')).to.equal('http://host/app/pic.png');
  });

  it('updateAttributeValue leaves an absolute uri untouched in web-component mode', () => {
    const element = document.createElement('img');
    updateAttributeValue(fakeConfiguration(true, 'http://host/app/'), element, 'src', {
      uri: 'http://cdn/pic.png'
    });
    expect(element.getAttribute('src')).to.equal('http://cdn/pic.png');
  });

  it('updateAttribute resolves the configuration from the property node and applies the value', () => {
    const element = document.createElement('div');
    const property = {
      getName: () => 'data-x',
      getValue: () => 'v',
      getMap: () => ({
        getNode: () => ({
          getTree: () => ({
            getRegistry: () => ({ getApplicationConfiguration: () => fakeConfiguration(false) })
          })
        })
      })
    };
    updateAttribute(property, element);
    expect(element.getAttribute('data-x')).to.equal('v');
  });
});
