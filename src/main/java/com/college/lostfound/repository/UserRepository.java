package com.college.lostfound.repository;

import com.college.lostfound.model.User;
import com.college.lostfound.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByCollegeId(String collegeId);
    boolean existsByEmail(String email);
    boolean existsByCollegeId(String collegeId);
    List<User> findByRole(Role role);
}
