package asia.buildtheearth.asean.geotools.projection.metadata;

import org.geotools.api.metadata.citation.*;
import org.geotools.api.util.InternationalString;
import org.geotools.metadata.iso.MetadataEntity;
import org.geotools.util.SimpleInternationalString;

public class Parties {

    public static final ResponsibleParty BTE;

    static {
        final ResponsiblePartyImpl party = new ResponsiblePartyImpl(
            Role.OWNER,
            new SimpleInternationalString("Build-The-Earth Project"),
            new ContactImpl(Resources.BTE)
        );
        party.freeze();
        BTE = party;
    }

    private static class ResponsiblePartyImpl extends MetadataEntity implements ResponsibleParty {
        private final Role role;
        private final InternationalString organisation;
        private final Contact contact;

        public ResponsiblePartyImpl(Role role, InternationalString organisation, Contact contact) {
            this.role = role;
            this.organisation = organisation;
            this.contact = contact;
        }


        @Override
        public String getIndividualName() {
            return null;
        }

        @Override
        public InternationalString getOrganisationName() {
            return organisation;
        }

        @Override
        public InternationalString getPositionName() {
            return null;
        }

        @Override
        public Contact getContactInfo() {
            return contact;
        }

        @Override
        public Role getRole() {
            return role;
        }
    }

    private static class ContactImpl extends MetadataEntity implements Contact {
        private final OnLineResource resource;

        public ContactImpl(OnLineResource resource) {
            this.resource = resource;
        }

        @Override
        public Telephone getPhone() {
            return null;
        }

        @Override
        public Address getAddress() {
            return null;
        }

        @Override
        public OnLineResource getOnLineResource() {
            return resource;
        }

        @Override
        public InternationalString getHoursOfService() {
            return null;
        }

        @Override
        public InternationalString getContactInstructions() {
            return null;
        }
    }

}
