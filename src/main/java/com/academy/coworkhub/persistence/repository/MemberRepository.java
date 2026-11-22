package com.academy.coworkhub.persistence.repository;

import com.academy.coworkhub.persistence.entity.Member;
import com.academy.coworkhub.persistence.entity.MemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // Filtros opcionales: un parámetro null no filtra. El plan viene en la misma consulta (sin N+1)
    @EntityGraph(attributePaths = "plan")
    @Query("""
            select m from Member m
            where (:name is null or lower(m.fullName) like lower(concat('%', :name, '%')))
              and (:status is null or m.status = :status)
            order by m.fullName
            """)
    List<Member> search(@Param("name") String name, @Param("status") MemberStatus status);

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPlanId(Long planId);

    // RN-06: serializa las reservas simultáneas del mismo miembro
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
}
