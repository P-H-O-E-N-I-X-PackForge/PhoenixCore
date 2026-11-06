package net.phoenix.core.integration.astral.api.capability;

import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

public class AstralThreadIngredient {

    public static final AstralThreadIngredient EMPTY = new AstralThreadIngredient(0);

    public static final Codec<AstralThreadIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("thread").forGetter(AstralThreadIngredient::getThread))
            .apply(instance, AstralThreadIngredient::new));

    @Getter
    private int thread;

    public AstralThreadIngredient(int thread) {
        this.thread = thread;
    }

    public AstralThreadIngredient copy() {
        return new AstralThreadIngredient(thread);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AstralThreadIngredient other)) return false;
        return this.thread == other.thread;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(thread);
    }

    @Override
    public String toString() {
        return "AstralThreadIngredient{thread=" + thread + "}";
    }

    public static final class Serializer implements IContentSerializer<AstralThreadIngredient> {

        public static final AstralThreadIngredient.Serializer INSTANCE = new AstralThreadIngredient.Serializer();

        @Override
        public AstralThreadIngredient of(Object o) {
            if (o instanceof Integer integer) {
                return new AstralThreadIngredient(integer);
            } else if (o instanceof AstralThreadIngredient astralThreadIngredient) {
                return astralThreadIngredient;
            }
            return null;
        }

        @Override
        public AstralThreadIngredient defaultValue() {
            return EMPTY;
        }

        @Override
        public Class<AstralThreadIngredient> contentClass() {
            return AstralThreadIngredient.class;
        }

        @Override
        public Codec<AstralThreadIngredient> codec() {
            return CODEC;
        }
    }
}
