package com.pydlove.frametalk.identity.domain.repository;

import com.pydlove.frametalk.identity.domain.model.Email;
import com.pydlove.frametalk.identity.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(long id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    User save(User user);
}
