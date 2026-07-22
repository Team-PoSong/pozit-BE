package com.pozit.pozitserver.tag.repository;

import com.pozit.pozitserver.tag.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
