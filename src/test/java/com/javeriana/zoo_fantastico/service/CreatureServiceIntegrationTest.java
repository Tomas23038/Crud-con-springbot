package com.javeriana.zoo_fantastico.service;

import com.javeriana.zoo_fantastico.exception.ResourceNotFoEx;
import com.javeriana.zoo_fantastico.model.Creature;
import com.javeriana.zoo_fantastico.model.Zone;
import com.javeriana.zoo_fantastico.repository.CreatureRepository;
import com.javeriana.zoo_fantastico.repository.ZoneRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de {@link CreatureService} con la base de datos H2.
 *
 * <p>A diferencia de las pruebas unitarias, aquí <b>no hay mocks</b>. {@code @SpringBootTest}
 * levanta la aplicación completa de Spring, con el servicio y los repositorios reales, y los
 * conecta a una base de datos H2 en memoria. Así se prueba que el servicio y la base de datos
 * funcionan bien juntos: que las criaturas de verdad se guardan, se actualizan y se borran,
 * y que las reglas de negocio no dejan cambios indebidos en la base de datos.</p>
 *
 * <p>La conexión a H2 está en {@code src/test/resources/application.properties}. Ese archivo
 * solo se usa al correr las pruebas, así que la aplicación normal sigue usando MySQL.
 * {@code @AutoConfigureTestDatabase(replace = NONE)} le dice a Spring que use esa configuración
 * tal cual y no la reemplace por otra base de datos de prueba.</p>
 *
 * <p>Todas las pruebas comparten la misma base de datos H2 mientras corren. Por eso cada prueba
 * crea su propia zona y busca sus criaturas por ID, para no depender de los datos de otras pruebas.</p>
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CreatureServiceIntegrationTest {

    /**
     * El servicio real de criaturas, creado por Spring.
     *
     * <p>{@code @Autowired} le pide a Spring que entregue el {@code CreatureService} de la
     * aplicación, ya conectado a los repositorios reales y a H2. Es el que se prueba.</p>
     */
    @Autowired
    private CreatureService creatureService;

    /**
     * El repositorio real de criaturas, conectado a H2.
     *
     * <p>Se usa para consultar directamente la base de datos después de llamar al servicio
     * y comprobar que los cambios de verdad quedaron guardados.</p>
     */
    @Autowired
    private CreatureRepository creatureRepository;

    /**
     * El repositorio real de zonas, conectado a H2.
     *
     * <p>Se necesita porque el servicio no deja crear una criatura sin zona. Con este
     * repositorio se guardan primero las zonas en la base de datos.</p>
     */
    @Autowired
    private ZoneRepository zoneRepository;

    /**
     * Método de ayuda que guarda una zona nueva en H2.
     *
     * <p>Cada prueba crea su propia zona para que las criaturas de otras pruebas no ocupen
     * su capacidad.</p>
     *
     * @param capacity número máximo de criaturas que caben en la zona
     * @return la zona ya guardada en la base de datos (con su ID asignado)
     */
    private Zone newZone(int capacity) {
        Zone zone = new Zone();
        zone.setName("Bosque");
        zone.setCapacity(capacity);
        return zoneRepository.save(zone);
    }

    /**
     * Método de ayuda que crea una criatura válida dentro de una zona.
     *
     * <p>La criatura tiene todos los datos que exige el modelo (nombre, especie, nivel de
     * peligro, estado de salud y zona); si faltara alguno, la base de datos la rechazaría.
     * La criatura todavía no se guarda: eso lo hace cada prueba con el servicio.</p>
     *
     * @param name nombre de la criatura
     * @param zone zona a la que pertenece (debe existir en la base de datos)
     * @return una criatura nueva y válida, sin guardar
     */
    private Creature newCreature(String name, Zone zone) {
        Creature creature = new Creature();
        creature.setName(name);
        creature.setSpecies("Caballo magico");
        creature.setDangerLevel(5);
        creature.setHealthStatus("healthy");
        creature.setZone(zone);
        return creature;
    }

    /**
     * 1. Crear una criatura y verificar que se guardó en la base de datos.
     *
     * <p>Crea la criatura "Unicornio" con el servicio y luego la busca directamente en la base
     * de datos con el repositorio. Debe existir y tener el mismo nombre.</p>
     *
     * <p>Sirve para comprobar que {@code createCreature} de verdad guarda la criatura en la
     * base de datos, no solo la devuelve.</p>
     */
    @Test
    void testCreateCreature_ShouldPersistInDatabase() {
        Creature creature = newCreature("Unicornio", newZone(5));

        creatureService.createCreature(creature);
        Optional<Creature> foundCreature = creatureRepository.findById(creature.getId());

        assertTrue(foundCreature.isPresent());
        assertEquals("Unicornio", foundCreature.get().getName());
    }

    /**
     * 2. Buscar por ID una criatura guardada en la base de datos.
     *
     * <p>Crea la criatura "Fenix" y luego la pide al servicio con su ID. El servicio debe
     * leerla de la base de datos y devolverla con su nombre y su especie.</p>
     *
     * <p>Sirve para comprobar que {@code getCreatureById} lee bien de la base de datos.</p>
     */
    @Test
    void testGetCreatureById_ShouldReturnCreatureFromDatabase() {
        Creature creature = newCreature("Fenix", newZone(5));
        creatureService.createCreature(creature);

        Creature result = creatureService.getCreatureById(creature.getId());

        assertEquals("Fenix", result.getName());
        assertEquals("Caballo magico", result.getSpecies());
    }

    /**
     * 3. Buscar una criatura que no existe en la base de datos.
     *
     * <p>Pide al servicio una criatura con un ID que nunca se guardó. Debe lanzar
     * {@code ResourceNotFoEx}.</p>
     *
     * <p>Sirve para comprobar que el servicio responde con un error claro cuando la base
     * de datos no tiene la criatura.</p>
     */
    @Test
    void testGetCreatureById_NotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoEx.class, () -> creatureService.getCreatureById(99999L));
    }

    /**
     * 4. Listar todas las criaturas incluye las que se guardaron.
     *
     * <p>Crea dos criaturas y luego pide la lista completa al servicio. Como otras pruebas
     * también guardan criaturas, no se revisa el tamaño exacto de la lista, sino que las dos
     * criaturas creadas aparezcan en ella.</p>
     *
     * <p>Sirve para comprobar que {@code getAllCreatures} devuelve lo que hay en la base de datos.</p>
     */
    @Test
    void testGetAllCreatures_ShouldIncludeSavedCreatures() {
        Zone zone = newZone(5);
        Creature dragon = newCreature("Dragon", zone);
        Creature grifo = newCreature("Grifo", zone);
        creatureService.createCreature(dragon);
        creatureService.createCreature(grifo);

        List<Long> ids = creatureService.getAllCreatures().stream().map(Creature::getId).toList();

        assertTrue(ids.contains(dragon.getId()));
        assertTrue(ids.contains(grifo.getId()));
    }

    /**
     * 5. No guardar una criatura si la zona está llena.
     *
     * <p>Guarda una zona con capacidad para 1 criatura y crea una. Al intentar crear una segunda,
     * el servicio debe lanzar {@code IllegalStateException}. Luego se revisa en la base de datos
     * que la zona siga con 1 sola criatura.</p>
     *
     * <p>Sirve para comprobar que la regla de capacidad evita que la criatura extra llegue a
     * guardarse en la base de datos.</p>
     */
    @Test
    void testCreateCreature_ZoneFull_ShouldNotPersist() {
        Zone zone = newZone(1);
        creatureService.createCreature(newCreature("Dragon", zone));

        Creature extra = newCreature("Grifo", zone);

        assertThrows(IllegalStateException.class, () -> creatureService.createCreature(extra));
        assertEquals(1, creatureRepository.countByZoneId(zone.getId()));
    }

    /**
     * 6. Actualizar una criatura y verificar los cambios en la base de datos.
     *
     * <p>Crea la criatura "Unicornio" y luego la actualiza con el nombre "Unicornio Dorado" y
     * nivel de peligro 8. Después la busca en la base de datos y revisa que tenga los datos nuevos.</p>
     *
     * <p>Sirve para comprobar que {@code updateCreature} de verdad guarda los cambios en la
     * base de datos.</p>
     */
    @Test
    void testUpdateCreature_ShouldUpdateInDatabase() {
        Zone zone = newZone(5);
        Creature creature = newCreature("Unicornio", zone);
        creatureService.createCreature(creature);

        Creature updated = newCreature("Unicornio Dorado", zone);
        updated.setDangerLevel(8);

        creatureService.updateCreature(creature.getId(), updated);
        Optional<Creature> foundCreature = creatureRepository.findById(creature.getId());

        assertTrue(foundCreature.isPresent());
        assertEquals("Unicornio Dorado", foundCreature.get().getName());
        assertEquals(8, foundCreature.get().getDangerLevel());
    }

    /**
     * 7. Mover una criatura a otra zona y verificarlo en la base de datos.
     *
     * <p>Crea una criatura en una zona y la actualiza para pasarla a otra zona con espacio.
     * En la base de datos, la criatura debe quedar en la zona nueva.</p>
     *
     * <p>Sirve para comprobar que la relación entre criatura y zona también se actualiza
     * en la base de datos.</p>
     */
    @Test
    void testUpdateCreature_ChangeZone_ShouldUpdateInDatabase() {
        Creature creature = newCreature("Unicornio", newZone(5));
        creatureService.createCreature(creature);

        Zone newZone = newZone(5);
        creatureService.updateCreature(creature.getId(), newCreature("Unicornio", newZone));
        Creature foundCreature = creatureRepository.findById(creature.getId()).orElseThrow();

        assertEquals(newZone.getId(), foundCreature.getZone().getId());
    }

    /**
     * 8. No mover una criatura a una zona llena.
     *
     * <p>Crea una criatura en una zona con espacio y otra zona con capacidad 1 que ya está
     * ocupada. Al intentar mover la criatura a la zona llena, el servicio debe lanzar
     * {@code IllegalStateException}, y en la base de datos la criatura debe seguir en su zona original.</p>
     *
     * <p>Sirve para comprobar que, si la actualización falla, la base de datos no queda con
     * datos a medio cambiar.</p>
     */
    @Test
    void testUpdateCreature_NewZoneFull_ShouldNotChangeDatabase() {
        Zone originalZone = newZone(5);
        Creature creature = newCreature("Unicornio", originalZone);
        creatureService.createCreature(creature);

        Zone fullZone = newZone(1);
        creatureService.createCreature(newCreature("Dragon", fullZone));

        Creature updated = newCreature("Unicornio", fullZone);

        assertThrows(IllegalStateException.class,
                () -> creatureService.updateCreature(creature.getId(), updated));
        Creature foundCreature = creatureRepository.findById(creature.getId()).orElseThrow();
        assertEquals(originalZone.getId(), foundCreature.getZone().getId());
    }

    /**
     * 9. Eliminar una criatura y confirmar que ya no existe en la base de datos.
     *
     * <p>Crea la criatura "Unicornio" (con estado de salud {@code "healthy"}, así que sí se
     * puede borrar), la elimina con el servicio y luego la busca en la base de datos. Ya no
     * debe encontrarse.</p>
     *
     * <p>Sirve para comprobar que {@code deleteCreature} de verdad borra la criatura de la
     * base de datos.</p>
     */
    @Test
    void testDeleteCreature_ShouldRemoveFromDatabase() {
        Creature creature = newCreature("Unicornio", newZone(5));
        creatureService.createCreature(creature);

        creatureService.deleteCreature(creature.getId());
        Optional<Creature> foundCreature = creatureRepository.findById(creature.getId());

        assertFalse(foundCreature.isPresent());
    }

    /**
     * 10. No eliminar una criatura en estado crítico.
     *
     * <p>Crea una criatura con estado {@code "critical"}. Al intentar borrarla, el servicio debe
     * lanzar {@code IllegalStateException}, y la criatura debe seguir existiendo en la base de datos.</p>
     *
     * <p>Sirve para comprobar, ya con base de datos real, la regla del laboratorio de que no se
     * puede eliminar una criatura en estado crítico.</p>
     */
    @Test
    void testDeleteCreature_Critical_ShouldStayInDatabase() {
        Creature creature = newCreature("Unicornio", newZone(5));
        creature.setHealthStatus("critical");
        creatureService.createCreature(creature);

        assertThrows(IllegalStateException.class,
                () -> creatureService.deleteCreature(creature.getId()));
        assertTrue(creatureRepository.findById(creature.getId()).isPresent());
    }
}
