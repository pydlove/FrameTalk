package com.pydlove.frametalk.identity.application.command;

import com.pydlove.frametalk.identity.application.query.AuthSessionGrant;
import com.pydlove.frametalk.identity.application.query.AuthenticatedUser;
import com.pydlove.frametalk.identity.domain.model.AuthSession;
import com.pydlove.frametalk.identity.domain.model.Email;
import com.pydlove.frametalk.identity.domain.model.User;
import com.pydlove.frametalk.identity.domain.model.VerificationCode;
import com.pydlove.frametalk.identity.domain.port.PasswordHasher;
import com.pydlove.frametalk.identity.domain.port.SessionTokenCodec;
import com.pydlove.frametalk.identity.domain.port.VerificationCodeCodec;
import com.pydlove.frametalk.identity.domain.port.VerificationEmailSender;
import com.pydlove.frametalk.identity.domain.repository.AuthSessionRepository;
import com.pydlove.frametalk.identity.domain.repository.UserRepository;
import com.pydlove.frametalk.identity.domain.repository.VerificationCodeRepository;
import com.pydlove.frametalk.shared.domain.AuthenticationException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-11-02T08:00:00Z");

    @Test
    void registrationLoginAndPasswordChangeWorkEndToEnd() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemorySessionRepository sessions = new InMemorySessionRepository();
        InMemoryVerificationCodeRepository codes =
                new InMemoryVerificationCodeRepository();
        FakePasswordHasher passwords = new FakePasswordHasher();
        FakeSessionTokenCodec tokens = new FakeSessionTokenCodec();
        FakeVerificationCodeCodec verificationCodes =
                new FakeVerificationCodeCodec();
        CapturingEmailSender emails = new CapturingEmailSender();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        EmailVerificationService verificationService =
                new EmailVerificationService(
                        users,
                        codes,
                        verificationCodes,
                        emails,
                        clock,
                        Duration.ofMinutes(5),
                        Duration.ofSeconds(60),
                        10);
        AuthService auth = new AuthService(
                users,
                sessions,
                passwords,
                tokens,
                verificationService,
                clock,
                Duration.ofHours(8),
                Duration.ofDays(30));

        auth.sendRegistrationCode("writer@example.com");
        assertEquals("123456", emails.lastCode);

        AuthenticatedUser registered = auth.register(new RegisterCommand(
                "writer@example.com",
                emails.lastCode,
                "secret123",
                "secret123"));
        assertEquals("Writer", registered.displayName());

        AuthSessionGrant grant = auth.login(new LoginCommand(
                "writer@example.com",
                "secret123",
                true));
        assertTrue(grant.remembered());
        assertTrue(auth.authenticate(grant.rawToken()).isPresent());

        auth.changePassword(
                grant.rawToken(),
                new ChangePasswordCommand(
                        "secret123",
                        "secret456",
                        "secret456"));

        assertThrows(
                AuthenticationException.class,
                () -> auth.login(new LoginCommand(
                        "writer@example.com",
                        "secret123",
                        false)));
        assertDoesNotThrow(() -> auth.login(new LoginCommand(
                "writer@example.com",
                "secret456",
                false)));
    }

    private static final class FakePasswordHasher implements PasswordHasher {

        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return hash(rawPassword).equals(encodedPassword);
        }
    }

    private static final class FakeSessionTokenCodec implements SessionTokenCodec {

        private int sequence;

        @Override
        public String generateRawToken() {
            sequence += 1;
            return "token-" + sequence;
        }

        @Override
        public String hash(String rawToken) {
            return "hashed:" + rawToken;
        }
    }

    private static final class FakeVerificationCodeCodec
            implements VerificationCodeCodec {

        @Override
        public String generateCode() {
            return "123456";
        }

        @Override
        public String hash(String rawCode) {
            return "hashed:" + rawCode;
        }
    }

    private static final class CapturingEmailSender
            implements VerificationEmailSender {

        private String lastCode;

        @Override
        public void sendRegistrationCode(
                Email email,
                String rawCode,
                Duration ttl) {
            lastCode = rawCode;
        }
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final AtomicLong ids = new AtomicLong();
        private final Map<Long, User> users = new LinkedHashMap<>();

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return users.values().stream()
                    .filter(user -> user.email().equals(email))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(Email email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public User save(User user) {
            User stored = user.id() == null
                    ? user.withId(ids.incrementAndGet())
                    : user;
            users.put(stored.id(), stored);
            return stored;
        }
    }

    private static final class InMemorySessionRepository
            implements AuthSessionRepository {

        private final AtomicLong ids = new AtomicLong();
        private final Map<Long, AuthSession> sessions = new LinkedHashMap<>();

        @Override
        public AuthSession save(AuthSession session) {
            AuthSession stored = session.id() == null
                    ? session.withId(ids.incrementAndGet())
                    : session;
            sessions.put(stored.id(), stored);
            return stored;
        }

        @Override
        public Optional<AuthSession> findActive(String tokenHash, Instant now) {
            return sessions.values().stream()
                    .filter(session -> session.tokenHash().equals(tokenHash))
                    .filter(session -> !session.isExpired(now))
                    .findFirst();
        }

        @Override
        public void deleteByTokenHash(String tokenHash) {
            sessions.values().removeIf(
                    session -> session.tokenHash().equals(tokenHash));
        }

        @Override
        public void deleteByUserIdExceptTokenHash(
                long userId,
                String currentTokenHash) {
            sessions.values().removeIf(session ->
                    session.userId() == userId
                            && !session.tokenHash().equals(currentTokenHash));
        }
    }

    private static final class InMemoryVerificationCodeRepository
            implements VerificationCodeRepository {

        private final AtomicLong ids = new AtomicLong();
        private final List<VerificationCode> codes = new ArrayList<>();

        @Override
        public Optional<VerificationCode> findActive(Email email, Instant now) {
            return codes.stream()
                    .filter(code -> code.email().equals(email.value()))
                    .filter(code -> code.isActive(now))
                    .max(Comparator.comparingLong(this::identity));
        }

        @Override
        public Optional<Instant> findLatestCreatedAt(Email email) {
            return codes.stream()
                    .filter(code -> code.email().equals(email.value()))
                    .max(Comparator.comparingLong(this::identity))
                    .map(VerificationCode::createdAt);
        }

        @Override
        public int countCreatedSince(Email email, Instant since) {
            return (int) codes.stream()
                    .filter(code -> code.email().equals(email.value()))
                    .filter(code -> !code.createdAt().isBefore(since))
                    .count();
        }

        @Override
        public VerificationCode save(VerificationCode code) {
            VerificationCode stored = code.id() == null
                    ? code.withId(ids.incrementAndGet())
                    : code;
            codes.removeIf(existing -> existing.id().equals(stored.id()));
            codes.add(stored);
            return stored;
        }

        private long identity(VerificationCode code) {
            return code.id() == null ? 0 : code.id();
        }
    }
}
