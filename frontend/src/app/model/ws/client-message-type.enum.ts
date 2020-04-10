export namespace ClientMessageType {
    export type ClientMessageTypeEnum = 'ACK' | 'REGISTER' | 'UNREGISTER';
    export const ClientMessageTypeEnum = {
        ACK: 'ACK' as ClientMessageTypeEnum,
        REGISTER: 'REGISTER' as ClientMessageTypeEnum,
        UNREGISTER: 'UNREGISTER' as ClientMessageTypeEnum,
    };
    export const ClientMessageTypes: ClientMessageTypeEnum[] = ['ACK', 'REGISTER', 'UNREGISTER'];
}
