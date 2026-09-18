package com.itmsg.device42.pipeline.d42maximo;

import com.itmsg.device42.source.device42.DoqlClient;
import com.itmsg.device42.source.device42.ci.relation.CiRelationQuery;
import com.itmsg.device42.pipeline.d42maximo.selection.MaximoSourcePolicy;
import com.itmsg.device42.source.device42.asset.computer.ComputerQuery;
import com.itmsg.device42.source.device42.asset.cpu.CpuQuery;
import com.itmsg.device42.source.device42.asset.device.DeployedAssetQuery;
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
import com.itmsg.device42.source.device42.conversion.adapter.DpamAdapterQuery;
import com.itmsg.device42.source.device42.conversion.adapter.DpamAdptVariantQuery;
import com.itmsg.device42.source.device42.conversion.manufacturer.DpamManuVariantQuery;
import com.itmsg.device42.source.device42.conversion.manufacturer.DpamManufacturerQuery;
import com.itmsg.device42.source.device42.conversion.os.DpamOsQuery;
import com.itmsg.device42.source.device42.conversion.os.DpamOsVariantQuery;
import com.itmsg.device42.source.device42.conversion.processor.DpamProcVariantQuery;
import com.itmsg.device42.source.device42.conversion.processor.DpamProcessorQuery;
import com.itmsg.device42.source.device42.software.catalog.TloamSoftwareQuery;
import com.itmsg.device42.source.device42.software.installed.DpaSoftwareQuery;
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
    DpamManufacturerQuery dpamManufacturerQuery(DoqlClient doql) {
        return new DpamManufacturerQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.ASSET_DEVICE, MaximoSourcePolicy.UNKNOWN, doql);
    }

    @Bean
    DpamManuVariantQuery dpamManuVariantQuery(DoqlClient doql) {
        return new DpamManuVariantQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.ASSET_DEVICE, MaximoSourcePolicy.UNKNOWN, doql);
    }

    @Bean
    DpamProcessorQuery dpamProcessorQuery(DoqlClient doql) {
        return new DpamProcessorQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    DpamProcVariantQuery dpamProcVariantQuery(DoqlClient doql) {
        return new DpamProcVariantQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    DpamOsQuery dpamOsQuery(DoqlClient doql) {
        return new DpamOsQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    DpamOsVariantQuery dpamOsVariantQuery(DoqlClient doql) {
        return new DpamOsVariantQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }

    @Bean
    DpamAdapterQuery dpamAdapterQuery(DoqlClient doql) {
        return new DpamAdapterQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.UNKNOWN, doql);
    }

    @Bean
    DpamAdptVariantQuery dpamAdptVariantQuery(DoqlClient doql) {
        return new DpamAdptVariantQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.UNKNOWN, doql);
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
    DeployedAssetQuery deployedAssetQuery(DoqlClient doql) {
        return new DeployedAssetQuery(MaximoSourcePolicy.ASSET_DEVICE, doql);
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
    TloamSoftwareQuery tloamSoftwareQuery(DoqlClient doql) {
        return new TloamSoftwareQuery(MaximoSourcePolicy.ASSET_COMPUTER, MaximoSourcePolicy.UNKNOWN, doql);
    }

    @Bean
    DpaSoftwareQuery dpaSoftwareQuery(DoqlClient doql) {
        return new DpaSoftwareQuery(MaximoSourcePolicy.ASSET_COMPUTER, doql);
    }
}
