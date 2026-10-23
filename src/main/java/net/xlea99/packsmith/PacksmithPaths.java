package net.xlea99.packsmith;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;

public final class PacksmithPaths {
    private PacksmithPaths() {}

    /** Packsmith root: <gameDir>/<outputDir>/ */
    public static Path packsmithRoot() {
        return FMLPaths.GAMEDIR.get().resolve(PacksmithConfig.outputDir);
    }

    /** Registry dumps (flat ID lists): dumps/registries/ */
    public static Path registriesDir() {
        return packsmithRoot().resolve("registries");
    }

    /** Attribute dumps (entry properties like localization): dumps/attributes/ */
    public static Path attributesDir() {
        return packsmithRoot().resolve("attributes");
    }
}
