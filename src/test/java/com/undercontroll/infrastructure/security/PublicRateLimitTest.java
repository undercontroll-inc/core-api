package com.undercontroll.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicRateLimitTest {

    private static MockHttpServletResponse call(RateLimitFilter filter, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    @DisplayName("the customer page has its own 30/min bucket per IP with a pt-BR page when exceeded")
    void publicBucket() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(10, 100, 30);
        filter.init();

        for (int i = 0; i < 30; i++) {
            assertEquals(200, call(filter, "/p/7Hq2xLmP9vKa3TzR8dNw4c", "10.0.0.1").getStatus());
        }
        MockHttpServletResponse blocked = call(filter, "/p/7Hq2xLmP9vKa3TzR8dNw4c", "10.0.0.1");
        assertEquals(429, blocked.getStatus());
        assertTrue(blocked.getContentAsString().contains("Aguarde um minuto"));
        assertEquals(429, call(filter, "/v1/public/orders/x", "10.0.0.1").getStatus());
        assertEquals(200, call(filter, "/v1/api/orders", "10.0.0.1").getStatus());
        assertEquals(200, call(filter, "/p/x", "10.0.0.2").getStatus());
        assertTrue(RateLimitFilter.isPublicEndpoint("/v1/public/orders/x"));
        assertFalse(RateLimitFilter.isPublicEndpoint("/v1/api/orders"));
    }
}
