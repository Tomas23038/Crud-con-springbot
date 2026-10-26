package com.javeriana.zoo_fantastico.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las validaciones del modelo {@link Zone}.
 *
 * <p>La clase {@code Zone} tiene dos reglas: el nombre no puede estar vacío ({@code @NotBlank})
 * y la capacidad debe ser al menos 1 ({@code @Min(1)}). El controlador las revisa con
 * {@code @Valid} cuando se crea o actualiza una zona desde la API.</p>
 *
 * <p>Estas pruebas comprueban que esas reglas rechazan los datos inválidos. No usan Mockito
 * porque no llaman a ningún repositorio ni servicio.</p>
 */
class ZoneValidationTest {

    /**
     * Validador de Jakarta Bean Validation.
     *
     * <p>Es el mismo motor que usa {@code @Valid} en el controlador. {@code validate(zona)} devuelve
     * los errores encontrados; si no hay ninguno, la zona es válida.</p>
     */
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Método de ayuda que crea una zona con todos sus datos correctos.
     *
     * <p>Cada prueba parte de esta zona válida y cambia un solo dato, para saber con certeza
     * qué regla causa el error.</p>
     *
     * @return una zona que cumple todas las validaciones del modelo
     */
    private Zone validZone() {
        Zone zone = new Zone();
        zone.setName("Bosque");
        zone.setDescription("Zona de criaturas del bosque");
        zone.setCapacity(5);
        return zone;
    }

    /**
     * 1. Una zona con todos los datos correctos no tiene errores.
     *
     * <p>Sirve como punto de control: confirma que {@code validZone()} de verdad es válida,
     * para que las demás pruebas sean confiables.</p>
     */
    @Test
    void validZone_noErrors() {
        assertTrue(validator.validate(validZone()).isEmpty());
    }

    /**
     * 2. El nombre no puede estar vacío.
     *
     * <p>Revisa la regla {@code @NotBlank} del campo {@code name}.</p>
     */
    @Test
    void emptyName_isInvalid() {
        Zone zone = validZone();
        zone.setName("");

        assertFalse(validator.validate(zone).isEmpty());
    }

    /**
     * 3. La capacidad debe ser al menos 1.
     *
     * <p>Revisa la regla {@code @Min(1)} del campo {@code capacity}: una zona donde no cabe
     * ninguna criatura no tiene sentido en el zoológico.</p>
     */
    @Test
    void zeroCapacity_isInvalid() {
        Zone zone = validZone();
        zone.setCapacity(0);

        assertFalse(validator.validate(zone).isEmpty());
    }
}
