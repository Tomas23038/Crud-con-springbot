package com.javeriana.zoo_fantastico.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZoneValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Zone validZone() {
        Zone zone = new Zone();
        zone.setName("Bosque");
        zone.setDescription("Zona de criaturas del bosque");
        zone.setCapacity(5);
        return zone;
    }

    // 1. Una zona con todos los datos correctos no tiene errores
    @Test
    void validZone_noErrors() {
        assertTrue(validator.validate(validZone()).isEmpty());
    }

    // 2. El nombre no puede estar vacío
    @Test
    void emptyName_isInvalid() {
        Zone zone = validZone();
        zone.setName("");

        assertFalse(validator.validate(zone).isEmpty());
    }

    // 3. La capacidad debe ser al menos 1
    @Test
    void zeroCapacity_isInvalid() {
        Zone zone = validZone();
        zone.setCapacity(0);

        assertFalse(validator.validate(zone).isEmpty());
    }
}
