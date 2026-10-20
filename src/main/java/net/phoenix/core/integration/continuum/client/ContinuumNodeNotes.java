package net.phoenix.core.integration.continuum.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.research.ResearchNode;
import net.phoenix.core.integration.continuum.data.ContinuumData;

import java.util.ArrayList;
import java.util.List;

/** The "Surveys Rime" lines Continuum adds to the Conflux node viewer, through ConfluxNodeNotes. */
@OnlyIn(Dist.CLIENT)
public final class ContinuumNodeNotes {

    private ContinuumNodeNotes() {}

    public static List<String> notes(ResearchNode node) {
        List<String> out = new ArrayList<>();
        for (var body : ContinuumData.allBodies()) {
            if (node.id.equals(body.surveyedResearch())) out.add("\u00a7b\u25ce Surveys " + body.name());
            else if (node.id.equals(body.detectedResearch())) out.add("\u00a7e\u25ce Detects " + body.name());
        }
        for (var system : ContinuumData.systems()) {
            if (node.id.equals(system.surveyedResearch())) out.add("\u00a7b\u25ce Surveys the " + system.name() + " system");
            else if (node.id.equals(system.detectedResearch())) {
                out.add("\u00a7e\u25ce Detects the " + system.name() + " system");
            }
        }
        return out;
    }
}
