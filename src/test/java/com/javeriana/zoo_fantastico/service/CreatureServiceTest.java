
package com.javeriana.zoo_fantastico.service;

import com.javeriana.zoo_fantastico.exception.ResourceNotFoEx;
import com.javeriana.zoo_fantastico.model.Creature;
import com.javeriana.zoo_fantastico.model.Zone;
import com.javeriana.zoo_fantastico.repository.CreatureRepository;
import com.javeriana.zoo_fantastico.repository.ZoneRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatureServiceTest {

    @Mock
    private CreatureRepository creatureRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private CreatureService creatureService;

    // 1. Crear una criatura correctamente
    @Test
    void createCreature_success() {
        Creature creature = mock(Creature.class);
        Zone zone = mock(Zone.class);

        when(creature.getZone()).thenReturn(zone);
        when(zone.getId()).thenReturn(1L);
        when(zone.getCapacity()).thenReturn(5);
        when(zoneRepository.findById(1L))
                .thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L))
                .thenReturn(0L);
        when(creatureRepository.save(creature))
                .thenReturn(creature);

        Creature result = creatureService.createCreature(creature);

        assertSame(creature, result);
        verify(creatureRepository).save(creature);
        verify(creature).setZone(zone);
    }

    // 2. No permitir crear criaturas si la zona está llena
    @Test
    void createCreature_zoneFull() {
        Creature creature = mock(Creature.class);
        Zone zone = mock(Zone.class);

        when(creature.getZone()).thenReturn(zone);
        when(zone.getId()).thenReturn(1L);
        when(zone.getCapacity()).thenReturn(2);
        when(zoneRepository.findById(1L))
                .thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L))
                .thenReturn(2L);

        assertThrows(IllegalStateException.class,
                () -> creatureService.createCreature(creature));

        verify(creatureRepository, never()).save(any());
    }

    // 3. No permitir crear una criatura sin zona
    @Test
    void createCreature_withoutZone() {
        Creature creature = mock(Creature.class);

        assertThrows(ResourceNotFoEx.class,
                () -> creatureService.createCreature(creature));

        verify(creatureRepository, never()).save(any());
    }

    // 4. Buscar una criatura existente por ID
    @Test
    void getCreatureById_success() {
        Creature creature = mock(Creature.class);

        when(creatureRepository.findById(1L))
                .thenReturn(Optional.of(creature));

        Creature result = creatureService.getCreatureById(1L);

        assertSame(creature, result);
    }

    // 5. Buscar una criatura que no existe
    @Test
    void getCreatureById_notFound() {
        when(creatureRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoEx.class,
                () -> creatureService.getCreatureById(99L));
    }

    // 6. Actualizar una criatura correctamente
    @Test
    void updateCreature_success() {
        Creature creature = mock(Creature.class);
        Creature updated = mock(Creature.class);
        Zone zone = mock(Zone.class);

        when(creatureRepository.findById(1L))
                .thenReturn(Optional.of(creature));
        when(updated.getZone()).thenReturn(zone);
        when(creature.getZone()).thenReturn(zone);
        when(zone.getId()).thenReturn(2L);
        when(zoneRepository.findById(2L))
                .thenReturn(Optional.of(zone));
        when(creatureRepository.save(creature))
                .thenReturn(creature);

        Creature result = creatureService.updateCreature(1L, updated);

        assertSame(creature, result);
        verify(creature).setZone(zone);
        verify(creatureRepository).save(creature);
    }

    // 7. Impedir cambiar una criatura a una zona llena
    @Test
    void updateCreature_newZoneFull() {
        Creature creature = mock(Creature.class);
        Creature updated = mock(Creature.class);
        Zone newZone = mock(Zone.class);

        when(creatureRepository.findById(1L))
                .thenReturn(Optional.of(creature));
        when(updated.getZone()).thenReturn(newZone);
        when(newZone.getId()).thenReturn(2L);
        when(newZone.getCapacity()).thenReturn(1);
        when(zoneRepository.findById(2L))
                .thenReturn(Optional.of(newZone));
        when(creatureRepository.countByZoneId(2L))
                .thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> creatureService.updateCreature(1L, updated));

        verify(creatureRepository, never()).save(any());
    }

    // 8. Eliminar una criatura saludable
    @Test
    void deleteCreature_success() {
        Creature creature = mock(Creature.class);

        when(creatureRepository.findById(1L))
                .thenReturn(Optional.of(creature));
        when(creature.getHealthStatus()).thenReturn("healthy");

        creatureService.deleteCreature(1L);

        verify(creatureRepository).delete(creature);
    }

    // 9. Impedir eliminar una criatura crítica
    @Test
    void deleteCreature_critical() {
        Creature creature = mock(Creature.class);

        when(creatureRepository.findById(1L))
                .thenReturn(Optional.of(creature));
        when(creature.getHealthStatus()).thenReturn("critical");

        assertThrows(IllegalStateException.class,
                () -> creatureService.deleteCreature(1L));

        verify(creatureRepository, never()).delete(any());
    }
}
