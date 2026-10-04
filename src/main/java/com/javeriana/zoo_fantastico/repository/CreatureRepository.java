package com.javeriana.zoo_fantastico.repository;

import com.javeriana.zoo_fantastico.model.Creature;
import com.javeriana.zoo_fantastico.dto.zonecount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CreatureRepository extends JpaRepository<Creature, Long> {
    long countByZoneId(Long zoneId);

    @Override
    @EntityGraph(attributePaths="zone")
    List<Creature>findAll();
    @Query("select c.zone.id as zoneId, count(c) as total from Creature c group by c.zone.id")
    List<zonecount> countCreaturesByZone();
}