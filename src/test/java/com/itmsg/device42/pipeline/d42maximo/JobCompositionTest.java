package com.itmsg.device42.pipeline.d42maximo;

import com.itmsg.device42.pipeline.d42maximo.asset.AssetIntegrationJob;
import com.itmsg.device42.pipeline.d42maximo.ci.CiIntegrationJob;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiClassification;
import com.itmsg.device42.pipeline.d42maximo.conversion.ConversionIntegrationJob;
import com.itmsg.device42.pipeline.d42maximo.software.SoftwareIntegrationJob;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionCache;
import com.itmsg.device42.runtime.IntegrationJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JobCompositionTest {
    static Stream<Arguments> jobs() {
        return Stream.of(
                Arguments.of(AssetIntegrationJob.class, List.of("DeployedAssetImport", "ComputerImport",
                        "NetDeviceImport", "NetPrinterImport", "CpuImport", "DiskImport", "NetAdapterImport",
                        "MediaAdapterImport", "TcpIpImport", "LogicalDriveImport", "OsImport")),
                Arguments.of(ConversionIntegrationJob.class, List.of("DpamManufacturerImport", "DpamManuVariantImport",
                        "DpamOsImport", "DpamOsVariantImport", "DpamProcessorImport", "DpamProcVariantImport",
                        "DpamAdapterImport", "DpamAdptVariantImport")),
                Arguments.of(SoftwareIntegrationJob.class, List.of("TloamSoftwareImport", "DpaSoftwareImport")),
                Arguments.of(CiIntegrationJob.class, List.of("CiDefinitionLoader", "DatabaseInstanceCiImport",
                        "DeviceCiImport", "DiskCiImport", "FilesystemCiImport", "IpCiImport", "OsCiImport", "CiRelationJob")));
    }

    @ParameterizedTest
    @MethodSource("jobs")
    void explicitlyComposesEveryExistingStepInBaselineOrder(Class<? extends IntegrationJob> type,
                                                          List<String> expected) throws Exception {
        var calls = new ArrayList<String>();
        var snapshot = new CiDefinitionCache(Map.of(), Map.of(), Set.of(), Map.of());
        var constructor = type.getConstructors()[0];
        var dependencies = Stream.of(constructor.getParameterTypes()).map(dependency -> mock(dependency, invocation -> {
            String method = invocation.getMethod().getName();
            if (Set.of("load", "integrate", "run").contains(method)) {
                calls.add(dependency.getSimpleName());
                if (method.equals("load")) {
                    Object actualIds = invocation.getArgument(0);
                    assertThat(actualIds).isEqualTo(CiClassification.ids());
                    return snapshot;
                }
                if (invocation.getArguments().length == 1) {
                    assertThat(invocation.getArgument(0, CiDefinitionCache.class)).isSameAs(snapshot);
                }
            }
            return null;
        })).toArray();
        var job = (IntegrationJob) constructor.newInstance(dependencies);
        job.run();
        assertThat(calls).containsExactlyElementsOf(expected);
        calls.clear();
        job.run();
        assertThat(calls).containsExactlyElementsOf(expected);
    }
}
