package com.base.repo;

import com.base.model.DmsDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface DmsDocRepository extends JpaRepository<DmsDoc, UUID> {
}
