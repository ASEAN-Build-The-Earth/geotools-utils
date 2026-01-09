package asia.buildtheearth.asean.geotools.test.mocks;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Interface to mocks minecraft server lifecycle.
 *
 * @see #onServerMock()
 * @see #onServerStarted(ServerMock, WorldEditPlugin)
 * @see #onServerStop()
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public interface MockWorldEditServer {
    String WORLD_EDIT_SYMBOL = "WorldEdit", WORLD_EDIT_VERSION = "7.3.18";

    /**
     * Register all blocks we will be testing to WorldEdit registry.
     */
    default void registerTestBlockRegistry() {
        BlockType.REGISTRY.register("minecraft:diamond_block", new BlockType("minecraft:diamond_block"));
        BlockType.REGISTRY.register("minecraft:air", new BlockType("minecraft:air"));
    }

    /**
     * Invoked before every test triggering server start event with server instance and its initialized plugins.
     *
     * @see #onServerStarted(ServerMock, WorldEditPlugin)
     */
    @BeforeAll
    default void onServerMock() {
        String classLoader = JavaPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        this.registerTestBlockRegistry();

        Assertions.assertTrue(
            classLoader.matches(".*/io/papermc/paper/paper-api/.*"),
            "Expected 'JavaPlugin' to be loaded from 'io.papermc.paper:paper-api' artifact,\n" +
                    "but it was loaded from: " + classLoader + "\nPossible cause: " +
                    "spigot-api is not excluded in test scope and overrides paper-api due to classpath order."
        );

        // Initialize a mocks server
        ServerMock server = Assertions.assertDoesNotThrow((ThrowingSupplier<ServerMock>) MockBukkit::mock,
                "MockBukkit should be able to mocks the server using papermc-api"
        );

        Assertions.assertDoesNotThrow(() -> server.getLogger().info("[TEST] MockBukkit Server has started."));

        // DiscordSRV should exist in the plugin manager
        WorldEditPlugin worldedit = Assertions.assertDoesNotThrow(() -> {
            server.getLogger().info("[TEST] ==========[ MOCK WorldEdit START ]==========");
            server.getLogger().info("[TEST] Expecting a non-fatal error from WorldEdit. This is normal.");

            // Load WorldEditPlugin plugin as a mocks, should throw non-fatal exception on instantiation
            PluginDescriptionFile info = new PluginDescriptionFile(WORLD_EDIT_SYMBOL, WORLD_EDIT_VERSION, WorldEditPlugin.class.getName());
            WorldEditPlugin plugin = MockBukkit.loadWith(MockWorldEditPlugin.class, info);

            server.getLogger().info("[TEST] ==========[ MOCK WorldEdit END ]============");

            return plugin;
        }, "Creating WorldEditPlugin mocks instance should not throw fatal exception.");


        this.onServerStarted(server, worldedit);
    }

    /**
     * Invoked after all tests un-mocking the mocks-bukkit server by default.
     *
     * @see AfterAll
     * @see org.mockbukkit.mockbukkit.MockBukkit#unmock
     */
    @AfterAll
    default void onServerStop() {
        MockBukkit.unmock();
    }

    /**
     * Invoked on server started and all its instance is validated.
     *
     * @param server The {@link ServerMock} instance
     */
    void onServerStarted(ServerMock server, WorldEditPlugin worldedit);
}
