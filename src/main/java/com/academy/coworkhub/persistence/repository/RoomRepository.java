package com.academy.coworkhub.persistence.repository;

import com.academy.coworkhub.persistence.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // Sede y equipamiento en la misma consulta: sin N+1 en los listados
    @Override
    @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findAll();

    @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findByLocationId(Long locationId);

    boolean existsByLocationId(Long locationId);

    boolean existsByLocationIdAndNameIgnoreCase(Long locationId, String name);

    boolean existsByLocationIdAndNameIgnoreCaseAndIdNot(Long locationId, String name, Long id);

    boolean existsByEquipment_Id(Long equipmentId);

    // RN-01: bloquea la fila de la sala hasta el commit, así dos reservas simultáneas no ven el mismo hueco
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
