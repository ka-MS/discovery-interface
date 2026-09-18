package com.itmsg.device42.pipeline.d42maximo;

import com.itmsg.device42.source.device42.DoqlClient;
import com.itmsg.device42.source.device42.ci.relation.CiRelationQuery;
import com.itmsg.device42.pipeline.d42maximo.selection.MaximoSourcePolicy;
import com.itmsg.device42.source.device42.asset.computer.ComputerQuery;
import com.itmsg.device42.source.device42.asset.cpu.CpuQuery;
import com.itmsg.device42.source.device42.asset.device.DeviceQuery;
import com.itmsg.device42.source.device42.asset.disk.DiskQuery;
import com.itmsg.device42.source.device42.asset.logicaldrive.LogicalDriveQuery;
import com.itmsg.device42.source.device42.asset.mediaadapter.MediaAdapterQuery;
import com.itmsg.device42.source.device42.asset.netadapter.NetAdapterQuery;
import com.itmsg.device42.source.device42.asset.netdevice.NetDeviceQuery;
import com.itmsg.device42.source.device42.asset.netprinter.NetPrinterQuery;
import com.itmsg.device42.source.device42.asset.os.OsQuery;
import com.itmsg.device42.source.device42.asset.tcpip.TcpIpQuery;
import com.itmsg.device42.source.device42.ci.databaseinstance.DatabaseInstanceCiQuery;
import com.itmsg.device42.source.device42.ci.device.DeviceCiQuery;
import com.itmsg.device42.source.device42.ci.disk.DiskCiQuery;
import com.itmsg.device42.source.device42.ci.filesystem.FilesystemCiQuery;
import com.itmsg.device42.source.device42.ci.ip.IpCiQuery;
import com.itmsg.device42.source.device42.ci.os.OsCiQuery;
import com.itmsg.device42.source.device42.conversion.adapter.AdapterModelsQuery;
import com.itmsg.device42.source.device42.conversion.manufacturer.ManufacturerNamesQuery;
import com.itmsg.device42.source.device42.conversion.os.OperatingSystemNamesQuery;
import com.itmsg.device42.source.device42.conversion.processor.ProcessorModelsQuery;
import com.itmsg.device42.source.device42.software.catalog.SoftwareCatalogQuery;
import com.itmsg.device42.source.device42.software.installed.InstalledSoftwareQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Maximo 수집 범위로 D42 조회를 조립한다. 원천 Query에는 기본 정책이 없다. */
@Configuration(proxyBeanMethods = false)
public class MaximoQueries {

    @Bean
    CiRelationQuery ciRelationQuery(DoqlClient doql) {
        return new CiRelationQuery(MaximoSourcePolicy.RELATIONS, doql);
    }

    @Bean
    ManufacturerNamesQuery dpamManufacturerQuery(DoqlClient doql) {
        return new ManufacturerNamesQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.ASSET_DEVICE, MaximoSourcePolicy.UNKNOWN, doql);
    }

    @Bean
    ProcessorModelsQuery dpamProcessorQuery(DoqlClient doql) {
        return new ProcessorModelsQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    OperatingSystemNamesQuery dpamOsQuery(DoqlClient doql) {
        return new OperatingSystemNamesQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    AdapterModelsQuery dpamAdapterQuery(DoqlClient doql) {
        return new AdapterModelsQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.UNKNOWN, doql);
    }

    @Bean
    NetPrinterQuery netPrinterQuery(DoqlClient doql) {
        return new NetPrinterQuery(MaximoSourcePolicy.ASSET_PRINTER, doql);
    }

    @Bean
    MediaAdapterQuery mediaAdapterQuery(DoqlClient doql) {
        return new MediaAdapterQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    NetDeviceQuery netDeviceQuery(DoqlClient doql) {
        return new NetDeviceQuery(MaximoSourcePolicy.ASSET_NETWORK, doql);
    }

    @Bean
    DeviceQuery deployedAssetQuery(DoqlClient doql) {
        return new DeviceQuery(MaximoSourcePolicy.ASSET_DEVICE, doql);
    }

    @Bean
    DiskQuery diskQuery(DoqlClient doql) {
        return new DiskQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    ComputerQuery computerQuery(DoqlClient doql) {
        return new ComputerQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    LogicalDriveQuery logicalDriveQuery(DoqlClient doql) {
        return new LogicalDriveQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.ASSET_FILESYSTEM, doql);
    }

    @Bean
    CpuQuery cpuQuery(DoqlClient doql) {
        return new CpuQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    OsQuery osQuery(DoqlClient doql) {
        return new OsQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    TcpIpQuery tcpIpQuery(DoqlClient doql) {
        return new TcpIpQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    NetAdapterQuery netAdapterQuery(DoqlClient doql) {
        return new NetAdapterQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    DatabaseInstanceCiQuery databaseInstanceCiQuery(DoqlClient doql) {
        return new DatabaseInstanceCiQuery(doql);
    }

    @Bean
    DeviceCiQuery deviceCiQuery(DoqlClient doql) {
        return new DeviceCiQuery(MaximoSourcePolicy.CI_DEVICE, doql);
    }

    @Bean
    IpCiQuery ipCiQuery(DoqlClient doql) {
        return new IpCiQuery(doql);
    }

    @Bean
    DiskCiQuery diskCiQuery(DoqlClient doql) {
        return new DiskCiQuery(MaximoSourcePolicy.CI_COMPUTER, doql);
    }

    @Bean
    FilesystemCiQuery filesystemCiQuery(DoqlClient doql) {
        return new FilesystemCiQuery(MaximoSourcePolicy.CI_COMPUTER, MaximoSourcePolicy.CI_FILESYSTEM, doql);
    }

    @Bean
    OsCiQuery osCiQuery(DoqlClient doql) {
        return new OsCiQuery(MaximoSourcePolicy.CI_COMPUTER, doql);
    }

    @Bean
    SoftwareCatalogQuery tloamSoftwareQuery(DoqlClient doql) {
        return new SoftwareCatalogQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.UNKNOWN, doql);
    }

    @Bean
    InstalledSoftwareQuery dpaSoftwareQuery(DoqlClient doql) {
        return new InstalledSoftwareQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }
}
