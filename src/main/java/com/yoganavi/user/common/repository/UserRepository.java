package com.yoganavi.user.common.repository;

import com.yoganavi.user.common.entity.Users;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByEmail(String email);

    List<Users> findByDeletedAtBeforeAndIsDeletedFalse(Instant dateTime);

}
