package com.base.repo;

import com.base.model.DmsDoc;
import com.base.model.DmsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DmsFileRepository extends JpaRepository<DmsFile, Long> {
}
