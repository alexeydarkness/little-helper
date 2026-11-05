package ru.littlehelper.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ru.littlehelper.backend.user.User;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

	private final JwtEncoder encoder;
	private final Duration lifetime;

	public JwtService(JwtEncoder encoder, @Value("${app.jwt.lifetime}") Duration lifetime) {
		this.encoder = encoder;
		this.lifetime = lifetime;
	}

	public String createToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plus(lifetime))
				.claim("role", user.getRole().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
