package com.javeriana.zoo_fantastico.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las validaciones del modelo {@link Creature}.
 *
 * <p>La clase {@code Creature} tiene anotaciones que definen qué datos son válidos:
 * {@code @NotBlank} (texto no vacío), {@code @Min} y {@code @Max} (rangos de números) y
 * {@code @NotNull} (no puede faltar). Cuando llega una petición a la API, el controlador usa
 * {@code @Valid} para revisar esas reglas y rechazar los datos inválidos con un error 400.</p>
 *
 * <p>Estas pruebas cumplen el punto del laboratorio que pide cubrir parámetros de entrada
 * inválidos usando las validaciones del modelo. No usan Mockito porque no llaman a ningún
 * repositorio ni servicio: solo revisan las reglas sobre una criatura creada con {@code new}.</p>
 */
class CreatureValidationTest {

    /**
     * Validador de Jakarta Bean Validation.
     *
     * <p>Es el mismo motor que usa {@code @Valid} en el controlador. Su método {@code validate(objeto)}
     * revisa todas las anotaciones del objeto y devuelve un conjunto con los errores encontrados.
     * Si el conjunto está vacío, el objeto es válido.</p>
     */
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Método de ayuda que crea una criatura con todos sus datos correctos.
     *
     * <p>Cada prueba parte de esta criatura válida y cambia <b>un solo</b> dato para dejarlo
     * inválido. Así, si aparece un error, se sabe que viene de ese dato y no de otro.</p>
     *
     * @return una criatura que cumple todas las validaciones del modelo
     */
    private Creature validCreature() {
        Zone zone = new Zone();
        zone.setId(1L);

        Creature creature = new Creature();
        creature.setName("Fenix");
        creature.setSpecies("Ave");
        creature.setSize(2.5);
        creature.setDangerLevel(5);
        creature.setHealthStatus("healthy");
        creature.setZone(zone);
        return creature;
    }

    /**
     * 1. Una criatura con todos los datos correctos no tiene errores.
     *
     * <p>Sirve como punto de control: confirma que {@code validCreature()} de verdad es válida.
     * Si esta prueba fallara, las demás no servirían, porque el error podría venir de otro dato.</p>
     */
    @Test
    void validCreature_noErrors() {
        assertTrue(validator.validate(validCreature()).isEmpty());
    }

    /**
     * 2. El nombre no puede estar vacío.
     *
     * <p>Revisa la regla {@code @NotBlank} del campo {@code name}: una criatura con nombre
     * {@code ""} debe tener al menos un error de validación.</p>
     */
    @Test
    void emptyName_isInvalid() {
        Creature creature = validCreature();
        creature.setName("");

        assertFalse(validator.validate(creature).isEmpty());
    }

    /**
     * 3. La especie no puede estar vacía.
     *
     * <p>Revisa la regla {@code @NotBlank} del campo {@code species}.</p>
     */
    @Test
    void emptySpecies_isInvalid() {
        Creature creature = validCreature();
        creature.setSpecies("");

        assertFalse(validator.validate(creature).isEmpty());
    }

    /**
     * 4. El tamaño no puede ser negativo.
     *
     * <p>Revisa la regla {@code @Min(0)} del campo {@code size}: un tamaño de -1 no tiene sentido
     * y debe ser rechazado.</p>
     */
    @Test
    void negativeSize_isInvalid() {
        Creature creature = validCreature();
        creature.setSize(-1);

        assertFalse(validator.validate(creature).isEmpty());
    }

    /**
     * 5. El nivel de peligro no puede ser menor que 1.
     *
     * <p>Revisa la regla {@code @Min(1)} del campo {@code dangerLevel}, probando con 0,
     * que está justo por debajo del límite.</p>
     */
    @Test
    void dangerLevelTooLow_isInvalid() {
        Creature creature = validCreature();
        creature.setDangerLevel(0);

        assertFalse(validator.validate(creature).isEmpty());
    }

    /**
     * 6. El nivel de peligro no puede ser mayor que 10.
     *
     * <p>Revisa la regla {@code @Max(10)} del campo {@code dangerLevel}, probando con 15.</p>
     */
    @Test
    void dangerLevelTooHigh_isInvalid() {
        Creature creature = validCreature();
        creature.setDangerLevel(15);

        assertFalse(validator.validate(creature).isEmpty());
    }

    /**
     * 7. El estado de salud no puede estar vacío.
     *
     * <p>Revisa la regla {@code @NotBlank} del campo {@code healthStatus}. Este dato es importante
     * porque el servicio lo usa para decidir si una criatura se puede eliminar.</p>
     */
    @Test
    void emptyHealthStatus_isInvalid() {
        Creature creature = validCreature();
        creature.setHealthStatus("");

        assertFalse(validator.validate(creature).isEmpty());
    }

    /**
     * 8. La criatura debe tener una zona.
     *
     * <p>Revisa la regla {@code @NotNull} del campo {@code zone}: toda criatura tiene que estar
     * asignada a una zona del zoológico.</p>
     */
    @Test
    void nullZone_isInvalid() {
        Creature creature = validCreature();
        creature.setZone(null);

        assertFalse(validator.validate(creature).isEmpty());
    }
}
