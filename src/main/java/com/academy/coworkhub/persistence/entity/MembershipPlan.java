package com.academy.coworkhub.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "membership_plans")
@Getter
@Setter
@NoArgsConstructor
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private int includedHoursPerMonth;

    /** Porcentaje (0-100) de descuento sobre las horas excedentes. */
    @Column(nullable = false)
    private int overageDiscount;

    @Column(nullable = false)
    private int maxActiveBookings;
}
