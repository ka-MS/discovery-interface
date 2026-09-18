package com.itmsg.device42.pipeline.d42maximo.selection;

import com.itmsg.device42.source.device42.selection.DeviceSelection;
import com.itmsg.device42.source.device42.selection.FilesystemFilter;
import com.itmsg.device42.source.device42.ci.relation.Device42Relation;
import java.util.List;

/** 기존 연계 수집 범위. D42 조회는 이 선택을 주입받으며 Maximo를 역참조하지 않는다. */
public final class MaximoSourcePolicy {
    private MaximoSourcePolicy() {}

    public static final DeviceSelection ASSET_DEVICE = DeviceSelection.discovered(
            List.of("virtual", "physical"), 15, null, List.of("PDU"));
    public static final DeviceSelection ASSET_COMPUTER = DeviceSelection.discovered(
            List.of("virtual", "physical"), 15, false, List.of("Network Printer", "PDU"));
    public static final DeviceSelection ASSET_NETWORK = DeviceSelection.discovered(
            List.of("physical"), null, true, List.of());
    public static final DeviceSelection ASSET_PRINTER = DeviceSelection.discovered(
            List.of("virtual", "physical"), 15, false, List.of()).physicalSubtype("Network Printer");
    public static final DeviceSelection CI_COMPUTER = DeviceSelection.compute(
            List.of("Generic", "Rackable", "Blade", "WorkStation", "ThinClient", "Laptop"),
            List.of("Internal VM", "Amazon EC2 Instance", "VMWare", "Hyper-V"));
    public static final DeviceSelection CI_DEVICE = CI_COMPUTER.includingNetwork("Switch");
    public static final FilesystemFilter CI_FILESYSTEM = new FilesystemFilter(
            List.of("overlay", "squashfs", "efivarfs"));
    public static final FilesystemFilter ASSET_FILESYSTEM = new FilesystemFilter(
            List.of("overlay", "devtmpfs", "efivarfs"));
    public static final String UNKNOWN = "UNKNOWN";
    public static final Device42Relation.Selection RELATIONS = new Device42Relation.Selection(
            CI_COMPUTER, CI_DEVICE, CI_FILESYSTEM, "Switch", "Network Printer");
}
