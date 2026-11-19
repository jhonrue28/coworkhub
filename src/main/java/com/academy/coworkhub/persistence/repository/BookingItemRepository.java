package com.academy.coworkhub.persistence.repository;

import com.academy.coworkhub.persistence.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    // RN-12: un servicio usado en alguna reserva no se elimina
    boolean existsByServiceId(Long serviceId);
}
