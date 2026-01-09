package asia.buildtheearth.asean.geotools.test.mocks;

import com.google.common.collect.ImmutableList;
import com.sk89q.bukkit.util.ClassSourceValidator;
import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditListener;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.event.platform.PlatformUnreadyEvent;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.world.World;
import io.papermc.lib.PaperLib;

public class MockWorldEditPlugin extends WorldEditPlugin {

    private MockBukkitPlatform mockPlatform;

    @Override
    public void onLoad() {
        super.onLoad(); // Load Defaults

        WorldEdit worldEdit = WorldEdit.getInstance();

        // Unregister Loaded platform (we need it register as mocked)
        for(Platform platform : worldEdit.getPlatformManager().getPlatforms())
            worldEdit.getPlatformManager().unregister(platform);

        // Re-register platform as our mocked one
        this.mockPlatform = new MockBukkitPlatform(this, this.getServer());
        worldEdit.getPlatformManager().register(this.mockPlatform);
    }

    @Override
    public void onEnable() {
        ClassSourceValidator verifier = new ClassSourceValidator(this);
        verifier.reportMismatches(ImmutableList.of(World.class, EditSession.class, Actor.class));
        WorldEdit.getInstance().getEventBus().post(new PlatformsRegisteredEvent());
        PermissionsResolverManager.initialize(this);
        this.getServer().getPluginManager().registerEvents(new WorldEditListener(this), this);
        PaperLib.suggestPaper(this);
        WorldEdit.getInstance().loadMappings();
    }

    @Override
    public void onDisable() {
        WorldEdit worldEdit = WorldEdit.getInstance();
        worldEdit.getSessionManager().unload();
        if (this.mockPlatform != null) {
            worldEdit.getEventBus().post(new PlatformUnreadyEvent(this.mockPlatform));
            worldEdit.getPlatformManager().unregister(this.mockPlatform);
            this.mockPlatform.unregisterCommands();
        }

        if (getLocalConfiguration() != null) getLocalConfiguration().unload();

        this.getServer().getScheduler().cancelTasks(this);
    }
}
