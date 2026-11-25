package com.academy.coworkhub.business.security;

import com.academy.coworkhub.persistence.entity.UserAccount;
import com.academy.coworkhub.persistence.repository.UserAccountRepository;
import com.academy.coworkhub.presentation.dto.request.LoginRequest;
import com.academy.coworkhub.presentation.dto.response.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** RF-11: valida usuario y contraseña y emite un JWT con sub, role y memberId. */
@Service
public class AuthService {

    public static final String ROLE_CLAIM = "role";
    public static final String MEMBER_ID_CLAIM = "memberId";

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final int expirationMinutes;

    public AuthService(AuthenticationManager authenticationManager,
                       UserAccountRepository userAccountRepository,
                       JwtEncoder jwtEncoder,
                       Clock clock,
                       @Value("${coworkhub.jwt.expiration-minutes}") int expirationMinutes) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.expirationMinutes = expirationMinutes;
    }

    /** Lanza BadCredentialsException (401) si el usuario o la contraseña no coinciden. */
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        UserAccount account = userAccountRepository.findByUsername(request.username()).orElseThrow();

        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("coworkhub")
                .subject(account.getUsername())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(ROLE_CLAIM, account.getRole().name());
        if (account.getMember() != null) {
            claims.claim(MEMBER_ID_CLAIM, account.getMember().getId());
        }
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        return new LoginResponse(token, "Bearer", expiresAt);
    }
}
