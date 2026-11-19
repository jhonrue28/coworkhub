package com.academy.coworkhub.persistence.repository;

import com.academy.coworkhub.persistence.entity.Room;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // Sede y equipamiento en la misma consulta: sin N+1 en los listados
    @Override
    @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findAll();

    @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findByLocationId(Long locationId);

    boolean existsByLocationId(Long locationId);

    boolean existsByLocationIdAndNameIgnoreCase(Long locationId, String name);

    boolean existsByEquipment_Id(Long equipmentId);
}
