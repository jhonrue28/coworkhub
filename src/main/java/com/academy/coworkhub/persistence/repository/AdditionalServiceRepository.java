package com.academy.coworkhub.persistence.repository;

import com.academy.coworkhub.persistence.entity.AdditionalService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {

    boolean existsByNameIgnoreCase(String name);
}
