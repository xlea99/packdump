package net.xlea99.packdump.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.xlea99.packdump.PackdumpConfig;
import net.xlea99.packdump.PackdumpPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dumps the texture each block renders with, and the texture its model asked for.
 *
 * <p><b>Why this cannot be inferred from outside.</b> {@code <namespace>:block/<path>} is a
 * convention, not a rule. Alex's Caves nests paths, StoneZone registers blocks at
 * {@code c/alexscaves/cut_guanostone}, and any mod is free to point a model wherever it
 * likes. A tool generating a stair from a foreign base block has to name that base's
 * texture exactly; guessing produces the purple checkerboard, which is a failure nobody
 * sees until they are standing in front of it.
 *
 * <p><b>Why the baked model rather than the jar.</b> Reading {@code twigs.jar} directly
 * would give the texture the mod shipped, not the texture the game draws — every resource
 * pack in the load order, including the user's own, is applied by the time a model is
 * baked. Asking the renderer answers for the pack as it actually is, which is the whole
 * reason this dump exists rather than a file reader.
 *
 * <p><b>Missing textures are recorded, not skipped.</b> A block whose texture does not
 * resolve reports {@code minecraft:missingno}, and that is a fact worth carrying: it is
 * exactly the state of a generated block whose art has not been drawn yet. Dropping the
 * entry would make "no texture" indistinguishable from "not in this dump".
 *
 * <p><b>Which is exactly why {@code requested} exists (schema 5).</b> {@code missingno} says
 * a block has no art; it does not say where the art belongs, because by bake time the
 * missing-texture sprite has already been substituted and the name the model asked for is
 * gone. That name is the one thing a tool needs in order to *create* the art — so it is read
 * from the UNBAKED model, whose texture map is plain JSON and never consults the atlas. The
 * two answers together say "this renders as nothing, and here is the file that would fix
 * it", which neither says alone.
 *
 * <p><b>Blocks only.</b> An item's inventory model is a different fact, and for a BlockItem
 * the block is reachable through {@code block_items.json}'s {@code places} map — so adding
 * an item map here would be a second answer to a question already answered.
 */
public final class TextureDumpService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TextureDumpService() {}

    /**
     * Writes {@code attributes/textures.json}, or nothing at all on a dedicated server.
     *
     * <p>Textures are client data and there is no server-side answer to fall back on. The
     * file is therefore absent rather than empty on a server, so a reader can tell "dumped
     * without a client" from "dumped and every block came back blank" — the same
     * present-or-absent rule {@code block_items.json} follows.
     */
    public static void dumpTextures() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            LOGGER.info("packdump texture dump skipped: no client, textures do not exist here");
            return;
        }
        LOGGER.info("packdump dumping block textures...");

        // Reached only behind the dist check and only through this call, so a dedicated
        // server never loads the class and never resolves `Minecraft` at all. Verifying a
        // method body that mentions a client-only type is enough to throw there.
        JsonObject resolved = new JsonObject();
        JsonObject requested = new JsonObject();
        ClientTextures.collect(resolved, requested);

        JsonObject textures = new JsonObject();
        textures.add("minecraft:block", resolved);

        // A sibling key, not a reshaping of `textures` — a schema 4 reader ignores it and a
        // schema 5 reader can still tell "absent" from "empty". Written for every block
        // rather than only the missing ones so that absence keeps ONE meaning: this dump
        // never looked. A sparse map would have made it mean "same as the other field" too,
        // and a reader that forgot the rule would read a hole as an answer.
        JsonObject requestedByRegistry = new JsonObject();
        requestedByRegistry.add("minecraft:block", requested);

        JsonObject root = new JsonObject();
        root.addProperty("schema_version", DumpSchema.VERSION);
        root.addProperty("type", "textures");
        root.add("textures", textures);
        root.add("requested", requestedByRegistry);

        try {
            Path outDir = PackdumpPaths.attributesDir();
            Files.createDirectories(outDir);
            Path outFile = outDir.resolve("textures.json");

            if (!PackdumpConfig.overwriteExisting && Files.exists(outFile)) {
                outFile = outDir.resolve("textures_" + System.currentTimeMillis() + ".json");
            }
            Files.writeString(outFile, GSON.toJson(root), StandardCharsets.UTF_8);
            LOGGER.info("packdump texture dump wrote {} ({} blocks, {} with a requested name)",
                    outFile.toAbsolutePath(), resolved.size(), requested.size());
        } catch (IOException e) {
            LOGGER.error("packdump texture dump failed", e);
        }
    }

    /**
     * The client half, in its own class so the server never classloads it.
     *
     * <p>Baked models are immutable once the resource reload that produced them has
     * finished, and everything here is a map lookup plus two field reads — so reading them
     * from the server thread during {@code ServerStartingEvent} is safe. Handing the work
     * to the client thread instead would be worse than unnecessary: at that moment the
     * client thread is blocked waiting for the integrated server to finish starting, so
     * waiting on it would deadlock.
     *
     * <p>The same holds for the unbaked models. {@code ModelManager} keeps its
     * {@code ModelBakery} after the reload that built it, and {@code ModelBakery.getModel}
     * serves from a cache that baking already ran {@code resolveParents} over — so nothing
     * here has to hook the bake, keep static state, or notice a resource reload. It reads
     * what is sitting there, exactly like the baked half does.
     */
    private static final class ClientTextures {

        private ClientTextures() {}

        /**
         * Fills both maps in one pass over the block registry.
         *
         * <p>One pass rather than two because the answers must agree about which blocks
         * exist: iterating twice would let a mod that throws on one call and not the other
         * produce a `requested` entry for a block with no `texture`, which is a state
         * nothing downstream should have to reason about.
         */
        static void collect(JsonObject resolved, JsonObject requested) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var renderer = minecraft.getBlockRenderer();
            var bakery = minecraft.getModelManager().getModelBakery();

            net.minecraftforge.registries.ForgeRegistries.BLOCKS.getEntries().stream()
                    .sorted(java.util.Comparator.comparing(e -> e.getKey().location().toString()))
                    .forEach(entry -> {
                        ResourceLocation id = entry.getKey().location();
                        var state = entry.getValue().defaultBlockState();
                        try {
                            var model = renderer.getBlockModel(state);
                            // The Forge overload, not the deprecated no-arg one. `ModelData.EMPTY`
                            // is what a block with no block entity resolves with anyway, and it is
                            // the only thing available outside a world.
                            var sprite = model.getParticleIcon(
                                    net.minecraftforge.client.model.data.ModelData.EMPTY);
                            resolved.addProperty(id.toString(), sprite.contents().name().toString());
                        } catch (Exception e) {
                            // One misbehaving mod's model must not cost the other 13,000
                            // blocks their textures. Absent means "could not be answered",
                            // which is what happened.
                            LOGGER.warn("packdump could not resolve a texture for {}: {}",
                                    id, e.toString());
                        }
                        try {
                            ResourceLocation asked = requestedTexture(bakery, state);
                            if (asked != null) {
                                requested.addProperty(id.toString(), asked.toString());
                            }
                        } catch (Exception e) {
                            LOGGER.warn("packdump could not read the requested texture for {}: {}",
                                    id, e.toString());
                        }
                    });
        }

        /**
         * The texture name a block's model declares, before the atlas got involved.
         *
         * <p>{@code particle} rather than a face, because it is the one texture key every
         * block model has — vanilla's parents define it, and a model with faces but no
         * explicit particle inherits {@code #all} or {@code #side} through the parent chain.
         * Asking for a face instead would mean knowing which faces this model happens to
         * have, which is the mod-specific guessing this whole file exists to avoid.
         */
        private static ResourceLocation requestedTexture(
                net.minecraft.client.resources.model.ModelBakery bakery,
                net.minecraft.world.level.block.state.BlockState state) {
            var location = net.minecraft.client.renderer.block.BlockModelShaper
                    .stateToModelLocation(state);
            var model = blockModelOf(bakery, bakery.getModel(location));
            return model == null ? null : model.getMaterial("particle").texture();
        }

        /**
         * The {@code BlockModel} behind whatever a blockstate resolved to.
         *
         * <p>A blockstate's top-level model is a {@code MultiVariant} or a {@code MultiPart},
         * neither of which holds textures — they hold references to the models that do, and
         * one hop reaches those.
         *
         * <p><b>Which one, and why it must not be "whichever".</b> {@code getDependencies}
         * returns {@code Collectors.toSet()} — unordered, so "the first" is arbitrary and
         * two blocks with identical JSON can answer differently. {@code MultiVariant} keeps
         * its variants as a {@code List} in declaration order, so the first is the one the
         * blockstate file names first, which is the primary by convention. Anything else
         * (multipart, and the odd custom type) is sorted before choosing, purely so the
         * answer is a function of the pack rather than of a hash bucket.
         *
         * <p><b>This is a best answer, not the answer.</b> Measured on a 300-mod pack, 211
         * blocks of 13,841 render with a texture the unbaked model does not name — Forge
         * geometry loaders and CTM replace the baked model entirely, so the JSON underneath
         * has stopped describing what is drawn. That is why {@code texture} stays the
         * authority whenever it resolves, and this is consulted only when nothing renders,
         * which is the one case {@code texture} genuinely cannot answer.
         */
        private static net.minecraft.client.renderer.block.model.BlockModel blockModelOf(
                net.minecraft.client.resources.model.ModelBakery bakery,
                net.minecraft.client.resources.model.UnbakedModel model) {
            if (model instanceof net.minecraft.client.renderer.block.model.BlockModel direct) {
                return direct;
            }
            if (model == null) {
                return null;
            }
            java.util.List<ResourceLocation> candidates;
            if (model instanceof net.minecraft.client.renderer.block.model.MultiVariant variants) {
                candidates = variants.getVariants().stream()
                        .map(net.minecraft.client.renderer.block.model.Variant::getModelLocation)
                        .toList();
            } else {
                candidates = model.getDependencies().stream()
                        .sorted(java.util.Comparator.comparing(ResourceLocation::toString))
                        .toList();
            }
            for (ResourceLocation dependency : candidates) {
                if (bakery.getModel(dependency)
                        instanceof net.minecraft.client.renderer.block.model.BlockModel child) {
                    return child;
                }
            }
            return null;
        }
    }
}
