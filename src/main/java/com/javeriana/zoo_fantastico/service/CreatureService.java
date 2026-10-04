package com.javeriana.zoo_fantastico.service;

import com.javeriana.zoo_fantastico.exception.ResourceNotFoEx;
import com.javeriana.zoo_fantastico.model.Creature;
import com.javeriana.zoo_fantastico.model.Zone;
import com.javeriana.zoo_fantastico.repository.CreatureRepository;
import com.javeriana.zoo_fantastico.repository.ZoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreatureService {

    private final CreatureRepository creatureRepository;
    private final ZoneRepository zoneRepository;

    public CreatureService(CreatureRepository creatureRepository, ZoneRepository zoneRepository) {
        this.creatureRepository = creatureRepository;
        this.zoneRepository = zoneRepository;
    }

    public Creature createCreature(Creature creature) {
        Zone zone = findZone(creature.getZone());
        validateCapacity(zone);
        creature.setZone(zone);
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
        Zone newZone = findZone(updatedCreature.getZone());

        boolean changesZone = creature.getZone() == null
                || !creature.getZone().getId().equals(newZone.getId());
        if (changesZone) {
            validateCapacity(newZone);
        }

        creature.setName(updatedCreature.getName());
        creature.setSpecies(updatedCreature.getSpecies());
        creature.setSize(updatedCreature.getSize());
        creature.setDangerLevel(updatedCreature.getDangerLevel());
        creature.setHealthStatus(updatedCreature.getHealthStatus());
        creature.setZone(newZone);
        return creatureRepository.save(creature);
    }

    public void deleteCreature(Long id) {
        Creature creature = getCreatureById(id);
        if ("critical".equalsIgnoreCase(creature.getHealthStatus())) {
            throw new IllegalStateException("Cannot delete a creature in critical health");
        }
        creatureRepository.delete(creature);
    }

    private Zone findZone(Zone zoneReference) {
        if (zoneReference == null || zoneReference.getId() == null) {
            throw new ResourceNotFoEx("Zone not found");
        }
        return zoneRepository.findById(zoneReference.getId())
                .orElseThrow(() -> new ResourceNotFoEx("Zone not found"));
    }

    private void validateCapacity(Zone zone) {
        if (creatureRepository.countByZoneId(zone.getId()) >= zone.getCapacity()) {
            throw new IllegalStateException("The zone has reached its maximum capacity");
        }
    }
}