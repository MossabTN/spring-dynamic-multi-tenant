package io.maxilog.web;


import io.maxilog.dto.TenantConfigDTO;
import io.maxilog.service.impl.TenantConfigService;
import io.maxilog.web.Util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@RestController
@RequestMapping("/api/tenants")
public class TenantConfigResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantConfigResource.class);

    private final TenantConfigService tenantConfigService;

    public TenantConfigResource(TenantConfigService tenantConfigService) {
        this.tenantConfigService = tenantConfigService;
    }

    @GetMapping("/{tenant}/config")
    public ResponseEntity findById(@PathVariable("tenant") String tenant) {
        LOGGER.debug("REST request to get tenant Config by tenantId : {}", tenant);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(tenantConfigService.findById(tenant)));
    }

    @PostMapping("/{tenant}")
    public ResponseEntity addClient(@PathVariable("tenant") String tenant) throws URISyntaxException {
        LOGGER.debug("REST request to save tenant : {}", tenant);
        tenantConfigService.save(new TenantConfigDTO(tenant));
        return ResponseEntity
                .created(new URI("/tenants/" + tenant + "/config"))
                .build();
    }
}
