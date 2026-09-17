package com.itmsg.device42.enums.ci;

/**
 * DB Instance 네 분류에서 수집할 ASSETATTRID.
 * 대조 기준은 승격 대상 CI 분류 네 개(CI.SQLSERVER·CI.DB2INSTANCE·CI.ORACLEINSTANCE·
 * CI.DATABASESERVER)의 공통 속성 8개다. 그중 D42에 원천이 있는 5개만 둔다.
 * VENDORNAME·EXECUTABLENAME·STATUS는 원천이 없어 제외했다.
 */
public enum DatabaseInstanceSpec implements CiSpec {
    NAME("APPSERVER_NAME"),
    PRODUCT_NAME("APPSERVER_PRODUCTNAME"),
    PRODUCT_VERSION("APPSERVER_PRODUCTVERSION"),
    KEY_NAME("APPSERVER_KEYNAME"),
    HOME("DATABASESERVER_HOME");

    private final String attributeId;

    DatabaseInstanceSpec(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public String attributeId() {
        return attributeId;
    }
}
