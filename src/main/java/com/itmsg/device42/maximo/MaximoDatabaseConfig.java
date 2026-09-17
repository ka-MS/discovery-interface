package com.itmsg.device42.maximo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class MaximoDatabaseConfig {

    @Bean("maximoJdbcTemplate")
    public JdbcTemplate maximoJdbcTemplate(
            @Qualifier("dataSource") DataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }
}
