package com.javeriana.zoo_fantastico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Creature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String species;

    @Min(0)
    private double size;

    @Min(1)
    @Max(10)
    private int dangerLevel;

    @NotBlank
    private String healthStatus;

    @NotNull
    @ManyToOne
    @JoinColumn(name="zone_id")
    private Zone zone;
}