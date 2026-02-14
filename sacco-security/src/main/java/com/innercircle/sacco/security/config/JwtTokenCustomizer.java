package com.innercircle.sacco.security.config;

import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserAccountRepository userAccountRepository;

    @Override
    public void customize(JwtEncodingContext context) {
        if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()) ||
                "access_token".equals(context.getTokenType().getValue())) {

            Authentication principal = context.getPrincipal();
            String username = principal.getName();

            userAccountRepository.findByUsername(username).ifPresent(userAccount -> {
                JwtClaimsSet.Builder claims = context.getClaims();

                claims.claim("userId", userAccount.getId().toString());
                claims.claim("email", userAccount.getEmail());

                Set<String> roles = userAccount.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet());
                claims.claim("roles", roles);

                Set<String> authorities = principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());
                claims.claim("authorities", authorities);
            });
        }
    }
}
