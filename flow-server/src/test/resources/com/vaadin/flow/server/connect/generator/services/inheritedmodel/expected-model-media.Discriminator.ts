export default interface Discriminator {
  mapping?: { [key: string]: string | null; } | null;
  propertyName?: string | null;
}
