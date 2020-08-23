package io.maxilog.service.impl;

import io.maxilog.config.ApplicationProperties;
import io.maxilog.config.tenant.TenantDataSourceRepository;
import io.maxilog.domain.TenantConfig;
import io.maxilog.dto.TenantConfigDTO;
import io.maxilog.dto.TenantOIDCConfigDTO;
import io.maxilog.mapper.TenantConfigMapper;
import io.maxilog.repository.TenantConfigRepository;
import io.maxilog.web.errors.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;

@Service
public class TenantConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(TenantConfigService.class);

    private final EnvironmentService multiTenantService;
    private final TenantConfigRepository tenantConfigRepository;
    private final TenantConfigMapper tenantConfigMapper;
    private final ApplicationProperties configuration;


    public TenantConfigService(EnvironmentService multiTenantService, TenantConfigRepository tenantConfigRepository, TenantConfigMapper tenantConfigMapper, ApplicationProperties configuration) {
        this.multiTenantService = multiTenantService;
        this.tenantConfigRepository = tenantConfigRepository;
        this.tenantConfigMapper = tenantConfigMapper;
        this.configuration = configuration;
    }

    public TenantOIDCConfigDTO findById(String tenant) {
        return tenantConfigRepository.findByTenantId(tenant)
                .map(clientConfig -> new TenantOIDCConfigDTO(configuration.getKeycloak().getUrl() + "/auth/", clientConfig.getOidcRealm()))
                .orElseThrow(() -> new NotFoundException("Client Config Not Found"));
    }

    public void save(TenantConfigDTO tenantConfigDTO) {
        try {
            multiTenantService.createEnv(tenantConfigDTO);
            TenantConfig config = tenantConfigMapper.toEntity(tenantConfigDTO);
            tenantConfigRepository.save(config);
            TenantDataSourceRepository.addTenantConfig(config);
            multiTenantService.migrate(config.getTenantId());
        } catch (Exception e) {
            //TODO delete database and realm
            e.printStackTrace();
            throw new WebApplicationException("Error while creating new tenant");
        }

    }

}
