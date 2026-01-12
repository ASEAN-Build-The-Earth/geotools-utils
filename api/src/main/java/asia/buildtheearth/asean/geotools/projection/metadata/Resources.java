package asia.buildtheearth.asean.geotools.projection.metadata;

import org.geotools.api.metadata.citation.OnLineFunction;
import org.geotools.api.metadata.citation.OnLineResource;
import org.geotools.api.util.InternationalString;
import org.geotools.metadata.iso.MetadataEntity;

import java.net.URI;

public class Resources {

    public static final OnLineResource BTE;

    static {
        final OnLineResourceImpl resource;
        BTE = resource = new OnLineResourceImpl(URI.create("https://buildtheearth.net"));
        resource.freeze();
    }

    private static class OnLineResourceImpl extends MetadataEntity implements OnLineResource {

        private URI linkage;

        public OnLineResourceImpl(final URI linkage) {
            setLinkage(linkage);
        }

        @Override
        public URI getLinkage() {
            return linkage;
        }

        public void setLinkage(final URI newValue) {
            checkWritePermission();
            linkage = newValue;
        }

        @Override
        public String getProtocol() {
            final URI linkage = this.linkage;
            return linkage != null ? linkage.getScheme() : null;
        }

        @Override
        public String getApplicationProfile() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public InternationalString getDescription() {
            return null;
        }

        @Override
        public OnLineFunction getFunction() {
            return null;
        }
    }
}
