package com.pydlove.frametalk.identity.application.command;

import com.pydlove.frametalk.identity.application.query.AuthSessionGrant;
import com.pydlove.frametalk.identity.application.query.AuthenticatedUser;
import com.pydlove.frametalk.identity.application.query.CurrentAuthSession;
import com.pydlove.frametalk.identity.domain.model.AuthSession;
import com.pydlove.frametalk.identity.domain.model.Email;
import com.pydlove.frametalk.identity.domain.model.User;
import com.pydlove.frametalk.identity.domain.port.PasswordHasher;
import com.pydlove.frametalk.identity.domain.port.SessionTokenCodec;
import com.pydlove.frametalk.identity.domain.repository.AuthSessionRepository;
import com.pydlove.frametalk.identity.domain.repository.UserRepository;
import com.pydlove.frametalk.shared.domain.AuthenticationException;
import com.pydlove.frametalk.shared.domain.DomainException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MAX_SESSION_TOKEN_LENGTH = 128;

    private final UserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;
    private final SessionTokenCodec tokenCodec;
    private final EmailVerificationService emailVerificationService;
    private final Clock clock;
    private final Duration sessionTtl;
    private final Duration rememberedSessionTtl;

    public AuthService(
            UserRepository userRepository,
            AuthSessionRepository sessionRepository,
            PasswordHasher passwordHasher,
            SessionTokenCodec tokenCodec,
            EmailVerificationService emailVerificationService,
            Clock clock,
            Duration sessionTtl,
            Duration rememberedSessionTtl) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.tokenCodec = Objects.requireNonNull(tokenCodec);
        this.emailVerificationService = Objects.requireNonNull(emailVerificationService);
        this.clock = Objects.requireNonNull(clock);
        this.sessionTtl = requirePositive(sessionTtl, "会话有效期");
        this.rememberedSessionTtl = requirePositive(
                rememberedSessionTtl,
                "保持登录有效期");
    }

    public void sendRegistrationCode(String email) {
        emailVerificationService.sendRegistrationCode(email);
    }

    public AuthenticatedUser register(RegisterCommand command) {
        Objects.requireNonNull(command, "注册命令");
        Email email = Email.of(command.email());
        requirePasswordPolicy(command.password());
        if (!command.password().equals(command.confirmPassword())) {
            throw new DomainException("两次输入的密码不一致");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DomainException("该邮箱已经注册，请直接登录");
        }

        emailVerificationService.verifyRegistrationCode(email, command.emailCode());
        Instant now = clock.instant();
        User user = User.register(
                email,
                defaultDisplayName(email),
                passwordHasher.hash(command.password()),
                now);
        return AuthenticatedUser.from(userRepository.save(user));
    }

    public AuthSessionGrant login(LoginCommand command) {
        User user = requireUser(command);
        Instant now = clock.instant();
        Duration ttl = command.rememberMe()
                ? rememberedSessionTtl
                : sessionTtl;
        String rawToken = tokenCodec.generateRawToken();
        AuthSession session = AuthSession.open(
                user.id(),
                tokenCodec.hash(rawToken),
                now,
                ttl,
                command.rememberMe());
        sessionRepository.save(session);
        return new AuthSessionGrant(
                AuthenticatedUser.from(user),
                rawToken,
                session.expiresAt(),
                session.remembered());
    }

    public CurrentAuthSession currentSession(String rawToken) {
        Instant now = clock.instant();
        AuthSession session = findActiveSession(rawToken, now)
                .orElseThrow(() -> new AuthenticationException("登录已失效，请重新登录"));
        User user = userRepository.findById(session.userId())
                .orElseThrow(() -> new AuthenticationException("登录已失效，请重新登录"));
        return new CurrentAuthSession(
                AuthenticatedUser.from(user),
                session.expiresAt(),
                session.remembered());
    }

    public Optional<AuthenticatedUser> authenticate(String rawToken) {
        return findActiveSession(rawToken, clock.instant())
                .flatMap(session -> userRepository.findById(session.userId()))
                .map(AuthenticatedUser::from);
    }

    public void changePassword(String rawToken, ChangePasswordCommand command) {
        if (command == null) {
            throw new DomainException("请输入修改密码信息");
        }
        String currentPassword = command.currentPassword() == null
                ? ""
                : command.currentPassword();
        if (currentPassword.isEmpty()) {
            throw new DomainException("请输入当前密码");
        }
        requirePasswordPolicy(command.newPassword());
        if (!command.newPassword().equals(command.confirmPassword())) {
            throw new DomainException("两次输入的新密码不一致");
        }

        CurrentAuthSession current = currentSession(rawToken);
        User user = userRepository.findById(current.user().id())
                .orElseThrow(() -> new AuthenticationException("登录已失效，请重新登录"));
        if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
            throw new DomainException("当前密码不正确");
        }
        if (passwordHasher.matches(command.newPassword(), user.passwordHash())) {
            throw new DomainException("新密码不能与当前密码相同");
        }

        Instant now = clock.instant();
        userRepository.save(user.withPasswordHash(
                passwordHasher.hash(command.newPassword()),
                now));
        sessionRepository.deleteByUserIdExceptTokenHash(
                user.id(),
                tokenCodec.hash(rawToken));
    }

    public void logout(String rawToken) {
        if (!isUsableToken(rawToken)) return;
        try {
            sessionRepository.deleteByTokenHash(tokenCodec.hash(rawToken));
        } catch (DomainException ignored) {
            // An invalid cookie is equivalent to having no session.
        }
    }

    private Optional<AuthSession> findActiveSession(String rawToken, Instant now) {
        if (!isUsableToken(rawToken)) return Optional.empty();
        try {
            return sessionRepository.findActive(tokenCodec.hash(rawToken), now)
                    .filter(session -> !session.isExpired(now));
        } catch (DomainException ignored) {
            return Optional.empty();
        }
    }

    private User requireUser(LoginCommand command) {
        Objects.requireNonNull(command, "登录命令");
        int passwordLength = command.password() == null
                ? 0
                : command.password().codePointCount(0, command.password().length());
        if (command.email() == null
                || command.email().isBlank()
                || passwordLength < MIN_PASSWORD_LENGTH
                || passwordLength > MAX_PASSWORD_LENGTH) {
            throw invalidCredentials();
        }

        User user;
        try {
            user = userRepository.findByEmail(Email.of(command.email()))
                    .orElseThrow(AuthService::invalidCredentials);
        } catch (DomainException ignored) {
            throw invalidCredentials();
        }
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw invalidCredentials();
        }
        return user;
    }

    private static boolean isUsableToken(String rawToken) {
        return rawToken != null
                && !rawToken.isBlank()
                && rawToken.length() <= MAX_SESSION_TOKEN_LENGTH;
    }

    private static AuthenticationException invalidCredentials() {
        return new AuthenticationException("邮箱或密码不正确");
    }

    private static void requirePasswordPolicy(String password) {
        if (password == null) {
            throw new DomainException("密码不能为空");
        }
        int length = password.codePointCount(0, password.length());
        if (length < MIN_PASSWORD_LENGTH) {
            throw new DomainException("密码至少需要 " + MIN_PASSWORD_LENGTH + " 个字符");
        }
        if (length > MAX_PASSWORD_LENGTH) {
            throw new DomainException("密码不能超过 " + MAX_PASSWORD_LENGTH + " 个字符");
        }
    }

    private static String defaultDisplayName(Email email) {
        String localPart = email.value().split("@")[0];
        if (localPart.isBlank()) return "帧言创作者";
        String normalized = localPart.toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT)
                + normalized.substring(1);
    }

    private static Duration requirePositive(Duration value, String label) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new DomainException(label + "必须大于零");
        }
        return value;
    }
}
