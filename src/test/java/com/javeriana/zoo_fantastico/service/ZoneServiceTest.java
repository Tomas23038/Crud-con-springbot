
package com.javeriana.zoo_fantastico.service;

import com.javeriana.zoo_fantastico.dto.zoneresponse;
import com.javeriana.zoo_fantastico.exception.ResourceNotFoEx;
import com.javeriana.zoo_fantastico.model.Zone;
import com.javeriana.zoo_fantastico.repository.CreatureRepository;
import com.javeriana.zoo_fantastico.repository.ZoneRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private CreatureRepository creatureRepository;

    @InjectMocks
    private ZoneService zoneService;

    // 1. Crear una zona correctamente
    @Test
    void createZone_success() {
        Zone zone = mock(Zone.class);

        when(zoneRepository.save(zone)).thenReturn(zone);

        zoneresponse result = zoneService.createZone(zone);

        assertNotNull(result);
        verify(zoneRepository).save(zone);
    }

    // 2. Consultar todas las zonas
    @Test
    void getAllZones_success() {
        Zone zone = mock(Zone.class);

        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(creatureRepository.countCreaturesByZone())
                .thenReturn(List.of());

        List<zoneresponse> result = zoneService.getAllZones();

        assertEquals(1, result.size());
        verify(zoneRepository).findAll();
    }

    // 3. Buscar una zona existente
    @Test
    void getZoneById_success() {
        Zone zone = mock(Zone.class);

        when(zoneRepository.findById(1L))
                .thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L))
                .thenReturn(2L);

        zoneresponse result = zoneService.getZoneById(1L);

        assertNotNull(result);
        verify(creatureRepository).countByZoneId(1L);
    }

    // 4. Buscar una zona inexistente
    @Test
    void getZoneById_notFound() {
        when(zoneRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoEx.class,
                () -> zoneService.getZoneById(99L));
    }

    // 5. Actualizar una zona correctamente
    @Test
    void updateZone_success() {
        Zone zone = mock(Zone.class);
        Zone updated = mock(Zone.class);

        when(zoneRepository.findById(1L))
                .thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L))
                .thenReturn(2L);
        when(updated.getCapacity()).thenReturn(5);
        when(zoneRepository.save(zone)).thenReturn(zone);

        zoneresponse result = zoneService.updateZone(1L, updated);

        assertNotNull(result);
        verify(zone).setCapacity(5);
        verify(zoneRepository).save(zone);
    }

    // 6. Impedir reducir capacidad por debajo de las criaturas existentes
    @Test
    void updateZone_insufficientCapacity() {
        Zone zone = mock(Zone.class);
        Zone updated = mock(Zone.class);

        when(zoneRepository.findById(1L))
                .thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L))
                .thenReturn(5L);
        when(updated.getCapacity()).thenReturn(2);

        assertThrows(IllegalStateException.class,
                () -> zoneService.updateZone(1L, updated));

        verify(zoneRepository, never()).save(any());
    }

    // 7. Eliminar una zona vacía
    @Test
    void deleteZone_success() {
        Zone zone = mock(Zone.class);

        when(zoneRepository.findById(1L))
                .thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L))
                .thenReturn(0L);

        zoneService.deleteZone(1L);

        verify(zoneRepository).delete(zone);
    }

    // 8. Impedir eliminar una zona ocupada
    @Test
    void deleteZone_withCreatures() {
        Zone zone = mock(Zone.class);

        when(zoneRepository.findById(1L))
                .thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L))
                .thenReturn(3L);

        assertThrows(IllegalStateException.class,
                () -> zoneService.deleteZone(1L));

        verify(zoneRepository, never()).delete(any());
    }
}
