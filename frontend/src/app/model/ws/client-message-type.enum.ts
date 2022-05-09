export namespace ClientMessageType {
  export type ClientMessageTypeEnum = 'ACK' | 'REGISTER' | 'UNREGISTER' | 'PING';
  export const ClientMessageTypeEnum = {
    ACK: 'ACK' as ClientMessageTypeEnum,
    REGISTER: 'REGISTER' as ClientMessageTypeEnum,
    UNREGISTER: 'UNREGISTER' as ClientMessageTypeEnum,
    PING: 'PING' as ClientMessageTypeEnum,
  };
  export const ClientMessageTypes: ClientMessageTypeEnum[] = ['ACK', 'REGISTER', 'UNREGISTER', 'PING'];
}
