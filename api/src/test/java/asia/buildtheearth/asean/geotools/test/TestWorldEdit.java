package asia.buildtheearth.asean.geotools.test;

import asia.buildtheearth.asean.geotools.test.mocks.MockBukkitPlatform;
import asia.buildtheearth.asean.geotools.test.mocks.MockWorldEditServer;
import asia.buildtheearth.asean.geotools.test.utils.TestWorldEditActor;
import asia.buildtheearth.asean.geotools.worldedit.BufferingRegionExtent;
import asia.buildtheearth.asean.geotools.worldedit.DefaultPattern;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.*;
import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@DisplayName("WorldEdit Test")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestWorldEdit implements MockWorldEditServer {
    protected static final Supplier<BuiltInClipboardFormat> TESTING_FORMAT = () -> BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC;

    protected static ServerMock server;
    protected static WorldEditPlugin worldedit;

    @Override
    public void onServerStarted(ServerMock server, WorldEditPlugin worldedit) {
        TestWorldEdit.server = server;
        TestWorldEdit.worldedit = worldedit;
    }

    @AfterAll @Override
    public void onServerStop() {
        server.getLogger().info("[TEST] ==========[ PLUGIN TEST END ]============");
        MockWorldEditServer.super.onServerStop();
    }

    @Override
    public void registerTestBlockRegistry() {
        // Add more blocks if we were to test more here
        MockWorldEditServer.super.registerTestBlockRegistry();
    }

    @Test @Order(1)
    @DisplayName("Check WorldEdit")
    public void checkPlatform() {
        PlatformManager platform = WorldEdit.getInstance().getPlatformManager();

        Assertions.assertTrue(platform.isInitialized());

        Assertions.assertEquals(1,
            platform.getPlatforms().size()
        , "Should only have one registered platform.");
        Assertions.assertInstanceOf(
            MockBukkitPlatform.class,
            platform.getPlatforms().getFirst()
        , "Platform should be created from our mocked class.");

        Assertions.assertNotNull(BlockTypes.AIR);
        Assertions.assertNotNull(BlockTypes.DIAMOND_BLOCK);
    }

    @Test @Order(2)
    @DisplayName("Write as schematic file(s)")
    public void toSchematic(@TempDir @NotNull Path tempDir) throws WorldEditException {

        BufferingRegionExtent buffer = new BufferingRegionExtent();
        AtomicBoolean extent = TestWorldEdit.subscribe(buffer);

        try(EditSession edit = WorldEdit
            .getInstance()
            .newEditSessionBuilder()
            .world(NullWorld.getInstance())
            .actor(new TestWorldEditActor())
            .build()) {

            Assertions.assertTrue(extent::get);

            edit.drawLine(new DefaultPattern(() -> BlockTypes.DIAMOND_BLOCK),
                    BlockVector3.at(0, 0, 0),
                    BlockVector3.at(15, 0, 15),
                    0, false
            );

            // TODO: Test with our reader library
            // @SuppressWarnings("deprecation")
            // WorldEditGeometryWriter writer = new WorldEditGeometryWriter(edit, MinecraftProjection.ASEAN_BTE_PROJECTION)
            //     .fillGeometry()
            //     .setWritingSize(0);
            // AbstractTestFromKML.finalGeo.forEach((key, geometry) -> {
            //     Assertions.assertDoesNotThrow(() -> writer.writeGeometry(geometry), "Should write geometries");
            // });
        }
        catch (Exception ex) {
            Assertions.fail("Edit Session unsuccessful", ex);
        }

        System.out.println(buffer.getBuffer().size());

        Region region = buffer.asRegion();

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region) ;

        buffer.getBuffer().forEach(clipboard::setBlock);
        clipboard.setOrigin(BlockVector3.ZERO);
        Operations.complete(clipboard.commit());

        System.out.println(clipboard.getDimensions());

        BuiltInClipboardFormat format = Assertions.assertDoesNotThrow(TESTING_FORMAT::get);
        File schematic = tempDir.resolve("test." + format.getPrimaryFileExtension()).toFile();

        try (ClipboardWriter writer = format.getWriter(new FileOutputStream(schematic))) {
            writer.write(clipboard);
        }
        catch (IOException ex) { Assertions.fail("Failed to write schematic", ex); }

        try(ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
           Clipboard output = reader.read();

           System.out.println(output);

        }
        catch (IOException ex) { Assertions.fail("Failed to read schematic", ex); }
    }

    public static @NotNull AtomicBoolean subscribe(BufferingRegionExtent buffer) {
        AtomicBoolean extent = new AtomicBoolean(false);

        WorldEdit.getInstance().getEventBus().register(new EventListener() {
            @Subscribe @SuppressWarnings("unused")
            public void onEditSessionEvent(EditSessionEvent event) {
                // Actor should only ever be our test instance.
                Assertions.assertNotNull(event.getActor());
                Assertions.assertEquals(TestWorldEditActor.UNIQUE_ID, event.getActor().getUniqueId());

                if(event.getStage() == EditSession.Stage.BEFORE_CHANGE) {
                    event.setExtent(buffer);
                    extent.set(true);
                }
            }
        });

        return extent;
    }
}
