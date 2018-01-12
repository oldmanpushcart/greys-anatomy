package com.github.ompc.greys.client.command;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingSaxParser {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SAXParser saxParser;
    private final InputStream in;
    private final List<Command> commands = new ArrayList<Command>();
    private final Map<String, String> templates;

    public MappingSaxParser(final InputStream in) throws ParserConfigurationException, SAXException {
        this(SAXParserFactory.newInstance().newSAXParser(), new HashMap<String, String>(), in);
    }

    private MappingSaxParser(final SAXParser saxParser, final Map<String, String> templates, final InputStream in) {
        this.saxParser = saxParser;
        this.templates = templates;
        this.in = in;
    }

    public List<Command> parse() throws SAXException, IOException {
        _parse();
        return commands;
    }

    private void _parseTemplateMap() throws SAXException {
        final XMLReader reader = saxParser.getXMLReader();
        reader.setContentHandler(new DefaultHandler() {

            private String template;
            private String templateId;
            private boolean isEnterTemplate = false;

            @Override
            public void startElement(final String uri,
                                     final String localName,
                                     final String qName,
                                     final Attributes attributes) {
                isEnterTemplate = StringUtils.equals(qName, "template");
                if (isEnterTemplate) {
                    templateId = attributes.getValue("id");
                }
            }

            @Override
            public void characters(final char[] ch,
                                   final int start,
                                   final int length) {
                template = isEnterTemplate
                        ? new String(ch, start, length)
                        : null;
            }

            @Override
            public void endElement(final String uri,
                                   final String localName,
                                   final String qName) {
                if (StringUtils.equals(qName, "template")) {
                    if (templates.put(templateId, template) != null) {
                        logger.warn("template[id={}] was duplicate, will be replace.", templateId);
                    } else {
                        logger.debug("template[id={}] was parse success.", templateId);
                    }
                    isEnterTemplate = false;
                    template = null;
                }
            }
        });
    }

    private void _parse() throws SAXException, IOException {

        final XMLReader reader = saxParser.getXMLReader();
        reader.setContentHandler(new DefaultHandler() {

            private Command _command;
            private Param _param;

            @Override
            public void startElement(final String uri,
                                     final String localName,
                                     final String qName,
                                     final Attributes attributes) {

            }

            @Override
            public void endElement(final String uri,
                                   final String localName,
                                   final String qName) {

            }

        });

        reader.parse(new InputSource(in));

    }


}
