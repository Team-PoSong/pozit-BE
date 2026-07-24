package com.pozit.pozitserver.tag.repository;

import com.pozit.pozitserver.tag.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findAll();
}
