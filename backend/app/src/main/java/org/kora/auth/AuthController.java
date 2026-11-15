package org.kora.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final MemberRepository members;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts;

    public AuthController(
            MemberRepository members,
            PasswordEncoder passwords,
            AuthenticationManager authenticationManager,
            SecurityContextRepository contexts) {
        this.members = members;
        this.passwords = passwords;
        this.authenticationManager = authenticationManager;
        this.contexts = contexts;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/register")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public MemberView register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        if (members.existsByUsernameIgnoreCase(username) || members.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already in use");
        }
        Member saved = members.save(new Member(
                username,
                email,
                request.displayName().trim(),
                passwords.encode(request.password())));
        return MemberView.from(saved);
    }

    @PostMapping("/login")
    public MemberView login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (servletRequest.getSession(false) != null) {
            servletRequest.changeSessionId();
        }
        contexts.saveContext(context, servletRequest, servletResponse);
        Member member = members.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return MemberView.from(member);
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        SecurityContextHolder.clearContext();
        if (servletRequest.getSession(false) != null) {
            servletRequest.getSession(false).invalidate();
        }
        contexts.saveContext(SecurityContextHolder.createEmptyContext(), servletRequest, servletResponse);
        return Map.of("loggedOut", true);
    }

    @GetMapping("/me")
    public SessionView me(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return new SessionView(false, null);
        }
        Member member = members.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new SessionView(true, MemberView.from(member));
    }

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$") String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 2, max = 80) String displayName,
            @NotBlank @Size(min = 12, max = 128) String password) {}

    public record LoginRequest(
            @NotBlank @Size(max = 32) String username,
            @NotBlank @Size(max = 128) String password) {}

    public record MemberView(String username, String displayName) {
        static MemberView from(Member member) {
            return new MemberView(member.getUsername(), member.getDisplayName());
        }
    }

    public record SessionView(boolean authenticated, MemberView user) {}
}
