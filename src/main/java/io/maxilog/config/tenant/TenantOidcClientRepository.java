package io.maxilog.config.tenant;

import io.maxilog.config.tenant.TenantDataSourceRepository;
import io.maxilog.web.errors.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Component
public class TenantOidcClientRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {
    private final String issuerUri;
    private final Map<String, ClientRegistration> registrations;

    public TenantOidcClientRepository(OAuth2ClientProperties properties, @Value("${spring.security.oauth2.client.provider.default.issuer-uri}") String issuerUri) {
        this.registrations = OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties);
        this.issuerUri = issuerUri;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null) return registrations.get("default");
        return this.registrations.computeIfAbsent(registrationId, this::buildClientRegistration);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return this.registrations.values().iterator();
    }

    private ClientRegistration buildClientRegistration(String registrationId) {
        return Optional.ofNullable(TenantDataSourceRepository.findTenantConfig(registrationId))
                .map(config -> ClientRegistrations.fromIssuerLocation(issuerUri.replace("default", registrationId))
                        .registrationId(config.getOidcRealm())
                        .clientId(config.getOidcClient())
                        .clientSecret(config.getOidcSecret())
                        .clientAuthenticationMethod(new ClientAuthenticationMethod("jwt"))
                        .build()).orElseThrow(() -> new NotFoundException("TENANT_NOT_FOUND"));
    }

}
