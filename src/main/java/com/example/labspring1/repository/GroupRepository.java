package com.example.labspring1.repository;

import com.example.labspring1.model.Group;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @EntityGraph(attributePaths = {"schedules"})
    Optional<Group> findByGroupNumber(String groupNumber);
}