package com.javeriana.zoo_fantastico.repository;

import com.javeriana.zoo_fantastico.model.Creature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatureRepository extends JpaRepository<Creature, Long> {
}