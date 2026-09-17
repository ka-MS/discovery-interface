package com.itmsg.device42.enums.ci;

/** CI 실행에서 사용할 분류. 추가한 항목은 다음 실행의 캐시 조회에 포함된다. */
public enum CiClassification {
    COMPUTER("SYS.COMPUTERSYSTEM"),
    VIRTUAL_COMPUTER("SYS.VIRTUALCOMPUTERSYSTEM"),
    GENERIC_SWITCH("SYS.GENERICSWITCH"),
    OPERATING_SYSTEM("SYS.OPERATINGSYSTEM"),
    DISK_DRIVE("DEV.DISKDRIVE"),
    FILE_SYSTEM("SYS.FILESYSTEM"),
    IP_ADDRESS("NET.IPADDRESS"),
    SQL_SERVER("APP.DB.MSSQL.SQLSERVER"),
    DB2_INSTANCE("APP.DB.DB2.DB2INSTANCE"),
    ORACLE_INSTANCE("APP.DB.ORACLE.ORACLEINSTANCE"),
    DATABASE_SERVER("APP.DB.DATABASESERVER");

    private final String classificationId;

    CiClassification(String classificationId) {
        this.classificationId = classificationId;
    }

    public String classificationId() {
        return classificationId;
    }
}
