package net.phoenix.core.integration.ars_nouveau.api.capability;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.ars_nouveau.api.recipe.lookup.MapSourceIngredient;
import net.phoenix.core.integration.ars_nouveau.common.data.recipe.custom.SourceIngredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SourceRecipeCapability extends RecipeCapability<SourceIngredient> {

    public static final SourceRecipeCapability CAP = new SourceRecipeCapability();

    protected SourceRecipeCapability() {
        super(ResourceLocation.fromNamespaceAndPath("phoenixcore", "source"), 0xC85CCFFF, false, 13,
                SourceIngredient.Serializer.INSTANCE);
    }

    /** Shows Source as a line of text in the recipe viewer (it has no slot to draw). */
    public static com.gregtechceu.gtceu.api.recipe.gui.GTRecipeTypeUILayout.Builder viewer(
                                                                                         com.gregtechceu.gtceu.api.recipe.gui.GTRecipeTypeUILayout.Builder builder) {
        builder.setRecipeViewerLayoutCapabilityLayoutBuilder(CAP, (layout, widget, io) -> {
            if (layout.getRecipeType().getMaxSlots(CAP, io) == 0) return;
            widget.textComponents.child(brachy.modularui.widgets.layout.Flow.col().childPadding(1)
                    .coverChildrenHeight()
                    .widthRel(1f)
                    .crossAxisAlignment(brachy.modularui.utils.Alignment.CrossAxis.START)
                    .name(com.gregtechceu.gtceu.api.recipe.gui.GTRecipeViewerWidget.capabilityWidgetName(CAP, io, 0)));
        });
        builder.setCapabilityContentBuilder(CAP, (widget, content, io, perTick, recipeType, recipe, chanceTier,
                                                  recipeTier) -> {
            if (!(widget instanceof brachy.modularui.widgets.layout.Flow flow)) return;
            if (!(content.content() instanceof SourceIngredient source)) return;
            String name = "source_" + io.name().toLowerCase(java.util.Locale.ROOT);
            var text = brachy.modularui.api.drawable.Text.str(
                    (io == com.gregtechceu.gtceu.api.capability.recipe.IO.IN ? "Uses " : "Makes ") +
                            String.format(java.util.Locale.ROOT, "%,d", source.getSource()) + " Source")
                    .color(0xFFC85CCF);
            var existing = brachy.modularui.widget.WidgetTree.findFirstWithNameNullable(flow, name);
            if (existing instanceof brachy.modularui.widgets.TextWidget<?> tw) tw.value(text);
            else flow.child(text.asWidget().name(name));
        });
        return builder;
    }

    @Override
    public SourceIngredient copyWithModifier(SourceIngredient content, ContentModifier modifier) {
        return new SourceIngredient((int) modifier.apply(content.getSource()));
    }

    @Override
    public SourceIngredient copyInner(SourceIngredient content) {
        return content.copy();
    }

    @Override
    public @Nullable List<AbstractMapIngredient> getDefaultMapIngredient(Object ingredient) {
        List<AbstractMapIngredient> ingredients = new ObjectArrayList<>(1);
        if (ingredient instanceof SourceIngredient s) ingredients.add(new MapSourceIngredient(s));
        return ingredients;
    }
}
