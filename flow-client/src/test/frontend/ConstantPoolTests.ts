import { expect } from '@open-wc/testing';
import { ConstantPool } from '../../main/frontend/internal/ConstantPool';

describe('ConstantPool', () => {
  it('imports constants from JSON and reads them back', () => {
    const pool = new ConstantPool();
    expect(pool.has('a')).to.equal(false);

    pool.importFromJson({ a: 'value-a', b: 42 });

    expect(pool.has('a')).to.equal(true);
    expect(pool.has('b')).to.equal(true);
    expect(pool.get<string>('a')).to.equal('value-a');
    expect(pool.get<number>('b')).to.equal(42);
  });

  it('reports unknown keys as absent', () => {
    const pool = new ConstantPool();
    pool.importFromJson({ a: 'value-a' });
    expect(pool.has('missing')).to.equal(false);
    expect(pool.get<string>('missing')).to.equal(undefined);
  });

  it('accumulates constants across imports', () => {
    const pool = new ConstantPool();
    pool.importFromJson({ a: '1' });
    pool.importFromJson({ b: '2' });
    expect(pool.get<string>('a')).to.equal('1');
    expect(pool.get<string>('b')).to.equal('2');
  });
});
