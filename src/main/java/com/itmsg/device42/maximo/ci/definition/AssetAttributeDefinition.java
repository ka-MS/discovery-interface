package com.itmsg.device42.maximo.ci.definition;

public record AssetAttributeDefinition(
        long id, String attributeId, String dataType, String measureUnitId,
        String orgId, String siteId
) {
}
