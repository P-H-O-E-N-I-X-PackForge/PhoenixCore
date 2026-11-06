Item/amount count compaction for the 1.20.1 branch (Forge), fixed. Three files; drop them into the same packages:

  net.phoenix.core.utils.CompactCount                               (utils)
  net.phoenix.core.mixin.minecraft.GuiGraphicsCountSuffixMixin      (vanilla item counts)
  net.phoenix.core.mixin.emi.EmiRenderHelperMixin                   (EMI amounts: this is the one GT recipes need)

and list both mixins in the CLIENT section of the mixins json:

    "minecraft.GuiGraphicsCountSuffixMixin",
    "emi.EmiRenderHelperMixin"

Marvin: change the package/import lines (phoenix -> sog) as before. Nothing else needs changing.

WHY THE OLD ONE WAS BROKEN. It hooked every GuiGraphics.drawString and rewrote ANY number any screen drew, which scrambled other
screens, and it put the compact text in the wrong place. And it never actually dealt with how a GT recipe in EMI draws its counts:

  * LDLib's ModularEmiRecipe (what GT's recipe pages use) clears the LDLib slots and adds EMI SlotWidgets in their place, so the
    item AND its count are drawn by EMI, not by vanilla.
  * EMI's ItemEmiStack.render draws the item, asks vanilla for decorations with an EMPTY count (so there is no vanilla count),
    then draws the amount itself with EmiRenderHelper.renderAmount(EmiDrawContext, int, int, Component).
  * renderAmount right-aligns the text but caps its width at 14px, so a long number starts inside the slot and runs into the
    next one. That is the "doubling/overlap" in the recipe page: the numbers overlap their neighbours.

WHAT IT DOES NOW.
  * EmiRenderHelperMixin: at the head of renderAmount, if the amount is a plain number of 1,000 or more it is drawn compactly
    (1.5k, 15.3k, 123k, 1.5M), smaller, right-aligned to the slot's edge, and the original call is cancelled. Anything that is
    not a plain number (fluid amounts like "16 B", chances, ranges) is left to EMI.
  * GuiGraphicsCountSuffixMixin: only the count vanilla draws on an item (GuiGraphics.renderItemDecorations); nothing else that
    happens to be a number. The compact text is right-aligned to the same edge vanilla used for the long text.
  * CompactCount.COUNT_THRESHOLD (1,000) is where both start compacting. compactIfNumeric(String) alone still uses 10,000.

VERSIONS. EmiRenderHelper.renderAmount and the EmiDrawContext methods used (push, pop, matrices, drawTextWithShadow) are the same
in EMI 1.1.13 and 1.1.24 for 1.20.1, and in 1.1.24 for 1.21.1 (checked in the jars). The vanilla GuiGraphics methods hooked
have the same signatures in 1.20.1 and 1.21.1. The EMI mixin uses remap = false (EMI is not obfuscated); the vanilla one needs
the usual refmap on Forge 1.20.1.

NOT IN THIS FOLDER: ItemDisplayWidgetMixin (modularui) is 1.21.1 only: ModularUI's ItemDisplayWidget draws the vanilla count
and its own amount on top of it. 1.20.1 uses LDLib, not ModularUI.

To see it work, PhoenixCore has two example recipes (common/data/CountDisplayExampleRecipes.java): an Assembler recipe with
inputs of 64 up to 1,500,000 and a Centrifuge recipe with outputs of 12 up to 25,000,000. Copy that class if you want them too.
