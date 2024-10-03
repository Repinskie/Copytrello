package com.repinsky.task_tracker_backend.repositories;

import com.repinsky.task_tracker_backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository  extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    @Query("delete from User u where u.id =: id")
    void deleteById(Long id);
}
