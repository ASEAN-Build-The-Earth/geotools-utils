package asia.buildtheearth.asean.geotools.projection.metadata;

import org.geotools.api.metadata.citation.Citation;
import org.geotools.metadata.iso.citation.CitationImpl;

public class Citations {
    /**
     * The <A HREF="https://buildtheearth.net">Build-The-Earth</A> organisation.
     *
     * @see Parties#BTE
     */
    public static final Citation BTE;

    static {
        final CitationImpl cite = new CitationImpl(Parties.BTE);
        cite.freeze();
        BTE = cite;
    }
}
