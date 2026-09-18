package com.itmsg.device42.pipeline.d42maximo.asset;

import com.itmsg.device42.pipeline.d42maximo.asset.computer.ComputerImport;
import com.itmsg.device42.pipeline.d42maximo.asset.cpu.CpuImport;
import com.itmsg.device42.pipeline.d42maximo.asset.device.DeployedAssetImport;
import com.itmsg.device42.pipeline.d42maximo.asset.disk.DiskImport;
import com.itmsg.device42.pipeline.d42maximo.asset.logicaldrive.LogicalDriveImport;
import com.itmsg.device42.pipeline.d42maximo.asset.mediaadapter.MediaAdapterImport;
import com.itmsg.device42.pipeline.d42maximo.asset.netadapter.NetAdapterImport;
import com.itmsg.device42.pipeline.d42maximo.asset.netdevice.NetDeviceImport;
import com.itmsg.device42.pipeline.d42maximo.asset.netprinter.NetPrinterImport;
import com.itmsg.device42.pipeline.d42maximo.asset.os.OsImport;
import com.itmsg.device42.pipeline.d42maximo.asset.tcpip.TcpIpImport;
import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.NamedTask;
import com.itmsg.device42.runtime.TaskSequence;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("asset")
public class AssetIntegrationJob implements IntegrationJob {
    private static final Logger log = LoggerFactory.getLogger(AssetIntegrationJob.class);

    private final List<NamedTask> tasks;

    public AssetIntegrationJob(
            DeployedAssetImport deployedAssetImport,
            ComputerImport computerImport,
            NetDeviceImport netDeviceImport,
            NetPrinterImport netPrinterImport,
            CpuImport cpuImport,
            DiskImport diskImport,
            NetAdapterImport netAdapterImport,
            MediaAdapterImport mediaAdapterImport,
            TcpIpImport tcpIpImport,
            LogicalDriveImport logicalDriveImport,
            OsImport osImport) {
        this.tasks = List.of(
                new NamedTask("DeployedAssetIntegrate", deployedAssetImport::integrate),
                new NamedTask("DpaComputerIntegrate", computerImport::integrate),
                new NamedTask("DpaNetDeviceIntegrate", netDeviceImport::integrate),
                new NamedTask("DpaNetPrinterIntegrate", netPrinterImport::integrate),
                new NamedTask("DpaCpuIntegrate", cpuImport::integrate),
                new NamedTask("DpaDiskIntegrate", diskImport::integrate),
                new NamedTask("DpaNetAdapterIntegrate", netAdapterImport::integrate),
                new NamedTask("DpaMediaAdapterIntegrate", mediaAdapterImport::integrate),
                new NamedTask("DpaTcpIpIntegrate", tcpIpImport::integrate),
                new NamedTask("DpaLogicalDriveIntegrate", logicalDriveImport::integrate),
                new NamedTask("DpaOsIntegrate", osImport::integrate)
        );
    }

    @Override
    public void run() {
        TaskSequence.run(log, tasks);
    }
}
