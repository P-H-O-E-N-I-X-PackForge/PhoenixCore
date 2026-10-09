package net.phoenix.core.integration.continuum.client.render;

import org.joml.Vector3f;

public record Nebula(String name, Vector3f center, float radius, int color1, int color2, float density, float seed,
                     int puffs) {}
