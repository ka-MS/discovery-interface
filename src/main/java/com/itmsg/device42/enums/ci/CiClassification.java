package com.itmsg.device42.enums.ci;

/** CI 실행에서 사용할 분류. 추가한 항목은 다음 실행의 캐시 조회에 포함된다. */
public enum CiClassification {
    COMPUTER("SYS.COMPUTERSYSTEM"),
    VIRTUAL_COMPUTER("SYS.VIRTUALCOMPUTERSYSTEM"),
    OPERATING_SYSTEM("SYS.OPERATINGSYSTEM");

    private final String classificationId;

    CiClassification(String classificationId) {
        this.classificationId = classificationId;
    }

    public String classificationId() {
        return classificationId;
    }
}
