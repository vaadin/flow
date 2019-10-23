export default interface Version {
  artifactId?: string | null;
  groupId?: string | null;
  majorVersion: number;
  minorVersion: number;
  patchLevel: number;
  snapshotInfo?: string | null;
}
