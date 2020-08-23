package io.maxilog.security;

import io.maxilog.config.tenant.TenantDataSourceRepository;
import io.maxilog.domain.TenantConfig;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TokenManager {

    private final Map<String, Keycloak> currentTokens = new HashMap<>();
    private final Map<String, Long> expirationTimes = new HashMap<>();
    @Value(value = "maxilog.keycloak.url")
    String host;
    @Value(value = "maxilog.keycloak.realm")
    String realm;
    @Value(value = "quarkus.oidc.client-id")
    String clientId;
    @Value(value = "quarkus.oidc.credentials.secret")
    String clientSecret;

    public synchronized AccessTokenResponse getAccessToken(String tenant) {
        if (currentTokens.get(tenant) == null) {
            grantToken(tenant);
        } else if (tokenExpired(tenant)) {
            refreshToken(tenant);
        }
        return currentTokens.get(tenant).tokenManager().getAccessToken();
    }

    private AccessTokenResponse grantToken(String tenant) {

        Keycloak keycloak;
        if (tenant == null || Objects.equals(tenant, "default")) {
            keycloak = KeycloakBuilder
                    .builder()
                    .serverUrl(host.concat("/auth")).realm(this.realm)
                    .clientId(clientId).clientSecret(clientSecret)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .build();
        } else {
            TenantConfig tenantConfig = Optional.ofNullable(TenantDataSourceRepository.findTenantConfig(tenant)).orElseThrow(() -> new NotFoundException("No config for this client"));
            keycloak = KeycloakBuilder
                    .builder()
                    .serverUrl(host.concat("/auth")).realm(tenantConfig.getOidcRealm())
                    .clientId(tenantConfig.getOidcClient()).clientSecret(tenantConfig.getOidcSecret())
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .build();
        }
        long requestTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
        synchronized (TokenManager.class) {
            AccessTokenResponse currentToken = keycloak.tokenManager().grantToken();
            currentTokens.put(tenant, keycloak);
            expirationTimes.put(tenant, requestTime + currentToken.getExpiresIn());
        }
        return currentTokens.get(tenant).tokenManager().getAccessToken();
    }

    private synchronized AccessTokenResponse refreshToken(String tenant) {
        try {
            long requestTime = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
            AccessTokenResponse currentToken = this.currentTokens.get(tenant).tokenManager().refreshToken();
            expirationTimes.put(tenant, requestTime + currentToken.getExpiresIn());
            return currentToken;
        } catch (WebApplicationException e) {
            return grantToken(tenant);
        }
    }

    private synchronized boolean tokenExpired(String tenant) {
        long minTokenValidity = 30L;
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond() + minTokenValidity >= expirationTimes.getOrDefault(tenant, 0L);
    }

}
