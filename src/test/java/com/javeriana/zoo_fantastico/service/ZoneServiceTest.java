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

/**
 * Pruebas unitarias del servicio {@link ZoneService}.
 *
 * <p>{@code ZoneService} tiene las reglas de negocio de las zonas del zoológico: crear, listar,
 * buscar, actualizar y eliminar (CRUD). También impide bajar la capacidad de una zona por
 * debajo del número de criaturas que ya tiene y no deja borrar zonas ocupadas.</p>
 *
 * <p>Igual que en {@code CreatureServiceTest}, los repositorios se reemplazan por <b>mocks de
 * Mockito</b> para probar el servicio sin MySQL. Las zonas son objetos reales
 * ({@code new Zone()}).</p>
 */
class ZoneServiceTest {

    /**
     * Repositorio de zonas simulado con Mockito.
     *
     * <p>En la aplicación real guarda y consulta zonas en MySQL. En las pruebas solo devuelve
     * lo que cada prueba le indique, por ejemplo qué zona tiene cierto ID o la lista de zonas.
     * También se usa con {@code verify(...)} para comprobar que el servicio llamó a {@code delete}.</p>
     */
    private ZoneRepository zoneRepository;

    /**
     * Repositorio de criaturas simulado con Mockito.
     *
     * <p>El servicio de zonas lo usa para saber cuántas criaturas hay en cada zona. Ese número
     * se muestra en la respuesta y sirve para decidir si se puede bajar la capacidad o borrar
     * la zona. En las pruebas se le indica qué número devolver.</p>
     */
    private CreatureRepository creatureRepository;

    /**
     * El servicio real que se está probando.
     *
     * <p>Es el {@code ZoneService} de verdad, con su lógica completa. Solo los repositorios que
     * recibe en el constructor son falsos.</p>
     */
    private ZoneService zoneService;

    /**
     * Prepara todo lo necesario antes de cada prueba.
     *
     * <p>Con {@code @BeforeEach}, JUnit ejecuta este método antes de <b>cada</b> prueba. Crea mocks
     * nuevos de los repositorios y un servicio nuevo, para que cada prueba empiece desde cero y no
     * herede lo que configuró otra.</p>
     */
    @BeforeEach
    void setUp() {
        zoneRepository = mock(ZoneRepository.class);
        creatureRepository = mock(CreatureRepository.class);
        zoneService = new ZoneService(zoneRepository, creatureRepository);
    }

    /**
     * Método de ayuda que crea una zona real con los datos indicados.
     *
     * <p>Sirve para no repetir {@code new Zone()} y los setters en cada prueba.</p>
     *
     * @param id       identificador de la zona (puede ser {@code null} para los datos de una actualización)
     * @param name     nombre de la zona
     * @param capacity número máximo de criaturas que caben en la zona
     * @return una zona nueva con esos datos
     */
    private Zone newZone(Long id, String name, int capacity) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setName(name);
        zone.setCapacity(capacity);
        return zone;
    }

    /**
     * 1. Crear una zona correctamente.
     *
     * <p>Simula que el repositorio guarda la zona "Bosque". El servicio debe devolver una respuesta
     * con ese nombre y 0 criaturas, porque una zona recién creada está vacía.</p>
     *
     * <p>Sirve para comprobar el caso normal de {@code createZone}.</p>
     */
    @Test
    void createZone_success() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.save(zone)).thenReturn(zone);

        zoneresponse result = zoneService.createZone(zone);

        assertNotNull(result);
        assertEquals("Bosque", result.name());
        assertEquals(0, result.creatureCount());
    }

    /**
     * 2. Consultar todas las zonas.
     *
     * <p>Simula que en el repositorio hay una sola zona, "Bosque". El servicio debe devolver una
     * lista con exactamente esa zona.</p>
     *
     * <p>Sirve para comprobar que {@code getAllZones} convierte bien las zonas en respuestas.</p>
     */
    @Test
    void getAllZones_success() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(creatureRepository.countCreaturesByZone()).thenReturn(List.of());

        List<zoneresponse> result = zoneService.getAllZones();

        assertEquals(1, result.size());
        assertEquals("Bosque", result.get(0).name());
    }

    /**
     * 3. Buscar una zona existente por ID.
     *
     * <p>Simula que existe la zona "Bosque" con ID 1 y que tiene 2 criaturas. El servicio debe
     * devolver la zona con su nombre y con el número de criaturas correcto.</p>
     *
     * <p>Sirve para comprobar el caso normal de {@code getZoneById}.</p>
     */
    @Test
    void getZoneById_success() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(2L);

        zoneresponse result = zoneService.getZoneById(1L);

        assertEquals("Bosque", result.name());
        assertEquals(2, result.creatureCount());
    }

    /**
     * 4. Buscar una zona que no existe.
     *
     * <p>Simula que el repositorio no encuentra ninguna zona con ID 99. El servicio debe lanzar
     * {@code ResourceNotFoEx}, que en la API se convierte en un error 404.</p>
     *
     * <p>Sirve para comprobar que el servicio avisa con un error claro cuando la zona no existe.</p>
     */
    @Test
    void getZoneById_notFound() {
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoEx.class, () -> zoneService.getZoneById(99L));
    }

    /**
     * 5. Actualizar una zona correctamente.
     *
     * <p>La zona "Bosque" tiene capacidad 3 y 2 criaturas. Se actualiza con el nombre
     * "Bosque Encantado" y capacidad 5. Como 5 es mayor que 2, el cambio es válido. La prueba
     * revisa que la respuesta tenga el nombre y la capacidad nuevos.</p>
     *
     * <p>Sirve para comprobar que {@code updateZone} sí copia los datos nuevos antes de guardar.</p>
     */
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

    /**
     * 6. Impedir bajar la capacidad por debajo de las criaturas que ya hay.
     *
     * <p>La zona tiene 5 criaturas y se intenta bajar su capacidad a 2. El servicio debe lanzar
     * {@code IllegalStateException}, porque las criaturas actuales no cabrían.</p>
     *
     * <p>Sirve para comprobar la regla de negocio que protege la capacidad de las zonas.</p>
     */
    @Test
    void updateZone_insufficientCapacity() {
        Zone zone = newZone(1L, "Bosque", 10);
        Zone updated = newZone(null, "Bosque", 2);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(5L);

        assertThrows(IllegalStateException.class, () -> zoneService.updateZone(1L, updated));
    }

    /**
     * 7. Eliminar una zona vacía.
     *
     * <p>Simula una zona con 0 criaturas. El servicio debe borrarla sin errores. Como
     * {@code delete} no devuelve nada, se usa {@code verify} de Mockito para comprobar que el
     * servicio sí le pidió al repositorio borrar esa zona.</p>
     *
     * <p>Sirve para comprobar el caso normal de {@code deleteZone}.</p>
     */
    @Test
    void deleteZone_success() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(0L);

        assertDoesNotThrow(() -> zoneService.deleteZone(1L));
        verify(zoneRepository).delete(zone);
    }

    /**
     * 8. Impedir eliminar una zona que tiene criaturas.
     *
     * <p>Simula una zona con 3 criaturas. El servicio debe lanzar {@code IllegalStateException}
     * y no borrarla, para que las criaturas no se queden sin zona.</p>
     *
     * <p>Sirve para comprobar la regla de negocio que protege a las criaturas asignadas.</p>
     */
    @Test
    void deleteZone_withCreatures() {
        Zone zone = newZone(1L, "Bosque", 5);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(3L);

        assertThrows(IllegalStateException.class, () -> zoneService.deleteZone(1L));
    }
}
