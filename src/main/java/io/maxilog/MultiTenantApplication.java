package io.maxilog;

import io.maxilog.config.ApplicationProperties;
import io.maxilog.config.tenant.TenantDataSourceRouting;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.util.HashMap;

@SpringBootApplication
@EnableJpaRepositories
@EnableConfigurationProperties(ApplicationProperties.class)
public class MultiTenantApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiTenantApplication.class, args);
    }

    @Bean("tenantDataSources")
    public DataSource dataSource(DataSourceProperties properties) {
        TenantDataSourceRouting customDataSource = new TenantDataSourceRouting();
        DataSource dataSource = properties.initializeDataSourceBuilder().build();
        customDataSource.setDefaultTargetDataSource(dataSource);
        customDataSource.setTargetDataSources(new HashMap<>());
        return customDataSource;
    }

}
