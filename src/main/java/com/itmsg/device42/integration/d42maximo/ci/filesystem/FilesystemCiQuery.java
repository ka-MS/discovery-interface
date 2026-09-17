package com.itmsg.device42.integration.d42maximo.ci.filesystem;

import com.itmsg.device42.device42.DoqlClient;
import com.itmsg.device42.integration.d42maximo.ci.selection.CiSourceFilter;
import com.itmsg.device42.integration.d42maximo.ci.selection.FilesystemSelection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FilesystemCiQuery {

    private static final Logger log = LoggerFactory.getLogger(FilesystemCiQuery.class);

    private final DoqlClient doql;

    public FilesystemCiQuery(DoqlClient doql) {
        this.doql = doql;
    }

    public long getTotalCount() {
        try {
            return doql.query(TOTAL_COUNT_QUERY, rs -> {
                return rs.next() ? rs.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Filesystem 건수 조회에 실패했습니다.", e);
        }
    }

    public List<FilesystemSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try {
            return doql.query(query, rs -> {
                List<FilesystemSource> data = new ArrayList<>(limit);
                while (rs.next()) {
                    long mountPointPk = rs.getLong("mountpoint_pk");
                    try {
                        data.add(new FilesystemSource(
                                mountPointPk, rs.getLong("device_fk"), rs.getString("mountpoint"),
                                rs.getString("fstype_name"), rs.getString("label"), rs.getBigDecimal("capacity"),
                                rs.getBigDecimal("free_capacity"), rs.getString("last_discovered")));
                    } catch (SQLException e) {
                        log.error("Filesystem 원천 변환에 실패했습니다. mountPointPk={}", mountPointPk, e);
                    }
                }
                return data;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Filesystem 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_mountpoint_v2 m
            WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + FilesystemSelection.EXCLUDED_TYPES_SQL + """
            ))
            AND EXISTS (
                SELECT 1 FROM view_device_v2 d
                WHERE d.device_pk = ANY(m.device_fks) AND
            """ + CiSourceFilter.COMPUTER + """
            )
            """;

    private static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.device_pk, d.last_discovered
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.COMPUTER + """
            )
            SELECT DISTINCT ON (m.mountpoint_pk)
                m.mountpoint_pk, c.device_pk AS device_fk,
                NULLIF(TRIM(m.mountpoint), '') AS mountpoint,
                NULLIF(TRIM(m.fstype_name), '') AS fstype_name,
                NULLIF(TRIM(m.label), '') AS label,
                m.capacity, m.free_capacity, c.last_discovered
            FROM view_mountpoint_v2 m
            JOIN computer c ON c.device_pk = ANY(m.device_fks)
            WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + FilesystemSelection.EXCLUDED_TYPES_SQL + """
            ))
            ORDER BY m.mountpoint_pk, c.device_pk
            LIMIT %d OFFSET %d
            """;
}
