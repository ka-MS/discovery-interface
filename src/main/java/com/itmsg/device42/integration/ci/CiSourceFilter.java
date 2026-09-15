package com.itmsg.device42.integration.ci;

/** 모든 CI 유형이 같은 Computer 집합을 부모로 삼도록 조건을 한 곳에 둔다. 별칭은 d다. */
public final class CiSourceFilter {
    private CiSourceFilter() {
    }

    public static final String COMPUTER = """
            d.type IN ('physical', 'virtual')
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                (d.type = 'physical' AND d.physicalsubtype IN
                    ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
                OR
                (d.type = 'virtual' AND d.virtualsubtype IN
                    ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
            )
            """;
}
