package io.maxilog.config.tenant;

import io.maxilog.security.oauth2.AudienceValidator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class TenantOidcClientResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    private final Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();

    private final ClientRegistrationRepository clientRegistrationRepository;

    public TenantOidcClientResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        String tenantID = request.getHeader("x-tenant");
        if (tenantID == null) tenantID = "default";
        TenantContext.setCurrentTenant(tenantID);
        return this.authenticationManagers.computeIfAbsent(tenantID, this::fromTenant);
    }

    private AuthenticationManager fromTenant(String tenant) {
        return Optional.ofNullable(this.clientRegistrationRepository.findByRegistrationId(tenant))
                .map(clientRegistration -> {
                    NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(clientRegistration.getClientName());
                    OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(Arrays.asList("account", "api://default"));
                    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(clientRegistration.getClientName());
                    OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);
                    jwtDecoder.setJwtValidator(withAudience);
                    return jwtDecoder;
                })
                .map(JwtAuthenticationProvider::new)
                .orElseThrow(() -> new IllegalArgumentException("unknown tenant"))::authenticate;
    }

}