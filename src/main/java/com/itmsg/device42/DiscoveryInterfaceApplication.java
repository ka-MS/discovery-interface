package com.itmsg.device42;

import org.springframework.boot.SpringApplication;
import com.itmsg.device42.cli.JobRunner;
import com.itmsg.device42.runtime.TargetModule;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@Import(JobRunner.class)
@ComponentScan(basePackages = "com.itmsg.device42.pipeline", useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TargetModule.class))
public class DiscoveryInterfaceApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(DiscoveryInterfaceApplication.class);
        springApplication.run(args);
    }
}
