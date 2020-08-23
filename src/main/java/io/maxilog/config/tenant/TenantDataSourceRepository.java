package io.maxilog.config.tenant;

import io.maxilog.config.ApplicationProperties;
import io.maxilog.domain.TenantConfig;
import io.maxilog.repository.TenantConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Component
@DependsOn("tenantDataSources")
public class TenantDataSourceRepository implements Serializable {

    private static Map<String, TenantConfig> tenantConfigs = new HashMap<>();
    private static DataSource defaultDataSource;
    private static String dataSourceHost;

    @Autowired
    private TenantConfigRepository tenantConfigRepository;
    @Autowired
    private ApplicationProperties applicationProperties;
    @Autowired
    private DataSourceProperties dataSourceProperties;

    @PostConstruct
    public void getAll() {
        defaultDataSource = dataSourceProperties.initializeDataSourceBuilder().build();
        dataSourceHost = applicationProperties.getDatasource().getHost();
        List<TenantConfig> tenantConfigs = tenantConfigRepository.findAll();
        TenantDataSourceRepository.tenantConfigs = tenantConfigs
                .stream()
                .collect(Collectors.toMap(TenantConfig::getTenantId, tenantConfig -> tenantConfig));
    }

    public static Map<String, DataSource> getDataSources() {
        return tenantConfigs.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, es -> buildDatasource(es.getValue())));
    }

    public static DataSource getDefaultDataSource(){
        return defaultDataSource;
    }

    public static DataSource findDataSource(String name) {
        return buildDatasource(tenantConfigs.get(name));
    }

    public static TenantConfig findTenantConfig(String name) {
        return tenantConfigs.get(name);
    }

    public static void addTenantConfig(TenantConfig config) {
        tenantConfigs.put(config.getTenantId(), config);
    }

    public static DataSource buildDatasource(TenantConfig config) {
        return Optional.ofNullable(config)
                .map(config1 -> DataSourceBuilder
                        .create().driverClassName("org.postgresql.Driver")
                        .username(config1.getDatasourceUsername())
                        .password(config1.getDatasourcePassword())
                        .url(dataSourceHost + config1.getDatasourceName())
                        .build())
                .orElse(null);
    }

}