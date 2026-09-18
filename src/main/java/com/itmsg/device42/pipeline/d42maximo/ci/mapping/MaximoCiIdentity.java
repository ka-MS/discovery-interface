package com.itmsg.device42.pipeline.d42maximo.ci.mapping;

import com.itmsg.device42.source.device42.ci.relation.Device42Entity;

/** CI 본체와 관계 끝점이 반드시 공유하는 D42 → Maximo 식별자 규칙. */
public final class MaximoCiIdentity {
    private MaximoCiIdentity() {}

    public static String of(Device42Entity entity, long pk) {
        return of(entity, Long.toString(pk));
    }

    public static String of(Device42Entity entity, String pk) {
        // SQL의 문자열 연결은 NULL PK를 NULL로 보존했다.
        if (pk == null) return null;
        String prefix = switch (entity) {
            case DEVICE -> "D42:DEVICE:";
            case DEVICEOS -> "D42:DEVICEOS:";
            case PART -> "D42:PART:";
            case MOUNTPOINT -> "D42:MOUNTPOINT:";
            case DATABASEINSTANCE -> "D42:DATABASEINSTANCE:";
            case IPADDRESS -> "D42:IPADDRESS:";
        };
        return prefix + pk;
    }
}
