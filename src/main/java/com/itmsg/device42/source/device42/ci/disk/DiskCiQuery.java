package com.itmsg.device42.source.device42.ci.disk;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskCiQuery {

    private static final Logger log = LoggerFactory.getLogger(DiskCiQuery.class);

    private final DoqlClient doql;
    private final DeviceSelection selection;

    public DiskCiQuery(DeviceSelection selection, DoqlClient doql) {
        this.doql = doql;
        this.selection = selection;
    }

    public long getTotalCount() {
        try {
            return doql.query(sql(TOTAL_COUNT_QUERY), rs -> {
                return rs.next() ? rs.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Disk 건수 조회에 실패했습니다.", e);
        }
    }

    public List<DiskSource> getData(long offset, int limit) {
        String query = sql(SOURCE_QUERY).formatted(limit, offset);
        try {
            return doql.query(query, rs -> {
                List<DiskSource> data = new ArrayList<>(limit);
                while (rs.next()) {
                    long partPk = rs.getLong("part_pk");
                    try {
                        data.add(new DiskSource(
                                partPk, rs.getLong("device_fk"), rs.getString("model"),
                                rs.getString("serial_no"), rs.getString("description"),
                                rs.getBigDecimal("hdsize"), rs.getString("hdsize_unit"),
                                rs.getString("last_discovered")));
                    } catch (SQLException e) {
                        log.error("Disk 원천 변환에 실패했습니다. partPk={}", partPk, e);
                    }
                }
                return data;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Disk 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql());
    }

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            WHERE pm.type_name = 'Hard Disk' AND
            """ +  "{{DEVICE_FILTER}}";

    private static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.device_pk, d.last_discovered
                FROM view_device_v2 d
                WHERE
            """ +  "{{DEVICE_FILTER}}" + """
            )
            SELECT p.part_pk, p.device_fk,
                NULLIF(TRIM(pm.name), '') AS model,
                NULLIF(TRIM(p.serial_no), '') AS serial_no,
                NULLIF(TRIM(p.description), '') AS description,
                pm.hdsize, NULLIF(TRIM(pm.hdsize_unit), '') AS hdsize_unit,
                c.last_discovered
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN computer c ON c.device_pk = p.device_fk
            WHERE pm.type_name = 'Hard Disk'
            ORDER BY p.part_pk
            LIMIT %d OFFSET %d
            """;
}
