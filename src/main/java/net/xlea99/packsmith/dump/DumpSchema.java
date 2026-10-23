package net.xlea99.packsmith.dump;

/**
 * The version stamped on every file a dump writes.
 *
 * <p>One constant rather than a literal per service: meta.json, the registry files and the
 * localization files each carried their own hardcoded {@code 1}, which is three places to
 * remember and two chances to forget. A dump whose files disagree about their own version is
 * worse than one that is simply old — the reader has no way to tell which file is lying.
 *
 * <p><b>Bump this whenever the shape of any dumped file changes.</b> Packsmith reads older
 * versions forever: a snapshot on disk is an archive, not a cache, and you cannot go back
 * and re-dump a pack as it was six months ago. Additive changes are therefore the strongly
 * preferred kind — a new block beside the old one, so an older reader ignores it and a newer
 * reader can tell it was absent rather than empty.
 *
 * <p>History:
 * <ul>
 *   <li><b>1</b> — initial: meta, registries, localization display names.</li>
 *   <li><b>2</b> — localization gained {@code keys}: the translation key behind each
 *       display name, which is what a resource pack needs in order to override it.</li>
 *   <li><b>3</b> — {@code block_items.json}: which items place a block, which block each
 *       places, and that block's form and Java class. Two guesses available from the
 *       registries alone disagreed on 114 items of a 300-mod pack.</li>
 *   <li><b>4</b> — {@code textures.json}: the texture each block actually renders with,
 *       read from the baked model so resource packs are already applied.
 *       {@code <namespace>:block/<path>} is a convention that mods break freely, and a
 *       generated block cut from a foreign base has to name its texture exactly.
 *       Client-only, and absent rather than empty when dumped without one.</li>
 *   <li><b>5</b> — {@code textures.json} gained {@code requested}: the texture name each
 *       block's model DECLARES, read from the unbaked model. Schema 4 answers "what does
 *       this render as" and reports {@code minecraft:missingno} for art nobody has drawn —
 *       true, and useless for drawing it, because the substitution happens at bake time and
 *       takes the wanted name with it. This is that name, which is where the PNG belongs.</li>
 * </ul>
 */
public final class DumpSchema {

    public static final int VERSION = 5;

    private DumpSchema() {}
}
