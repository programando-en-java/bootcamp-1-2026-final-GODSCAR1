package com.programandoenjava.airline.auth.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jwt.SignedJWT;
import com.programandoenjava.airline.auth.EnableDatabaseTest;
import com.programandoenjava.airline.auth.TestcontainersConfiguration;
import com.programandoenjava.airline.auth.infrastructure.adapter.out.passwords.PasswordConfiguration;
import com.programandoenjava.airline.auth.infrastructure.adapter.out.persistence.user.UserPersistenceConfiguration;
import com.programandoenjava.airline.auth.infrastructure.adapter.out.tokens.TokenConfiguration;
import com.programandoenjava.airline.auth.infrastructure.config.ApplicationConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.text.ParseException;
import java.util.List;

@SpringBootTest(classes = {
        AuthController.class,
        JwksController.class,
        GlobalExceptionHandler.class,
        ApplicationConfiguration.class,
        UserPersistenceConfiguration.class,
        PasswordConfiguration.class,
        TokenConfiguration.class
})
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        ValidationAutoConfiguration.class,
        JacksonAutoConfiguration.class
})
@AutoConfigureMockMvc
@DisplayName("Logging in")
class LoginSliceTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String JWKS = "/.well-known/jwks.json";

    private static final String PASSENGER = "passenger@airline.test";
    private static final String PASSENGER_PASSWORD = "passenger123";
    private static final String PASSENGER_ID = "11111111-1111-4111-8111-111111111111";

    private static final String ADMIN = "admin@airline.test";
    private static final String ADMIN_PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("with the right password")
    class RightPassword {

        @Test
        @DisplayName("should hand back a token")
        void shouldHandBackAToken() throws Exception {
            mockMvc.perform(login(PASSENGER, PASSENGER_PASSWORD))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.expiresAt").isNotEmpty());
        }

        @Test
        @DisplayName("should name the user in the subject")
        void shouldNameTheUserInTheSubject() throws Exception {
            SignedJWT token = tokenFrom(login(PASSENGER, PASSENGER_PASSWORD));

            Assertions.assertThat(subjectOf(token)).isEqualTo(PASSENGER_ID);
        }

        @Test
        @DisplayName("should carry the roles the account has")
        void shouldCarryTheRolesTheAccountHas() throws Exception {
            SignedJWT token = tokenFrom(login(ADMIN, ADMIN_PASSWORD));

            Assertions.assertThat(rolesOf(token)).containsExactly("ADMIN");
        }

        @Test
        @DisplayName("should sign it, and say which key signed it")
        void shouldSignItAndSayWhichKeySignedIt() throws Exception {
            SignedJWT token = tokenFrom(login(PASSENGER, PASSENGER_PASSWORD));

            Assertions.assertThat(token.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
            Assertions.assertThat(token.getHeader().getKeyID()).isNotBlank();
        }

        @Test
        @DisplayName("should not put the password anywhere in the answer")
        void shouldNotPutThePasswordAnywhereInTheAnswer() throws Exception {
            String body = mockMvc.perform(login(PASSENGER, PASSENGER_PASSWORD))
                    .andReturn().getResponse().getContentAsString();

            Assertions.assertThat(body).doesNotContain(PASSENGER_PASSWORD);
        }
    }

    @Nested
    @DisplayName("with the wrong credentials")
    class WrongCredentials {

        @Test
        @DisplayName("should refuse a wrong password")
        void shouldRefuseAWrongPassword() throws Exception {
            mockMvc.perform(login(PASSENGER, "not the password"))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("should refuse an email nobody has")
        void shouldRefuseAnEmailNobodyHas() throws Exception {
            mockMvc.perform(login("nobody@airline.test", PASSENGER_PASSWORD))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("should answer both the same way")
        void shouldAnswerBothTheSameWay() throws Exception {
            String wrongPassword = bodyOf(login(PASSENGER, "not the password"));
            String noSuchUser = bodyOf(login("nobody@airline.test", PASSENGER_PASSWORD));

            Assertions.assertThat(noSuchUser).isEqualTo(wrongPassword);
        }

        @Test
        @DisplayName("should reject a request with no password at all")
        void shouldRejectARequestWithNoPasswordAtAll() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "passenger@airline.test"}
                                    """))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("the published keys")
    class Jwks {

        @Test
        @DisplayName("should offer a key anyone can verify with")
        void shouldOfferAKeyAnyoneCanVerifyWith() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(JWKS))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.keys[0].kty").value("RSA"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.keys[0].kid").isNotEmpty());
        }

        /*
         * The test that matters here. A private exponent on this endpoint would
         * let anyone mint tokens, and it is one wrong method call away.
         */
        @Test
        @DisplayName("should never publish the private half")
        void shouldNeverPublishThePrivateHalf() throws Exception {
            String body = mockMvc.perform(MockMvcRequestBuilders.get(JWKS))
                    .andReturn().getResponse().getContentAsString();

            Assertions.assertThat(body)
                    .doesNotContain("\"d\"")
                    .doesNotContain("\"p\"")
                    .doesNotContain("\"q\"");
        }
    }

    private static MockHttpServletRequestBuilder login(final String email,
                                                      final String password) {

        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);

        return MockMvcRequestBuilders.post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String bodyOf(final MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse().getContentAsString();
    }

    private SignedJWT tokenFrom(final MockHttpServletRequestBuilder request) throws Exception {
        String body = bodyOf(request);
        String token = JsonPath.read(body, "$.accessToken");

        return SignedJWT.parse(token);
    }

    private static String subjectOf(final SignedJWT token) throws ParseException {
        return token.getJWTClaimsSet().getSubject();
    }

    private static List<String> rolesOf(final SignedJWT token) throws ParseException {
        return token.getJWTClaimsSet().getStringListClaim("roles");
    }
}
