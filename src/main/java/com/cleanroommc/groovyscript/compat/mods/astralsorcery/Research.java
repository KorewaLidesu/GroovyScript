package com.cleanroommc.groovyscript.compat.mods.astralsorcery;

import com.cleanroommc.groovyscript.api.GroovyBlacklist;
import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.compat.mods.ModSupport;
import com.cleanroommc.groovyscript.core.mixin.astralsorcery.ResearchNodeAccessor;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import hellfirepvp.astralsorcery.client.gui.journal.page.*;
import hellfirepvp.astralsorcery.common.crafting.altar.AltarRecipeRegistry;
import hellfirepvp.astralsorcery.common.crafting.altar.recipes.AttunementRecipe;
import hellfirepvp.astralsorcery.common.crafting.altar.recipes.ConstellationRecipe;
import hellfirepvp.astralsorcery.common.crafting.altar.recipes.DiscoveryRecipe;
import hellfirepvp.astralsorcery.common.crafting.altar.recipes.TraitRecipe;
import hellfirepvp.astralsorcery.common.data.research.ResearchNode;
import hellfirepvp.astralsorcery.common.data.research.ResearchProgression;
import hellfirepvp.astralsorcery.common.tile.TileAltar;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Research extends VirtualizedRegistry<ResearchNode> {

    private final HashMap<ResearchNode, ResearchProgression> scriptedCategories = new HashMap<>();
    private final HashMap<ResearchNode, ResearchProgression> removedCategories = new HashMap<>();
    private final HashMap<ResearchNode, ArrayList<ResearchNode>> scriptedConnections = new HashMap<>();
    private final HashMap<ResearchNode, ArrayList<ResearchNode>> removedConnections = new HashMap<>();
    private final HashMap<ResearchNode, Point> movedNodes = new HashMap<>();

    @Override
    @GroovyBlacklist
    @ApiStatus.Internal
    public void onReload() {
        removeScripted().forEach(node -> {
            if (this.scriptedCategories.containsKey(node))
                this.removeNode(scriptedCategories.get(node), node);
        });
        restoreFromBackup().forEach(node -> {
            if (this.removedCategories.containsKey(node))
                this.addNode(removedCategories.get(node), node, false);
        });

        scriptedConnections.forEach((source, connections) -> {
            for (ResearchNode dest : connections) {
                dest.getConnectionsTo().remove(source);
            }
        });
        removedConnections.forEach((source, connections) -> {
            for (ResearchNode dest : connections) {
                dest.addSourceConnectionFrom(source);
            }
        });

        movedNodes.forEach(((node, point) -> this.moveNode(node, point.x, point.y, false)));

        movedNodes.clear();
        scriptedConnections.clear();
        removedConnections.clear();
        scriptedCategories.clear();
        removedCategories.clear();
    }

    public ResearchNode getNode(String name) {
        return ResearchProgression.findNode(name);
    }

    public void addNode(ResearchProgression category, ResearchNode node) {
        this.addNode(category, node, true);
    }

    public void addNode(ResearchProgression category, ResearchNode node, boolean addScripted) {
        if (addScripted) {
            this.addScripted(node);
            this.scriptedCategories.put(node, category);
        }
        category.getRegistry().register(node);
    }

    public void removeNode(String name) {
        if (ResearchProgression.findNode(name) != null) {
            ResearchProgression.findProgression(ResearchProgression.findNode(name)).forEach(category -> {
                category.getResearchNodes().forEach(node -> {
                    if (node.getSimpleName().equals(name)) {
                        this.addBackup(node);
                        this.removedCategories.put(node, category);
                    }
                });
                category.getResearchNodes().removeIf(node -> node.getSimpleName().equals(name));
            });
        }
    }

    private void removeNode(ResearchProgression category, ResearchNode node) {
        category.getResearchNodes().removeIf(registeredNode -> node.getSimpleName().equals(registeredNode.getSimpleName()));
    }

    public void moveNode(String name, int x, int z) {
        ResearchNode node = this.getNode(name);
        if (node != null)
            this.moveNode(node, x, z, true);
    }

    private void moveNode(ResearchNode node, int x, int z, boolean doBackup) {
        if (node == null) return;
        if (doBackup)
            this.movedNodes.put(node, new Point(node.renderPosX, node.renderPosZ));
        ((ResearchNodeAccessor) node).setX(x);
        ((ResearchNodeAccessor) node).setZ(z);
    }

    public void connectNodes(String source, String dest) {
        this.connectNodes(this.getNode(source), this.getNode(dest), true);
    }

    public void connectNodes(ResearchNode source, ResearchNode dest, boolean addScripted) {
        if (addScripted) {
            if (!this.scriptedConnections.containsKey(source)) {
                this.scriptedConnections.put(source, new ArrayList<>());
            }
            this.scriptedConnections.get(source).add(dest);
        }
        dest.addSourceConnectionFrom(source);
    }

    public void disconnectNodes(String node1, String node2) {
        ResearchNode first = this.getNode(node1);
        ResearchNode second = this.getNode(node2);
        if (first == null || second == null) return;
        this.disconnectNodes(first, second, true);
        this.disconnectNodes(second, first, true);
    }

    public void disconnectNodes(ResearchNode node1, ResearchNode node2, boolean doBackup) {
        if (doBackup) {
            if (node1.getConnectionsTo().contains(node2)) {
                if (!this.removedConnections.containsKey(node1)) {
                    this.removedConnections.put(node1, new ArrayList<>());
                }
                this.removedConnections.get(node1).add(node2);
            }
        }

        node1.getConnectionsTo().remove(node2);
    }

    public ResearchNodeBuilder researchBuilder() {
        return new ResearchNodeBuilder();
    }

    public JournalPageBuilder pageBuilder() {
        return new JournalPageBuilder();
    }

    public static class ResearchNodeBuilder {

        private ResearchProgression category;
        private ItemStack node;
        private final ArrayList<IJournalPage> pages = new ArrayList<>();
        private String name;
        private Point location;
        private final ArrayList<ResearchNode> connections = new ArrayList<>();


        public ResearchNodeBuilder discovery() {
            this.category = ResearchProgression.DISCOVERY;
            return this;
        }

        public ResearchNodeBuilder exploration() {
            this.category = ResearchProgression.BASIC_CRAFT;
            return this;
        }

        public ResearchNodeBuilder attunement() {
            this.category = ResearchProgression.ATTUNEMENT;
            return this;
        }

        public ResearchNodeBuilder constellation() {
            this.category = ResearchProgression.CONSTELLATION;
            return this;
        }

        public ResearchNodeBuilder radiance() {
            this.category = ResearchProgression.RADIANCE;
            return this;
        }

        public ResearchNodeBuilder brilliance() {
            this.category = ResearchProgression.BRILLIANCE;
            return this;
        }

        public ResearchNodeBuilder icon(ItemStack item) {
            this.node = item;
            return this;
        }

        public ResearchNodeBuilder page(IJournalPage page) {
            this.pages.add(page);
            return this;
        }

        public ResearchNodeBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ResearchNodeBuilder point(int x, int y) {
            this.location = new Point(x, y);
            return this;
        }

        public ResearchNodeBuilder connectionFrom(String source) {
            this.connections.add(ModSupport.ASTRAL_SORCERY.get().research.getNode(source));
            return this;
        }

        public ResearchNodeBuilder connectionFrom(ResearchNode source) {
            this.connections.add(source);
            return this;
        }

        private boolean validate() {
            GroovyLog.Msg out = GroovyLog.msg("Error adding Research Node to Astral Sorcery Journal").error();

            if (this.name == null || this.name.equals("")) {
                out.add("Name not provided.");
            }
            if (this.node == null || this.node.isItemEqual(ItemStack.EMPTY)) {
                out.add("No display item provided.");
            }
            if (this.location == null) {
                out.add("No location specified.");
            }
            if (this.category == null) {
                out.add("No research tab specified.");
            }

            return !out.postIfNotEmpty();
        }

        public void register() {
            if (!validate()) return;
            ResearchNode researchNode = new ResearchNode(this.node, this.name, this.location.x, this.location.y);
            this.pages.forEach(researchNode::addPage);
            this.connections.forEach(researchNode::addSourceConnectionFrom);
            ModSupport.ASTRAL_SORCERY.get().research.addNode(category, researchNode);
        }

    }

    public static class JournalPageBuilder {

        // TODO: implement public IJournalPage vanillaRecipePage(IRecipe recipe); -> returns JournalPageRecipe

        public IJournalPage discoveryRecipePage(ItemStack output) {
            AtomicReference<DiscoveryRecipe> recipe = new AtomicReference<>(null);
            AltarRecipeRegistry.recipes.get(TileAltar.AltarLevel.DISCOVERY).forEach(registeredRecipe -> {
                if (registeredRecipe.getOutputForMatching().isItemEqual(output)) {
                    recipe.set((DiscoveryRecipe) registeredRecipe);
                }
            });
            return new JournalPageDiscoveryRecipe(recipe.get());
        }

        public IJournalPage constellationRecipePage(ItemStack output) {
            AtomicReference<ConstellationRecipe> recipe = new AtomicReference<>(null);
            AltarRecipeRegistry.recipes.get(TileAltar.AltarLevel.CONSTELLATION_CRAFT).forEach(registeredRecipe -> {
                if (registeredRecipe.getOutputForMatching().isItemEqual(output)) {
                    recipe.set((ConstellationRecipe) registeredRecipe);
                }
            });
            return new JournalPageConstellationRecipe(recipe.get());
        }

        public IJournalPage attunementRecipePage(ItemStack output) {
            AtomicReference<AttunementRecipe> recipe = new AtomicReference<>(null);
            AltarRecipeRegistry.recipes.get(TileAltar.AltarLevel.ATTUNEMENT).forEach(registeredRecipe -> {
                if (registeredRecipe.getOutputForMatching().isItemEqual(output)) {
                    recipe.set((AttunementRecipe) registeredRecipe);
                }
            });
            return new JournalPageAttunementRecipe(recipe.get());
        }

        public IJournalPage traitRecipePage(ItemStack output) {
            AtomicReference<TraitRecipe> recipe = new AtomicReference<>(null);
            AltarRecipeRegistry.recipes.get(TileAltar.AltarLevel.TRAIT_CRAFT).forEach(registeredRecipe -> {
                if (registeredRecipe.getOutputForMatching().isItemEqual(output)) {
                    recipe.set((TraitRecipe) registeredRecipe);
                }
            });
            return new JournalPageTraitRecipe(recipe.get());
        }

        public IJournalPage textPage(String unlocText) {
            return new JournalPageText(unlocText);
        }

        public IJournalPage emptyPage() {
            return new JournalPageEmpty();
        }

    }

}
