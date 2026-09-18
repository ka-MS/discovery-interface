package com.itmsg.device42.target.maximo.ci.definition;

public record AssetAttributeDefinition(
        long id, String attributeId, String dataType, String measureUnitId,
        String orgId, String siteId
) {
}
