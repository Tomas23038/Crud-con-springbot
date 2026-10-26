package com.javeriana.zoo_fantastico.service;

import com.javeriana.zoo_fantastico.exception.ResourceNotFoEx;
import com.javeriana.zoo_fantastico.model.Creature;
import com.javeriana.zoo_fantastico.model.Zone;
import com.javeriana.zoo_fantastico.repository.CreatureRepository;
import com.javeriana.zoo_fantastico.repository.ZoneRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreatureServiceTest {

    private CreatureRepository creatureRepository;
    private ZoneRepository zoneRepository;
    private CreatureService creatureService;

    @BeforeEach
    void setUp() {
        creatureRepository = mock(CreatureRepository.class);
        zoneRepository = mock(ZoneRepository.class);
        creatureService = new CreatureService(creatureRepository, zoneRepository);
    }

    private Zone newZone(Long id, int capacity) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setCapacity(capacity);
        return zone;
    }

    private Creature newCreature(String name, Zone zone) {
        Creature creature = new Creature();
        creature.setName(name);
        creature.setHealthStatus("healthy");
        creature.setZone(zone);
        return creature;
    }

    // 1. Crear una criatura correctamente
    @Test
    void createCreature_success() {
        Zone zone = newZone(1L, 5);
        Creature creature = newCreature("Dragon", zone);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(0L);
        when(creatureRepository.save(creature)).thenReturn(creature);

        Creature result = creatureService.createCreature(creature);

        assertEquals("Dragon", result.getName());
        assertEquals(1L, result.getZone().getId());
    }

    // 2. No permitir crear criaturas si la zona está llena
    @Test
    void createCreature_zoneFull() {
        Zone zone = newZone(1L, 2);
        Creature creature = newCreature("Dragon", zone);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> creatureService.createCreature(creature));
    }

    // 3. No permitir crear una criatura sin zona
    @Test
    void createCreature_withoutZone() {
        Creature creature = newCreature("Dragon", null);

        assertThrows(ResourceNotFoEx.class, () -> creatureService.createCreature(creature));
    }

    // 4. Buscar una criatura existente por ID
    @Test
    void getCreatureById_success() {
        Creature creature = newCreature("Fenix", newZone(1L, 5));

        when(creatureRepository.findById(1L)).thenReturn(Optional.of(creature));

        Creature result = creatureService.getCreatureById(1L);

        assertEquals("Fenix", result.getName());
    }

    // 5. Buscar una criatura que no existe
    @Test
    void getCreatureById_notFound() {
        when(creatureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoEx.class, () -> creatureService.getCreatureById(99L));
    }

    // 6. Actualizar una criatura correctamente
    @Test
    void updateCreature_success() {
        Zone zone = newZone(2L, 5);
        Creature creature = newCreature("Dragon", zone);
        Creature updated = newCreature("Dragon Rojo", zone);

        when(creatureRepository.findById(1L)).thenReturn(Optional.of(creature));
        when(zoneRepository.findById(2L)).thenReturn(Optional.of(zone));
        when(creatureRepository.save(creature)).thenReturn(creature);

        Creature result = creatureService.updateCreature(1L, updated);

        assertEquals("Dragon Rojo", result.getName());
        assertEquals(2L, result.getZone().getId());
    }

    // 7. Impedir cambiar una criatura a una zona llena
    @Test
    void updateCreature_newZoneFull() {
        Creature creature = newCreature("Dragon", newZone(1L, 5));
        Zone fullZone = newZone(2L, 1);
        Creature updated = newCreature("Dragon", fullZone);

        when(creatureRepository.findById(1L)).thenReturn(Optional.of(creature));
        when(zoneRepository.findById(2L)).thenReturn(Optional.of(fullZone));
        when(creatureRepository.countByZoneId(2L)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> creatureService.updateCreature(1L, updated));
    }

    // 8. Eliminar una criatura saludable
    @Test
    void deleteCreature_success() {
        Creature creature = newCreature("Dragon", newZone(1L, 5));

        when(creatureRepository.findById(1L)).thenReturn(Optional.of(creature));

        assertDoesNotThrow(() -> creatureService.deleteCreature(1L));
        verify(creatureRepository).delete(creature);
    }

    // 9. Impedir eliminar una criatura crítica
    @Test
    void deleteCreature_critical() {
        Creature creature = newCreature("Dragon", newZone(1L, 5));
        creature.setHealthStatus("critical");

        when(creatureRepository.findById(1L)).thenReturn(Optional.of(creature));

        assertThrows(IllegalStateException.class, () -> creatureService.deleteCreature(1L));
    }
}
