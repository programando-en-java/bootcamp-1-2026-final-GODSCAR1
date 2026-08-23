package com.programandoenjava.airline.auth.infrastructure.adapter.in.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private final RSAKey signingKey;

    JwksController(final RSAKey signingKey) {
        this.signingKey = signingKey;
    }

    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        JWKSet publicKeys = new JWKSet(signingKey.toPublicJWK());

        return publicKeys.toJSONObject();
    }
}
