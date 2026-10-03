package com.javeriana.zoo_fantastico.controller;
import com.javeriana.zoo_fantastico.dto.zoneresponse;
import com.javeriana.zoo_fantastico.model.Zone;
import com.javeriana.zoo_fantastico.service.ZoneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @PostMapping
    public ResponseEntity<zoneresponse> createZone(@Valid @RequestBody Zone zone) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zoneService.createZone(zone));
    }

    @GetMapping
    public List<zoneresponse> getAllZones() {
        return zoneService.getAllZones();
    }

    @GetMapping("/{id}")
    public zoneresponse getZoneById(@PathVariable Long id) {
        return zoneService.getZoneById(id);
    }

    @PutMapping("/{id}")
    public zoneresponse updateZone(@PathVariable Long id, @Valid @RequestBody Zone updatedZone) {
        return zoneService.updateZone(id, updatedZone);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        zoneService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }
}