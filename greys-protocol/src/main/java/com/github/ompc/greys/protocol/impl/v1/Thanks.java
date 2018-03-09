package com.github.ompc.greys.protocol.impl.v1;

import com.github.ompc.greys.protocol.Gp;
import com.github.ompc.greys.protocol.GpType;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 感谢名单
 */
@Data
@Gp(GpType.THANKS)
public class Thanks {

    private final Collection<Collaborator> collaborators = new ArrayList<Collaborator>();

    /**
     * 合作者
     */
    @Data
    public static class Collaborator {
        private final String name;
        private final String email;
        private final String website;
    }

}
