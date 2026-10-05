package com.javeriana.zoo_fantastico.dto;
public record zoneresponse(
    Long id,
    String name,
    String description,
    int capacity,
    long creatureCount){
    }