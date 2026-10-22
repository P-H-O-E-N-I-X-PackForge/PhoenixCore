Fixed CompactCount + GuiGraphicsCountSuffixMixin, for the 1.20.1 branch (Forge). Drop them into the same packages:

  net.phoenix.core.utils.CompactCount
  net.phoenix.core.mixin.minecraft.GuiGraphicsCountSuffixMixin   (and keep it listed in the mixins json)

Nothing in them is version specific: GuiGraphics.renderItemDecorations(Font, ItemStack, int, int, String), the two
GuiGraphics.drawString overloads it injects into, and GuiGraphics.pose() all have the same signatures in 1.20.1 (mojmap) and
1.21.1. On Forge 1.20.1 the mixin needs its refmap as usual (the mixin annotation processor makes it).

What was wrong with the old mixin: it rewrote EVERY number any screen drew (EMI's recipe preview and its overclock tier, text
fields, tooltips), and it right-aligned the compact text using the width of the COMPACT text instead of the width of the long
text vanilla had already aligned, so the number ended up in the wrong place. Now it only touches the count drawn on an item
(renderItemDecorations) and anchors the compact text to the same right edge as the long one.

CompactCount.java is unchanged apart from being included for convenience.
