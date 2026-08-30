package net.phoenix.core.integration.conflux.dimension.effects;

public abstract class PostProcessingEffects {

    public abstract void render(float partialTick);

    public static class RealityDistortion extends PostProcessingEffects {
        @Override
        public void render(float partialTick) {

        }
    }
}
