export default interface NullableModel {
  bar: string;
  foo: string;
  listOfMapNullable?: Array<{ [key: string]: string; }>;
  listOfMapNullableNotNull?: Array<{ [key: string]: string; }>;
  nullableInteger?: number;
  shouldBeNotNullByDefault: number;
}