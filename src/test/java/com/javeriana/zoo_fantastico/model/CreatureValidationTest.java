package com.javeriana.zoo_fantastico.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreatureValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

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

    // 1. Una criatura con todos los datos correctos no tiene errores
    @Test
    void validCreature_noErrors() {
        assertTrue(validator.validate(validCreature()).isEmpty());
    }

    // 2. El nombre no puede estar vacío
    @Test
    void emptyName_isInvalid() {
        Creature creature = validCreature();
        creature.setName("");

        assertFalse(validator.validate(creature).isEmpty());
    }

    // 3. La especie no puede estar vacía
    @Test
    void emptySpecies_isInvalid() {
        Creature creature = validCreature();
        creature.setSpecies("");

        assertFalse(validator.validate(creature).isEmpty());
    }

    // 4. El tamaño no puede ser negativo
    @Test
    void negativeSize_isInvalid() {
        Creature creature = validCreature();
        creature.setSize(-1);

        assertFalse(validator.validate(creature).isEmpty());
    }

    // 5. El nivel de peligro no puede ser menor que 1
    @Test
    void dangerLevelTooLow_isInvalid() {
        Creature creature = validCreature();
        creature.setDangerLevel(0);

        assertFalse(validator.validate(creature).isEmpty());
    }

    // 6. El nivel de peligro no puede ser mayor que 10
    @Test
    void dangerLevelTooHigh_isInvalid() {
        Creature creature = validCreature();
        creature.setDangerLevel(15);

        assertFalse(validator.validate(creature).isEmpty());
    }

    // 7. El estado de salud no puede estar vacío
    @Test
    void emptyHealthStatus_isInvalid() {
        Creature creature = validCreature();
        creature.setHealthStatus("");

        assertFalse(validator.validate(creature).isEmpty());
    }

    // 8. La criatura debe tener una zona
    @Test
    void nullZone_isInvalid() {
        Creature creature = validCreature();
        creature.setZone(null);

        assertFalse(validator.validate(creature).isEmpty());
    }
}
