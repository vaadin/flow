export enum ConnectionStatus {
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  UNAVAILABLE = 'unavailable',
  ERROR = 'error'
}

export abstract class Connection {
  static HEARTBEAT_INTERVAL = 180000;

  status: ConnectionStatus = ConnectionStatus.UNAVAILABLE;
  onHandshake() {
    // Intentionally empty
  }

  onConnectionError(_: string) {
    // Intentionally empty
  }

  onStatusChange(_: ConnectionStatus) {
    // Intentionally empty
  }

  setActive(yes: boolean) {
    if (!yes && this.status === ConnectionStatus.ACTIVE) {
      this.setStatus(ConnectionStatus.INACTIVE);
    } else if (yes && this.status === ConnectionStatus.INACTIVE) {
      this.setStatus(ConnectionStatus.ACTIVE);
    }
  }

  setStatus(status: ConnectionStatus) {
    if (this.status !== status) {
      this.status = status;
      this.onStatusChange(status);
    }
  }
}
