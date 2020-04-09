export namespace ClientMessageType {
    export type ClientMessageTypeEnum = 'ACK' | 'REGISTER' | 'UNREGISTER';
    export const ClientMessageTypeEnum = {
        COMPETITION_START: 'COMPETITION_START' as ClientMessageTypeEnum,
        COMPETITION_UPDATE: 'COMPETITION_UPDATE' as ClientMessageTypeEnum,
        COMPETITION_END: 'COMPETITION_END' as ClientMessageTypeEnum,
        TASK_PREPARE: 'TASK_PREPARE' as ClientMessageTypeEnum,
        TASK_START: 'TASK_START' as ClientMessageTypeEnum,
        TASK_UPDATED: 'TASK_UPDATED' as ClientMessageTypeEnum,
        TASK_END: 'TASK_END' as ClientMessageTypeEnum
    };
    export const ClientMessageTypes: ClientMessageTypeEnum[] = ['ACK', 'REGISTER', 'UNREGISTER'];
}
