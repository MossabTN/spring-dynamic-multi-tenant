package io.maxilog.config.tenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

public class TenantDataSourceRouting extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        if (TenantContext.getCurrentTenant() != null && !TenantContext.getCurrentTenant().equals("default")) {
            return TenantDataSourceRepository.findDataSource(TenantContext.getCurrentTenant());
        }
        return null;
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Object dataSource = this.determineCurrentLookupKey();
        if (dataSource instanceof DataSource) {
            return (DataSource) dataSource;
        }
        return super.determineTargetDataSource();
    }
}