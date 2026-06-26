import { expect } from '@open-wc/testing';
import { UpdatableModelProperties } from '../../main/frontend/internal/model/UpdatableModelProperties';

describe('UpdatableModelProperties', () => {
  it('reports the given properties as updatable and others as not', () => {
    const props = new UpdatableModelProperties(['name', 'age']);
    expect(props.isUpdatableProperty('name')).to.be.true;
    expect(props.isUpdatableProperty('age')).to.be.true;
    expect(props.isUpdatableProperty('other')).to.be.false;
  });

  it('treats an empty set as nothing updatable', () => {
    const props = new UpdatableModelProperties([]);
    expect(props.isUpdatableProperty('name')).to.be.false;
  });
});
