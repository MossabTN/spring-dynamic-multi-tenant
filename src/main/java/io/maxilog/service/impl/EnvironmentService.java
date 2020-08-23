package io.maxilog.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.maxilog.config.tenant.TenantDataSourceRepository;
import io.maxilog.dto.TenantConfigDTO;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;


@Service
public class EnvironmentService {

    private final Flyway flyway;
    private final Environment environment;
    private final ObjectMapper mapper;
    @Value("${maxilog.keycloak.url}")
    private String host;
    @Value("${maxilog.keycloak.realm}")
    private String realm;
    @Value("${spring.security.oauth2.client.registration.default.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.default.client-secret}")
    private String clientSecret;
    private Keycloak keycloak;

    public EnvironmentService(Flyway flyway, Environment environment, ObjectMapper mapper) {
        this.flyway = flyway;
        this.environment = environment;
        this.mapper = mapper;
    }

    @PostConstruct
    public void init() {
        this.checkMigration();
        keycloak = KeycloakBuilder
                .builder()
                .serverUrl(host.concat("/auth")).realm("master")
                .clientId("admin-cli")
                //.clientSecret(clientSecret)
                //.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .username("admin").password("admin").grantType(OAuth2Constants.PASSWORD)
                .build();
    }

    public void createEnv(TenantConfigDTO configDTO) throws Exception {
        createDataBase(configDTO);
        createRealm(configDTO);
    }


    public void checkMigration() {
        ClassicConfiguration configuration = ((ClassicConfiguration) flyway.getConfiguration());
        configuration.setSqlMigrationPrefix("MXV");
        TenantDataSourceRepository.getDataSources().values().forEach(dataSource -> {
            configuration.setDataSource(dataSource);
            Flyway f = new Flyway(configuration);
            f.migrate();
        });

    }

    public void migrate(String tenant) {
        if (!Objects.equals(environment.getProperty("spring.profiles.active"), "test")) {
            ClassicConfiguration configuration = ((ClassicConfiguration) flyway.getConfiguration());
            configuration.setSqlMigrationPrefix("MXV");
            DataSource dataSource = TenantDataSourceRepository.findDataSource(tenant);
            configuration.setDataSource(dataSource);
            Flyway f = new Flyway(configuration);
            f.migrate();
        }
    }


    private void createDataBase(TenantConfigDTO configDTO) throws SQLException {
        configDTO.setDatasourceName("maxilog-default".replaceAll("default", configDTO.getTenantId()));
        configDTO.setDatasourceUsername("maxilog-default-user".replaceAll("default", configDTO.getTenantId()));
        configDTO.setDatasourcePassword("maxilog-default-password".replaceAll("default", configDTO.getTenantId()));
        try (Connection connection = TenantDataSourceRepository.getDefaultDataSource().getConnection()) {
            Statement statement = connection.createStatement();
            statement.addBatch("CREATE DATABASE \"" + configDTO.getDatasourceName() + "\"");
            statement.addBatch("CREATE USER \"" + configDTO.getDatasourceUsername() + "\" WITH PASSWORD '" + configDTO.getDatasourcePassword() + "'");
            statement.addBatch("GRANT ALL PRIVILEGES ON DATABASE \"" + configDTO.getDatasourceName() + "\" TO \"" + configDTO.getDatasourceUsername() + "\"");
            statement.executeBatch();
        }
    }

    private void createRealm(TenantConfigDTO configDTO) throws IOException {
        configDTO.setOidcRealm(configDTO.getTenantId());
        configDTO.setOidcClient("back");
        configDTO.setOidcSecret("back");

        RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm(configDTO.getOidcRealm());
        newRealm.setEnabled(true);
        keycloak.realms().create(newRealm);

        RealmRepresentation realmRepresentation = mapper.readValue(new File("src/main/docker/data/realm/realm.json"), RealmRepresentation.class);
        PartialImportRepresentation partialImport = new PartialImportRepresentation();
        partialImport.setIfResourceExists(PartialImportRepresentation.Policy.SKIP.toString());
        partialImport.setClients(realmRepresentation.getClients());
        partialImport.setGroups(realmRepresentation.getGroups());
        partialImport.setIdentityProviders(realmRepresentation.getIdentityProviders());
        partialImport.setRoles(realmRepresentation.getRoles());
        partialImport.setUsers(realmRepresentation.getUsers());
        keycloak.realm(configDTO.getOidcRealm()).partialImport(partialImport);

    }
}
