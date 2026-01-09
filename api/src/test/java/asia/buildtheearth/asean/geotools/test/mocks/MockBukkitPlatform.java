package asia.buildtheearth.asean.geotools.test.mocks;

import com.sk89q.worldedit.bukkit.BukkitServerInterface;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.internal.Constants;
import org.bukkit.Server;
import org.enginehub.piston.CommandManager;

public class MockBukkitPlatform extends BukkitServerInterface {
    public MockBukkitPlatform(WorldEditPlugin plugin, Server server) {
        super(plugin, server);
    }

    /** We do not test in-game commands */
    @Override
    public void registerCommands(CommandManager dispatcher) { }

    /** We do not test in-game commands */
    @Override
    public void unregisterCommands() { }

    /**
     * Minecraft data-version, Doesn't really matter, we only use diamond block
     *
     * @return {@value Constants#DATA_VERSION_MC_1_21}
     */
    @Override
    public int getDataVersion() {
        return Constants.DATA_VERSION_MC_1_21;
    }
}
