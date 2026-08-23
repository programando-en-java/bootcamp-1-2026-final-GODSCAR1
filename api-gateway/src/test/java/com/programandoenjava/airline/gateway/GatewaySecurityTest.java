package com.programandoenjava.airline.gateway;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("The one door in")
class GatewaySecurityTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String SEARCH = "/api/v1/flights";
    private static final String BOOKINGS = "/api/v1/bookings";
    private static final String BOARDING_PASSES = "/api/v1/boarding-passes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("without a token")
    class Anonymous {

        @Test
        @DisplayName("should refuse to make a booking")
        void shouldRefuseToMakeABooking() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(BOOKINGS))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("should refuse to check in")
        void shouldRefuseToCheckIn() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(BOARDING_PASSES))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        /*
         * Getting as far as trying to connect is the assertion. There is
         * nothing behind this gateway in a test, so a request that security
         * allowed ends in a failure to reach the downstream, and one that
         * security refused never gets that far.
         */
        @Test
        @DisplayName("should let a login through, since logging in cannot require a login")
        void shouldLetALoginThrough() {
            Assertions.assertThatThrownBy(() -> mockMvc.perform(MockMvcRequestBuilders.post(LOGIN)))
                    .hasRootCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("should let a flight search through")
        void shouldLetAFlightSearchThrough() {
            Assertions.assertThatThrownBy(() -> mockMvc.perform(MockMvcRequestBuilders.get(SEARCH)))
                    .hasRootCauseInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("seat blocks")
    class SeatBlocks {

        @Test
        @DisplayName("should not be reachable, even by somebody logged in")
        void shouldNotBeReachableEvenBySomebodyLoggedIn() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders
                            .post(seatBlocks())
                            .with(SecurityMockMvcRequestPostProcessors.jwt()))
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        @DisplayName("should not be reachable for releasing either")
        void shouldNotBeReachableForReleasingEither() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders
                            .delete(seatBlocks() + "/" + UUID.randomUUID())
                            .with(SecurityMockMvcRequestPostProcessors.jwt()))
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        private String seatBlocks() {
            return "/api/v1/flights/" + UUID.randomUUID() + "/seat-blocks";
        }
    }
}
