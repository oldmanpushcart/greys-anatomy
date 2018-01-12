package com.github.ompc.greys.common.protocol;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 感谢名单
 */
public class InfoThanks extends Base {

    private Collection<Collaborator> collaborators = new ArrayList<Collaborator>();

    public Collection<Collaborator> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(Collection<Collaborator> collaborators) {
        this.collaborators = collaborators;
    }

    /**
     * 合作者
     */
    public static class Collaborator {

        private String name;
        private String email;
        private String website;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getWebsite() {
            return website;
        }

        public void setWebsite(String website) {
            this.website = website;
        }
    }

}
