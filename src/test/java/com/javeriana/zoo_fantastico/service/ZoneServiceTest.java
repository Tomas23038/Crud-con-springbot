package com.javeriana.zoo_fantastico.service;

import com.javeriana.zoo_fantastico.dto.zoneresponse;
import com.javeriana.zoo_fantastico.exception.ResourceNotFoEx;
import com.javeriana.zoo_fantastico.model.Zone;
import com.javeriana.zoo_fantastico.repository.CreatureRepository;
import com.javeriana.zoo_fantastico.repository.ZoneRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZoneServiceTest {

    private ZoneRepository zoneRepository;
    private CreatureRepository creatureRepository;
    private ZoneService zoneService;

    @BeforeEach
    void setUp() {
        zoneRepository = mock(ZoneRepository.class);
        creatureRepository = mock(CreatureRepository.class);
        zoneService = new ZoneService(zoneRepository, creatureRepository);
    }

    private Zone newZone(Long id, String name, int capacity) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setName(name);
        zone.setCapacity(capacity);
        return zone;
    }

    // 1. Crear una zona correctamente
    @Test
    void createZone_success() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.save(zone)).thenReturn(zone);

        zoneresponse result = zoneService.createZone(zone);

        assertNotNull(result);
        assertEquals("Bosque", result.name());
        assertEquals(0, result.creatureCount());
    }

    // 2. Consultar todas las zonas
    @Test
    void getAllZones_success() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(creatureRepository.countCreaturesByZone()).thenReturn(List.of());

        List<zoneresponse> result = zoneService.getAllZones();

        assertEquals(1, result.size());
        assertEquals("Bosque", result.get(0).name());
    }

    // 3. Buscar una zona existente
    @Test
    void getZoneById_success() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(2L);

        zoneresponse result = zoneService.getZoneById(1L);

        assertEquals("Bosque", result.name());
        assertEquals(2, result.creatureCount());
    }

    // 4. Buscar una zona inexistente
    @Test
    void getZoneById_notFound() {
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoEx.class, () -> zoneService.getZoneById(99L));
    }

    // 5. Actualizar una zona correctamente
    @Test
    void updateZone_success() {
        Zone zone = newZone(1L, "Bosque", 3);
        Zone updated = newZone(null, "Bosque Encantado", 5);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(2L);
        when(zoneRepository.save(zone)).thenReturn(zone);

        zoneresponse result = zoneService.updateZone(1L, updated);

        assertEquals("Bosque Encantado", result.name());
        assertEquals(5, result.capacity());
    }

    // 6. Impedir reducir capacidad por debajo de las criaturas existentes
    @Test
    void updateZone_insufficientCapacity() {
        Zone zone = newZone(1L, "Bosque", 10);
        Zone updated = newZone(null, "Bosque", 2);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(5L);

        assertThrows(IllegalStateException.class, () -> zoneService.updateZone(1L, updated));
    }

    // 7. Eliminar una zona vacía
    @Test
    void deleteZone_success() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(0L);

        assertDoesNotThrow(() -> zoneService.deleteZone(1L));
        verify(zoneRepository).delete(zone);
    }

    // 8. Impedir eliminar una zona ocupada
    @Test
    void deleteZone_withCreatures() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(3L);

        assertThrows(IllegalStateException.class, () -> zoneService.deleteZone(1L));
    }
}
