package com.javeriana.zoo_fantastico.service;

import com.javeriana.zoo_fantastico.dto.zonecount;
import com.javeriana.zoo_fantastico.dto.zoneresponse;
import com.javeriana.zoo_fantastico.exception.ResourceNotFoEx;
import com.javeriana.zoo_fantastico.model.Zone;
import com.javeriana.zoo_fantastico.repository.CreatureRepository;
import com.javeriana.zoo_fantastico.repository.ZoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final CreatureRepository creatureRepository;

    public ZoneService(ZoneRepository zoneRepository, CreatureRepository creatureRepository) {
        this.zoneRepository = zoneRepository;
        this.creatureRepository = creatureRepository;
    }

    public zoneresponse createZone(Zone zone) {
        return toResponse(zoneRepository.save(zone), 0);
    }

    public List<zoneresponse> getAllZones() {
        Map<Long, Long> counts = creatureRepository.countCreaturesByZone().stream()
                .collect(Collectors.toMap(zonecount::getZoneId, zonecount::getTotal));
        return zoneRepository.findAll().stream()
                .map(zone -> toResponse(zone, counts.getOrDefault(zone.getId(), 0L)))
                .toList();
    }

    public zoneresponse getZoneById(Long id) {
        Zone zone = findZone(id);
        return toResponse(zone, creatureRepository.countByZoneId(id));
    }

    public zoneresponse updateZone(Long id, Zone updatedZone) {
        Zone zone = findZone(id);
        long currentCount = creatureRepository.countByZoneId(id);
        if (updatedZone.getCapacity() < currentCount) {
            throw new IllegalStateException(
                    "Capacity cannot be lower than the current number of creatures in the zone");
        }
        zone.setName(updatedZone.getName());
        zone.setDescription(updatedZone.getDescription());
        zone.setCapacity(updatedZone.getCapacity());
        return toResponse(zoneRepository.save(zone), currentCount);
    }

    public void deleteZone(Long id) {
        Zone zone = findZone(id);
        if (creatureRepository.countByZoneId(id) > 0) {
            throw new IllegalStateException("Cannot delete a zone that has creatures assigned");
        }
        zoneRepository.delete(zone);
    }

    private Zone findZone(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoEx("Zone not found"));
    }

    private zoneresponse toResponse(Zone zone, long creatureCount) {
        return new zoneresponse(zone.getId(), zone.getName(), zone.getDescription(),
                zone.getCapacity(), creatureCount);
    }
}