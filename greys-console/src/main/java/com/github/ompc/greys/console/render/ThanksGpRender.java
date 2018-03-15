package com.github.ompc.greys.console.render;

import com.github.ompc.greys.protocol.GreysProtocol;
import com.github.ompc.greys.protocol.impl.v1.Thanks;
import com.github.ompc.greys.protocol.impl.v1.Thanks.Collaborator;
import org.crsh.text.Color;
import org.crsh.text.ui.LabelElement;
import org.crsh.text.ui.TableElement;

import static com.github.ompc.greys.console.textui.UIBuilder.*;
import static com.github.ompc.greys.protocol.GpType.THANKS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.crsh.text.Color.*;
import static org.crsh.text.Style.style;
import static org.crsh.text.Style.reset;

public class ThanksGpRender implements GpRender {

    @Override
    public String rendering(GreysProtocol<?> gp) {
        if (gp.getType() != THANKS) {
            return EMPTY;
        }
        final StringBuilder buffer = new StringBuilder();
        final Thanks thanks = (Thanks) gp.getContent();
        for (Collaborator collaborator : thanks.getCollaborators()) {
            buffer.append(collaboratorCard(collaborator)).append("\n");
        }
        return buffer.toString();
    }

    private String collaboratorCard(Collaborator collaborator) {

        final TableElement table = table(8, 4, 30)
                .leftCellPadding(0)
                .rightCellPadding(0)
                .border(null)
                .collapse()
                .style(style())
                .row(name(collaborator.getName()));
        if (isNotBlank(collaborator.getEmail())) {
            table.row(email(collaborator.getEmail()));
        }
        if (isNotBlank(collaborator.getWebsite())) {
            table.row(website(collaborator.getWebsite()));
        }

        return rending(40, table);
    }

    private static LabelElement[] name(String name) {
        return kv("    NAME", name, red);
    }

    private static LabelElement[] email(String email) {
        return kv("   EMAIL", email, blue);
    }

    private static LabelElement[] website(String website) {
        return kv(" WEBSITE", website, blue);
    }

    private static LabelElement[] kv(String key, String value, Color valueColor) {
        return new LabelElement[]{
                label(key).style(style().bold().bg(white)),
                label(" :").style(style().bold()),
                label(value).style(style().bold().underline().fg(valueColor))
        };
    }

}
