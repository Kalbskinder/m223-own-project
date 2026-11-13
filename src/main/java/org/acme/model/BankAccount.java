package org.acme.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import lombok.Data;

@Data 
@Entity 
public class BankAccount {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double money;

    @Column(nullable = false)
    private double interestRate;

    @ManyToOne 
    @JoinColumn(nullable = false)
    private User accountOwner;
}
