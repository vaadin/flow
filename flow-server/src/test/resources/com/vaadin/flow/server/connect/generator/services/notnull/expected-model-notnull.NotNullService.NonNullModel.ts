export default interface NonNullModel {
  bar: string;
  foo?: string | null;
  listOfMapNullable?: Array<{ [key: string]: string | null; } | null> | null;
  listOfMapNullableNotNull: Array<{ [key: string]: string | null; } | null>;
  nullableInteger?: number | null;
  shouldBeNotNullByDefault: number;
}
