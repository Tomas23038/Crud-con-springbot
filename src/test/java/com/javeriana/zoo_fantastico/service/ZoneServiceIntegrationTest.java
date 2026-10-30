package com.javeriana.zoo_fantastico.service;

import com.javeriana.zoo_fantastico.dto.zoneresponse;
import com.javeriana.zoo_fantastico.model.Creature;
import com.javeriana.zoo_fantastico.model.Zone;
import com.javeriana.zoo_fantastico.repository.CreatureRepository;
import com.javeriana.zoo_fantastico.repository.ZoneRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de {@link ZoneService} con la base de datos H2.
 *
 * <p>Siguen la misma idea que {@code CreatureServiceIntegrationTest}: sin mocks, con la
 * aplicación de Spring completa y una base de datos H2 en memoria. Revisan que las zonas
 * de verdad se guarden, se actualicen y se borren, y que las reglas de negocio (capacidad
 * mínima y no borrar zonas ocupadas) no dejen cambios indebidos en la base de datos.</p>
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ZoneServiceIntegrationTest {

    /**
     * El servicio real de zonas, creado por Spring y conectado a H2. Es el que se prueba.
     */
    @Autowired
    private ZoneService zoneService;

    /**
     * El repositorio real de zonas, conectado a H2.
     *
     * <p>Se usa para consultar directamente la base de datos después de llamar al servicio
     * y comprobar que los cambios quedaron guardados.</p>
     */
    @Autowired
    private ZoneRepository zoneRepository;

    /**
     * El repositorio real de criaturas, conectado a H2.
     *
     * <p>Se usa para guardar criaturas dentro de una zona y así probar las reglas que dependen
     * de cuántas criaturas tiene la zona.</p>
     */
    @Autowired
    private CreatureRepository creatureRepository;

    /**
     * Método de ayuda que crea una zona con los datos indicados, sin guardarla.
     *
     * @param name     nombre de la zona
     * @param capacity número máximo de criaturas que caben en la zona
     * @return una zona nueva y válida
     */
    private Zone newZone(String name, int capacity) {
        Zone zone = new Zone();
        zone.setName(name);
        zone.setCapacity(capacity);
        return zone;
    }

    /**
     * Método de ayuda que guarda directamente en H2 una criatura dentro de la zona indicada.
     *
     * <p>Se guarda con el repositorio (no con el servicio) porque aquí solo interesa que la zona
     * tenga criaturas para probar las reglas de {@code ZoneService}.</p>
     *
     * @param zone zona donde se guarda la criatura
     */
    private void saveCreatureIn(Zone zone) {
        Creature creature = new Creature();
        creature.setName("Dragon");
        creature.setSpecies("Reptil");
        creature.setDangerLevel(5);
        creature.setHealthStatus("healthy");
        creature.setZone(zone);
        creatureRepository.save(creature);
    }

    /**
     * 1. Crear una zona y verificar que se guardó en la base de datos.
     *
     * <p>Crea la zona "Bosque" con el servicio y la busca en la base de datos con el ID que
     * devolvió el servicio. Debe existir con el mismo nombre.</p>
     */
    @Test
    void testCreateZone_ShouldPersistInDatabase() {
        zoneresponse result = zoneService.createZone(newZone("Bosque", 5));

        Optional<Zone> foundZone = zoneRepository.findById(result.id());

        assertTrue(foundZone.isPresent());
        assertEquals("Bosque", foundZone.get().getName());
    }

    /**
     * 2. Buscar una zona y obtener el número real de criaturas.
     *
     * <p>Guarda una zona con 2 criaturas y la pide al servicio. La respuesta debe decir que
     * la zona tiene 2 criaturas, contadas desde la base de datos.</p>
     */
    @Test
    void testGetZoneById_ShouldCountCreaturesFromDatabase() {
        Zone zone = zoneRepository.save(newZone("Bosque", 5));
        saveCreatureIn(zone);
        saveCreatureIn(zone);

        zoneresponse result = zoneService.getZoneById(zone.getId());

        assertEquals(2, result.creatureCount());
    }

    /**
     * 3. Actualizar una zona y verificar los cambios en la base de datos.
     *
     * <p>Guarda la zona "Bosque" con capacidad 3 y la actualiza a "Bosque Encantado" con
     * capacidad 10. En la base de datos deben quedar los datos nuevos.</p>
     */
    @Test
    void testUpdateZone_ShouldUpdateInDatabase() {
        Zone zone = zoneRepository.save(newZone("Bosque", 3));

        zoneService.updateZone(zone.getId(), newZone("Bosque Encantado", 10));
        Zone foundZone = zoneRepository.findById(zone.getId()).orElseThrow();

        assertEquals("Bosque Encantado", foundZone.getName());
        assertEquals(10, foundZone.getCapacity());
    }

    /**
     * 4. No bajar la capacidad por debajo de las criaturas que ya hay.
     *
     * <p>Guarda una zona con capacidad 5 y 3 criaturas, e intenta bajar su capacidad a 1.
     * El servicio debe lanzar {@code IllegalStateException}, y en la base de datos la capacidad
     * debe seguir siendo 5.</p>
     */
    @Test
    void testUpdateZone_InsufficientCapacity_ShouldNotChangeDatabase() {
        Zone zone = zoneRepository.save(newZone("Bosque", 5));
        saveCreatureIn(zone);
        saveCreatureIn(zone);
        saveCreatureIn(zone);

        assertThrows(IllegalStateException.class,
                () -> zoneService.updateZone(zone.getId(), newZone("Bosque", 1)));
        assertEquals(5, zoneRepository.findById(zone.getId()).orElseThrow().getCapacity());
    }

    /**
     * 5. Eliminar una zona vacía y confirmar que ya no existe en la base de datos.
     */
    @Test
    void testDeleteZone_ShouldRemoveFromDatabase() {
        Zone zone = zoneRepository.save(newZone("Bosque", 5));

        zoneService.deleteZone(zone.getId());

        assertFalse(zoneRepository.findById(zone.getId()).isPresent());
    }

    /**
     * 6. No eliminar una zona que tiene criaturas.
     *
     * <p>Guarda una zona con una criatura e intenta borrarla. El servicio debe lanzar
     * {@code IllegalStateException}, y la zona debe seguir en la base de datos.</p>
     */
    @Test
    void testDeleteZone_WithCreatures_ShouldStayInDatabase() {
        Zone zone = zoneRepository.save(newZone("Bosque", 5));
        saveCreatureIn(zone);

        assertThrows(IllegalStateException.class, () -> zoneService.deleteZone(zone.getId()));
        assertTrue(zoneRepository.findById(zone.getId()).isPresent());
    }
}
