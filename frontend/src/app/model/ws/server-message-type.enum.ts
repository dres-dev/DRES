// tslint:disable-next-line:no-namespace
export namespace ServerMessageType {
    export type ServerMessageTypeEnum = 'COMPETITION_START' | 'COMPETITION_UPDATE' | 'COMPETITION_END' | 'TASK_PREPARE' | 'TASK_START' | 'TASK_UPDATED' | 'TASK_END';
    export const ServerMessageTypeEnum = {
        COMPETITION_START: 'COMPETITION_START' as ServerMessageTypeEnum,
        COMPETITION_UPDATE: 'COMPETITION_UPDATE' as ServerMessageTypeEnum,
        COMPETITION_END: 'COMPETITION_END' as ServerMessageTypeEnum,
        TASK_PREPARE: 'TASK_PREPARE' as ServerMessageTypeEnum,
        TASK_START: 'TASK_START' as ServerMessageTypeEnum,
        TASK_UPDATED: 'TASK_UPDATED' as ServerMessageTypeEnum,
        TASK_END: 'TASK_END' as ServerMessageTypeEnum
    };
    export const ServerMessageTypes: ServerMessageTypeEnum[] = [
        'COMPETITION_START',
        'COMPETITION_UPDATE',
        'COMPETITION_END',
        'TASK_PREPARE',
        'TASK_START',
        'TASK_UPDATED',
        'TASK_END'
    ];
}
