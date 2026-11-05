package org.acme.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity 
@Data 
public class ClimbingRoute {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key column "gym_id" -> climbinggym.id
    @ManyToOne(optional = false)
    @JoinColumn(name = "gym_id", nullable = false)
    private ClimbingGym gym;

    // Foreign key column "owner_id" -> climber.id: the user who created the route.
    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Climber owner;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private String color; // color in hex

    @Column(nullable = false)
    private boolean active;
}
