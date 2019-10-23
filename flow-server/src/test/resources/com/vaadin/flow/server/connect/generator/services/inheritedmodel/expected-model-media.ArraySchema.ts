import Schema from './Schema';

export default interface ArraySchema extends Schema {
  items?: Schema | null;
  type?: string | null;
}
