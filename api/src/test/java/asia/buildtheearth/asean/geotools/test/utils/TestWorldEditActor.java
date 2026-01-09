package asia.buildtheearth.asean.geotools.test.utils;

import com.sk89q.worldedit.extension.platform.AbstractNonPlayerActor;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.formatting.text.Component;

import java.util.Locale;
import java.util.UUID;

public class TestWorldEditActor extends AbstractNonPlayerActor {

    public static final String UNIQUE_NAME ="TEST_WORLD_EDIT_ACTOR";

    /**
     * One time generated ID from {@value UNIQUE_NAME}
     */
    public static final UUID UNIQUE_ID = UUID.fromString("b905e03b-294e-38cf-8cd5-acd8249903be");

    public TestWorldEditActor() {
    }

    @Override
    public String getName() {
        return UNIQUE_NAME;
    }

    @Override
    @Deprecated
    public void printRaw(String msg) {
        System.out.println(msg);
    }

    @Override
    @Deprecated
    public void print(String msg) {
        System.out.println(msg);
    }

    @Override
    @Deprecated
    public void printDebug(String msg) {
        System.out.println(msg);
    }

    @Override
    @Deprecated
    public void printError(String msg) {
        System.err.println(msg);
    }

    @Override
    public void print(Component component) {
        System.err.println("Trying to print WorldEdit component " + component.toString());
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKey() {
            @Override
            public String getName() {
                return UNIQUE_NAME;
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public boolean isPersistent() {
                return false;
            }

            @Override
            public UUID getUniqueId() {
                return UNIQUE_ID;
            }
        };
    }

    @Override
    public UUID getUniqueId() {
        return UNIQUE_ID;
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public void checkPermission(String permission) { }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }
}

