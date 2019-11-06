export default interface Version {
  artifactId: string;
  groupId: string;
  majorVersion: number;
  minorVersion: number;
  patchLevel: number;
  snapshotInfo: string;
}
