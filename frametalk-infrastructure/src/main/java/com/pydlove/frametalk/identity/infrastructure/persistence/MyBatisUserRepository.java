package com.pydlove.frametalk.identity.infrastructure.persistence;

import com.pydlove.frametalk.identity.domain.model.Email;
import com.pydlove.frametalk.identity.domain.model.User;
import com.pydlove.frametalk.identity.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MyBatisUserRepository implements UserRepository {

    private final UserMapper mapper;

    public MyBatisUserRepository(UserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(mapper.findById(id)).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return Optional.ofNullable(mapper.findByEmail(email.value()))
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return mapper.countByEmail(email.value()) > 0;
    }

    @Override
    public User save(User user) {
        UserData data = toData(user);
        if (data.getId() == null) {
            mapper.insert(data);
        } else {
            mapper.update(data);
        }
        return toDomain(data);
    }

    private User toDomain(UserData data) {
        return new User(
                data.getId(),
                Email.of(data.getEmail()),
                data.getDisplayName(),
                data.getPasswordHash(),
                data.getCreatedAt(),
                data.getUpdatedAt());
    }

    private UserData toData(User user) {
        UserData data = new UserData();
        data.setId(user.id());
        data.setEmail(user.email().value());
        data.setDisplayName(user.displayName());
        data.setPasswordHash(user.passwordHash());
        data.setCreatedAt(user.createdAt());
        data.setUpdatedAt(user.updatedAt());
        return data;
    }
}
