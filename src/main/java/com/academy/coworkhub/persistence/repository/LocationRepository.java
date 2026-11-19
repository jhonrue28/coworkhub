package com.academy.coworkhub.persistence.repository;

import com.academy.coworkhub.persistence.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
