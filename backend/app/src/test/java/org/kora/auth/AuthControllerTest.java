package org.kora.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kora.auth.AuthController.MemberView;
import org.kora.auth.AuthController.RegisterRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

class AuthControllerTest {
    @Mock
    private MemberRepository members;

    @Mock
    private PasswordEncoder passwords;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository contexts;

    private AutoCloseable mocks;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        controller = new AuthController(members, passwords, authenticationManager, contexts);
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        mocks.close();
    }

    @Test
    void registrationTrimsFieldsHashesPasswordAndReturnsSafeMemberView() {
        when(members.existsByUsernameIgnoreCase("Alice")).thenReturn(false);
        when(members.existsByEmailIgnoreCase("alice@example.test")).thenReturn(false);
        when(passwords.encode("long-enough-password")).thenReturn("encoded-hash");
        when(members.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemberView result = controller.register(new RegisterRequest(
                " Alice ",
                " ALICE@Example.Test ",
                " Alice Example ",
                "long-enough-password"));

        ArgumentCaptor<Member> savedMember = ArgumentCaptor.forClass(Member.class);
        verify(members).save(savedMember.capture());
        assertEquals("Alice", savedMember.getValue().getUsername());
        assertEquals("alice@example.test", savedMember.getValue().getEmail());
        assertEquals("Alice Example", savedMember.getValue().getDisplayName());
        assertEquals("encoded-hash", savedMember.getValue().getPasswordHash());
        assertEquals(new MemberView("Alice", "Alice Example"), result);
    }

    @Test
    void registrationRejectsUsernameAlreadyInUseWithoutSaving() {
        when(members.existsByUsernameIgnoreCase("Alice")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.register(new RegisterRequest(
                        "Alice", "alice@example.test", "Alice", "long-enough-password")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(members, never()).save(any(Member.class));
    }

    @Test
    void loginTrimsUsernameRotatesExistingSessionAndPersistsSecurityContext() {
        Member member = member("Alice");
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                "Alice", null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(members.findByUsernameIgnoreCase("Alice")).thenReturn(Optional.of(member));
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        HttpSession session = org.mockito.Mockito.mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        MemberView result = controller.login(
                new AuthController.LoginRequest(" alice ", "secret-password"),
                request,
                response);

        ArgumentCaptor<Authentication> attempted = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(attempted.capture());
        assertEquals("alice", attempted.getValue().getPrincipal());
        verify(request).changeSessionId();
        verify(contexts).saveContext(any(SecurityContext.class), same(request), same(response));
        assertEquals(authenticated, SecurityContextHolder.getContext().getAuthentication());
        assertEquals(new MemberView("Alice", "Alice"), result);
    }

    @Test
    void userDetailsLookupFindsUsernameWithoutCaseSensitivity() {
        Member member = member("Alice");
        when(members.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(member));

        var details = new SecurityConfig().userDetailsService(members).loadUserByUsername("alice");

        assertEquals("Alice", details.getUsername());
        assertEquals("encoded-hash", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_MEMBER")));
    }

    @Test
    void logoutInvalidatesSessionAndSavesEmptySecurityContext() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        HttpSession session = org.mockito.Mockito.mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("Alice", null, List.of()));

        var result = controller.logout(request, response);

        verify(session).invalidate();
        ArgumentCaptor<SecurityContext> saved = ArgumentCaptor.forClass(SecurityContext.class);
        verify(contexts).saveContext(saved.capture(), same(request), same(response));
        assertNull(saved.getValue().getAuthentication());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(Boolean.TRUE, result.get("loggedOut"));
    }

    @Test
    void anonymousSessionIsReturnedAsUnauthenticated() {
        assertEquals(new AuthController.SessionView(false, null), controller.me(null));
    }

    private Member member(String username) {
        return new Member(username, username.toLowerCase() + "@example.test", username, "encoded-hash");
    }
}
