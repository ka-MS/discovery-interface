package com.itmsg.device42.integration.maximo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SourceTargetMapRepository {

    private final JdbcTemplate maximoJdbcTemplate;

    public SourceTargetMapRepository(
            @Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate
    ) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    public Long findTargetId(SourceTargetMapKey key) {
        List<Long> targetIds = maximoJdbcTemplate.query(
                FIND_TARGET_ID_QUERY,
                (resultSet, rowNumber) -> resultSet.getLong("target_id"),
                key.sourceSystem(),
                key.sourceObject(),
                key.sourceId(),
                key.targetSystem(),
                key.targetObject()
        );
        return targetIds.isEmpty() ? null : targetIds.getFirst();
    }

    public void insert(
            SourceTargetMapKey key,
            Long sourceParentId,
            Long targetId,
            Long targetParentId,
            LocalDateTime applyDateTime
    ) {
        int updatedRows = maximoJdbcTemplate.update(
                INSERT_QUERY,
                key.sourceSystem(),
                key.sourceObject(),
                key.sourceId(),
                sourceParentId,
                key.targetSystem(),
                key.targetObject(),
                targetId,
                targetParentId,
                Timestamp.valueOf(applyDateTime),
                Timestamp.valueOf(applyDateTime)
        );
        requireSingleRow(updatedRows, "INSERT", key);
    }

    public void updateParents(
            SourceTargetMapKey key,
            Long sourceParentId,
            Long targetParentId,
            LocalDateTime applyDateTime
    ) {
        int updatedRows = maximoJdbcTemplate.update(
                UPDATE_PARENTS_QUERY,
                sourceParentId,
                targetParentId,
                Timestamp.valueOf(applyDateTime),
                key.sourceSystem(),
                key.sourceObject(),
                key.sourceId(),
                key.targetSystem(),
                key.targetObject()
        );
        requireSingleRow(updatedRows, "UPDATE", key);
    }

    private static void requireSingleRow(
            int updatedRows,
            String operation,
            SourceTargetMapKey key
    ) {
        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "SOURCE_TARGET_MAP %s 결과가 1건이 아닙니다. key=%s, rows=%d"
                            .formatted(operation, key, updatedRows)
            );
        }
    }

    private static final String FIND_TARGET_ID_QUERY = """
            SELECT target_id
            FROM DISCOVERY.SOURCE_TARGET_MAP
            WHERE source_system = ?
              AND source_object = ?
              AND source_id = ?
              AND target_system = ?
              AND target_object = ?
            """;

    private static final String INSERT_QUERY = """
            INSERT INTO DISCOVERY.SOURCE_TARGET_MAP (
                SOURCE_SYSTEM,
                SOURCE_OBJECT,
                SOURCE_ID,
                SOURCE_PARENT_ID,
                TARGET_SYSTEM,
                TARGET_OBJECT,
                TARGET_ID,
                TARGET_PARENT_ID,
                CREATED_DATE,
                UPDATED_DATE
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_PARENTS_QUERY = """
            UPDATE DISCOVERY.SOURCE_TARGET_MAP
            SET SOURCE_PARENT_ID = ?,
                TARGET_PARENT_ID = ?,
                UPDATED_DATE = ?
            WHERE SOURCE_SYSTEM = ?
              AND SOURCE_OBJECT = ?
              AND SOURCE_ID = ?
              AND TARGET_SYSTEM = ?
              AND TARGET_OBJECT = ?
            """;
}
