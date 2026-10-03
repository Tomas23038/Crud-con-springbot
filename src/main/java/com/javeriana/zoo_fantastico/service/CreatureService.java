package com.javeriana.zoo_fantastico.service;

import com.javeriana.zoo_fantastico.exception.ResourceNotFoEx;
import com.javeriana.zoo_fantastico.model.Creature;
import com.javeriana.zoo_fantastico.repository.CreatureRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreatureService {

    private final CreatureRepository creatureRepository;

    public CreatureService(CreatureRepository creatureRepository) {
        this.creatureRepository = creatureRepository;
    }

    public Creature createCreature(Creature creature) {
        return creatureRepository.save(creature);
    }

    public List<Creature> getAllCreatures() {
        return creatureRepository.findAll();
    }

    public Creature getCreatureById(Long id) {
        return creatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoEx("Creature not found"));
    }

    public Creature updateCreature(Long id, Creature updatedCreature) {
        Creature creature = getCreatureById(id);
        creature.setName(updatedCreature.getName());
        creature.setSpecies(updatedCreature.getSpecies());
        creature.setSize(updatedCreature.getSize());
        creature.setDangerLevel(updatedCreature.getDangerLevel());
        creature.setHealthStatus(updatedCreature.getHealthStatus());
        return creatureRepository.save(creature);
    }

    public void deleteCreature(Long id) {
        Creature creature = getCreatureById(id);
    if (!"critical".equals(creature.getHealthStatus())) {
        creatureRepository.delete(creature);
    }else{
        throw new IllegalStateException(
            "Cannot delete a creature in critical health");
    }
    }
}