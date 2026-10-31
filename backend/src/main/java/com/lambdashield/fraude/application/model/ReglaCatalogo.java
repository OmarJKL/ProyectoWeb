package com.lambdashield.fraude.application.model;

/** Regla antifraude activa del catálogo persistido. */
public record ReglaCatalogo(String codigo, String nombre, String descripcion, int pesoMaximo) {}