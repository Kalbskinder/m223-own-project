package org.acme.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import org.acme.enums.ClimbResult;

@Entity
@Data 
public class Climb {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key column "route_id" -> climbingroute.id
    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private ClimbingRoute route;

    // Foreign key column "owner_id" -> climber.id: the user who logged the climb.
    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Climber owner;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private ClimbResult result;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private String note;
}
