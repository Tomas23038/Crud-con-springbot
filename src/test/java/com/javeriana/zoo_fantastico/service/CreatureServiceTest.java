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

/**
 * Pruebas unitarias del servicio {@link CreatureService}.
 *
 * <p>Una prueba unitaria revisa una sola pieza del código de forma aislada. Aquí esa pieza es
 * {@code CreatureService}, que tiene las reglas de negocio de las criaturas: crear, buscar,
 * actualizar y eliminar (CRUD), además de validar la capacidad de las zonas y el estado de
 * salud antes de borrar.</p>
 *
 * <p>Para no depender de MySQL, los repositorios se reemplazan por <b>mocks de Mockito</b>:
 * objetos falsos a los que se les dice qué responder con {@code when(...).thenReturn(...)}.
 * Así cada prueba es rápida, no necesita base de datos y solo falla si la lógica del servicio
 * está mal. Las criaturas y zonas sí son objetos reales ({@code new Creature()}, {@code new Zone()}),
 * porque son datos simples y no hace falta simularlos.</p>
 */
class CreatureServiceTest {

    /**
     * Repositorio de criaturas simulado con Mockito.
     *
     * <p>En la aplicación real guarda y consulta criaturas en MySQL. En las pruebas es un mock:
     * no toca ninguna base de datos y solo devuelve lo que cada prueba le indique (por ejemplo,
     * cuántas criaturas hay en una zona o qué criatura tiene cierto ID). También sirve para
     * comprobar con {@code verify(...)} que el servicio sí llamó a un método, como {@code delete}.</p>
     */
    private CreatureRepository creatureRepository;

    /**
     * Repositorio de zonas simulado con Mockito.
     *
     * <p>El servicio de criaturas lo usa para buscar la zona a la que se asigna una criatura.
     * En las pruebas se le indica qué zona devolver para cada ID, sin necesidad de base de datos.</p>
     */
    private ZoneRepository zoneRepository;

    /**
     * El servicio real que se está probando.
     *
     * <p>No es un mock: es el {@code CreatureService} de verdad, con toda su lógica. Lo único
     * falso son los repositorios que recibe en el constructor. De esta forma se prueba el
     * comportamiento real del servicio sin depender de la base de datos.</p>
     */
    private CreatureService creatureService;

    /**
     * Prepara todo lo necesario antes de cada prueba.
     *
     * <p>Gracias a {@code @BeforeEach}, JUnit ejecuta este método antes de <b>cada</b> prueba.
     * Crea mocks nuevos de los dos repositorios y un servicio nuevo que los usa. Así cada prueba
     * empieza desde cero y lo que se configuró en una prueba no afecta a las demás.</p>
     */
    @BeforeEach
    void setUp() {
        creatureRepository = mock(CreatureRepository.class);
        zoneRepository = mock(ZoneRepository.class);
        creatureService = new CreatureService(creatureRepository, zoneRepository);
    }

    /**
     * Método de ayuda que crea una zona real con los datos indicados.
     *
     * <p>Sirve para no repetir las mismas líneas ({@code new Zone()}, {@code setId}, {@code setCapacity})
     * en cada prueba. Solo llena el ID y la capacidad, que son los datos que usa el servicio
     * para buscar la zona y validar si está llena.</p>
     *
     * @param id       identificador de la zona
     * @param capacity número máximo de criaturas que caben en la zona
     * @return una zona nueva con ese ID y esa capacidad
     */
    private Zone newZone(Long id, int capacity) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setCapacity(capacity);
        return zone;
    }

    /**
     * Método de ayuda que crea una criatura real con los datos indicados.
     *
     * <p>Le pone el nombre, la zona y el estado de salud {@code "healthy"} por defecto. Sirve para
     * no repetir código en cada prueba. Si una prueba necesita otro estado (por ejemplo
     * {@code "critical"}), lo cambia después con {@code setHealthStatus}.</p>
     *
     * @param name nombre de la criatura
     * @param zone zona a la que pertenece (puede ser {@code null} para probar una criatura sin zona)
     * @return una criatura nueva con esos datos
     */
    private Creature newCreature(String name, Zone zone) {
        Creature creature = new Creature();
        creature.setName(name);
        creature.setHealthStatus("healthy");
        creature.setZone(zone);
        return creature;
    }

    /**
     * 1. Crear una criatura correctamente.
     *
     * <p>Simula una zona con capacidad para 5 criaturas que todavía está vacía (0 criaturas).
     * Como hay espacio, el servicio debe guardar la criatura. La prueba revisa que la criatura
     * devuelta tenga el nombre "Dragon" y quede asignada a la zona con ID 1.</p>
     *
     * <p>Sirve para comprobar el caso normal de {@code createCreature}: buscar la zona, validar
     * que hay espacio, asignarla y guardar.</p>
     */
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

    /**
     * 2. No permitir crear criaturas si la zona está llena.
     *
     * <p>Simula una zona con capacidad para 2 criaturas que ya tiene 2. Al intentar crear otra,
     * el servicio debe lanzar {@code IllegalStateException} en vez de guardarla.</p>
     *
     * <p>Sirve para comprobar la regla de negocio que impide pasar el límite de capacidad
     * de una zona.</p>
     */
    @Test
    void createCreature_zoneFull() {
        Zone zone = newZone(1L, 2);
        Creature creature = newCreature("Dragon", zone);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> creatureService.createCreature(creature));
    }

    /**
     * 3. No permitir crear una criatura sin zona.
     *
     * <p>Crea una criatura con zona {@code null}. El servicio debe lanzar {@code ResourceNotFoEx}
     * porque toda criatura tiene que estar asignada a una zona.</p>
     *
     * <p>Sirve para comprobar que el servicio no guarda criaturas con datos incompletos.</p>
     */
    @Test
    void createCreature_withoutZone() {
        Creature creature = newCreature("Dragon", null);

        assertThrows(ResourceNotFoEx.class, () -> creatureService.createCreature(creature));
    }

    /**
     * 4. Buscar una criatura existente por ID.
     *
     * <p>Simula que en el repositorio existe una criatura llamada "Fenix" con ID 1. Al buscarla,
     * el servicio debe devolverla con su nombre correcto.</p>
     *
     * <p>Sirve para comprobar el caso normal de {@code getCreatureById}.</p>
     */
    @Test
    void getCreatureById_success() {
        Creature creature = newCreature("Fenix", newZone(1L, 5));

        when(creatureRepository.findById(1L)).thenReturn(Optional.of(creature));

        Creature result = creatureService.getCreatureById(1L);

        assertEquals("Fenix", result.getName());
    }

    /**
     * 5. Buscar una criatura que no existe.
     *
     * <p>Simula que el repositorio no encuentra ninguna criatura con ID 99
     * ({@code Optional.empty()}). El servicio debe lanzar {@code ResourceNotFoEx}, que en la API
     * se convierte en un error 404.</p>
     *
     * <p>Sirve para comprobar que el servicio avisa con un error claro cuando la criatura no existe,
     * en vez de devolver {@code null}.</p>
     */
    @Test
    void getCreatureById_notFound() {
        when(creatureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoEx.class, () -> creatureService.getCreatureById(99L));
    }

    /**
     * 6. Actualizar una criatura correctamente.
     *
     * <p>Existe una criatura llamada "Dragon" en la zona 2 y se actualiza con el nombre
     * "Dragon Rojo" en la misma zona. Como no cambia de zona, no hace falta validar capacidad.
     * La prueba revisa que la criatura guardada tenga el nombre nuevo y siga en la zona 2.</p>
     *
     * <p>Sirve para comprobar que {@code updateCreature} sí copia los datos nuevos a la
     * criatura existente antes de guardarla.</p>
     */
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

    /**
     * 7. Impedir cambiar una criatura a una zona llena.
     *
     * <p>La criatura está en la zona 1 y se intenta mover a la zona 2, que tiene capacidad para
     * 1 criatura y ya tiene 1. El servicio debe lanzar {@code IllegalStateException}.</p>
     *
     * <p>Sirve para comprobar que la regla de capacidad también se aplica al actualizar,
     * no solo al crear.</p>
     */
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

    /**
     * 8. Eliminar una criatura saludable.
     *
     * <p>Simula una criatura con estado {@code "healthy"}. El servicio debe eliminarla sin lanzar
     * errores. Como {@code delete} no devuelve nada, se usa {@code verify} de Mockito para
     * comprobar que el servicio sí le pidió al repositorio borrar esa criatura.</p>
     *
     * <p>Sirve para comprobar el caso normal de {@code deleteCreature}.</p>
     */
    @Test
    void deleteCreature_success() {
        Creature creature = newCreature("Dragon", newZone(1L, 5));

        when(creatureRepository.findById(1L)).thenReturn(Optional.of(creature));

        assertDoesNotThrow(() -> creatureService.deleteCreature(1L));
        verify(creatureRepository).delete(creature);
    }

    /**
     * 9. Impedir eliminar una criatura en estado crítico.
     *
     * <p>Simula una criatura con estado {@code "critical"}. El servicio debe lanzar
     * {@code IllegalStateException} y no borrarla.</p>
     *
     * <p>Sirve para cumplir la prueba adicional que pide el laboratorio: no se puede eliminar
     * una criatura en estado crítico.</p>
     */
    @Test
    void deleteCreature_critical() {
        Creature creature = newCreature("Dragon", newZone(1L, 5));
        creature.setHealthStatus("critical");

        when(creatureRepository.findById(1L)).thenReturn(Optional.of(creature));

        assertThrows(IllegalStateException.class, () -> creatureService.deleteCreature(1L));
    }

    /**
     * 10. Crear una criatura ignora el ID que venga en los datos.
     *
     * <p>Simula que el cliente manda una criatura con ID 99 en un POST. El servicio debe quitar
     * ese ID antes de guardar, para que el repositorio cree una criatura nueva en vez de
     * reemplazar la que ya tenga el ID 99.</p>
     *
     * <p>Sirve para comprobar que un POST siempre crea y nunca sobrescribe otra criatura.</p>
     */
    @Test
    void createCreature_withId_ignoresId() {
        Zone zone = newZone(1L, 5);
        Creature creature = newCreature("Dragon", zone);
        creature.setId(99L);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(creatureRepository.countByZoneId(1L)).thenReturn(0L);
        when(creatureRepository.save(creature)).thenReturn(creature);

        Creature result = creatureService.createCreature(creature);

        assertNull(result.getId());
    }
}
