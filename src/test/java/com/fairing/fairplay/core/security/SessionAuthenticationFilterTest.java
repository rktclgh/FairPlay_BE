package com.fairing.fairplay.core.security;

import com.fairing.fairplay.core.service.SessionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticationFilterTest {

    @Mock
    private SessionService sessionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesFromRedisSessionWithoutUserRepositoryLookup() throws Exception {
        when(sessionService.getSessionData("session-1")).thenReturn(Map.of(
                "userId", 10,
                "email", "admin@example.com",
                "role", "ADMIN",
                "roleId", 1
        ));

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/events/applications/38");
        request.setCookies(new Cookie("FAIRPLAY_SESSION", "session-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SessionAuthenticationFilter(sessionService)
                .doFilter(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ADMIN");
        assertThat(((CustomUserDetails) authentication.getPrincipal()).getUserId()).isEqualTo(10L);
        assertThat(((CustomUserDetails) authentication.getPrincipal()).getEmail()).isEqualTo("admin@example.com");
        assertThat(((CustomUserDetails) authentication.getPrincipal()).getRoleId()).isEqualTo(1);

        verify(sessionService).getSessionData("session-1");
        verify(sessionService, never()).deleteSession("session-1");
    }

    @Test
    void dependsOnlyOnRedisSessionServiceForRequestAuthentication() {
        assertThat(SessionAuthenticationFilter.class.getDeclaredFields())
                .filteredOn(field -> !field.isSynthetic())
                .extracting(field -> field.getType().getName())
                .contains("com.fairing.fairplay.core.service.SessionService")
                .doesNotContain("com.fairing.fairplay.user.repository.UserRepository");
    }

    @Test
    void clearsRedisSessionWhenRoleIsMissing() throws Exception {
        when(sessionService.getSessionData("session-missing-role")).thenReturn(Map.of(
                "userId", 10,
                "email", "admin@example.com"
        ));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications/stream");
        request.setCookies(new Cookie("FAIRPLAY_SESSION", "session-missing-role"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SessionAuthenticationFilter(sessionService)
                .doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sessionService).deleteSession("session-missing-role");
    }

    @Test
    void doesNotAuthenticateWhenRedisSessionWasDeleted() throws Exception {
        when(sessionService.getSessionData("deleted-session")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications/stream");
        request.setCookies(new Cookie("FAIRPLAY_SESSION", "deleted-session"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SessionAuthenticationFilter(sessionService)
                .doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sessionService).getSessionData("deleted-session");
        verify(sessionService, never()).deleteSession("deleted-session");
    }

    @Test
    void skipsRedisSessionLookupForStaticUploadPathsEvenWhenCookieExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uploads/event/poster.png");
        request.setCookies(new Cookie("FAIRPLAY_SESSION", "session-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SessionAuthenticationFilter(sessionService)
                .doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sessionService, never()).getSessionData("session-1");
    }

    @Test
    void authenticatesProtectedEventRoleLookupEvenThoughItIsAGetRequest() throws Exception {
        when(sessionService.getSessionData("session-1")).thenReturn(Map.of(
                "userId", 10,
                "email", "admin@example.com",
                "role", "ADMIN",
                "roleId", 1
        ));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events/user/role");
        request.setCookies(new Cookie("FAIRPLAY_SESSION", "session-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SessionAuthenticationFilter(sessionService)
                .doFilter(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(((CustomUserDetails) authentication.getPrincipal()).getUserId()).isEqualTo(10L);
        verify(sessionService).getSessionData("session-1");
    }

    @Test
    void skipsRedisSessionLookupOnlyForKnownPublicEventGetPathsWhenCookieExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events/32/details");
        request.setCookies(new Cookie("FAIRPLAY_SESSION", "session-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SessionAuthenticationFilter(sessionService)
                .doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sessionService, never()).getSessionData("session-1");
    }
}
