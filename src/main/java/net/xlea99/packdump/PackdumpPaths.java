package net.xlea99.packdump;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;

public final class PackdumpPaths {
    private PackdumpPaths() {}

    /** packdump root: <gameDir>/<outputDir>/ */
    public static Path packdumpRoot() {
        return FMLPaths.GAMEDIR.get().resolve(PackdumpConfig.outputDir);
    }

    /** Registry dumps (flat ID lists): dumps/registries/ */
    public static Path registriesDir() {
        return packdumpRoot().resolve("registries");
    }

    /** Attribute dumps (entry properties like localization): dumps/attributes/ */
    public static Path attributesDir() {
        return packdumpRoot().resolve("attributes");
    }
}
