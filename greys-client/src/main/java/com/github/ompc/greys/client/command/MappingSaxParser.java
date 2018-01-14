package com.github.ompc.greys.client.command;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import static com.github.ompc.greys.client.command.MappingSaxParser.Element.*;

/**
 * 命令映射配置文件解析器
 */
public class MappingSaxParser {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String MAPPING_RES = "/com/github/ompc/greys/client/res/command-http-mappings.xml";
    private final SAXParserFactory saxParserFactory;
    private final SAXParser saxParser;
    private final InputStream in;

    // 解析出来的命令集
    private final Map<String, Command> commands = new LinkedHashMap<String, Command>();

    // 解析出来的模版元素
    private final Map<String, String> templates;

    public MappingSaxParser() throws ParserConfigurationException, SAXException {
        this(
                SAXParserFactory.newInstance(),
                MappingSaxParser.class.getResourceAsStream(MAPPING_RES),
                new HashMap<String, String>()
        );
    }

    private MappingSaxParser(final SAXParserFactory saxParserFactory,
                             final InputStream in,
                             final Map<String, String> templates) throws ParserConfigurationException, SAXException {
        this.saxParserFactory = saxParserFactory;
        this.saxParser = saxParserFactory.newSAXParser();
        this.in = in;
        this.templates = templates;
    }

    /**
     * 根据映射文件解析出对应的命令对象
     *
     * @return 命令对象集合
     */
    public Map<String, Command> parse() throws SAXException, IOException {
        _parse();
        return commands;
    }

    /**
     * 需要处理的XML元素
     */
    enum Element {

        COMMAND,
        PARAM,
        TEMPLATE,
        IMPORT,
        EXAMPLE,
        NO_NEED_DEAL;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        // 判断qName是否当前需要处理的元素
        boolean is(String qName) {
            return StringUtils.equals(qName, toString());
        }

        // 将qName转换为需要处理的元素
        static Element to(String qName) {
            for (Element element : Element.values()) {
                if (element.is(qName)) {
                    return element;
                }
            }
            return NO_NEED_DEAL;
        }
    }

    // 解析XML
    private void _parse() throws SAXException, IOException {
        final XMLReader reader = saxParser.getXMLReader();
        reader.setContentHandler(new DefaultHandler() {

            Stack<Element> stack = new Stack<Element>();

            Command _command;

            Param _param;
            String _paramDesc;

            String _templateId;
            String _example;

            @Override
            public void startElement(final String uri,
                                     final String localName,
                                     final String qName,
                                     final Attributes attributes) throws SAXException {

                final Element element = stack.push(Element.to(qName));
                switch (element) {
                    case COMMAND:
                        _command = newCommand(attributes);
                        break;
                    case PARAM:
                        _param = newParam(attributes);
                        break;
                    case TEMPLATE:
                        _templateId = attributes.getValue("id");
                        break;
                    case IMPORT:
                        final String importPath = attributes.getValue("path");
                        final InputStream importIn = getClass().getResourceAsStream(importPath);
                        if (null == importIn) {
                            logger.warn("import path: {}, was not existed, ignore this import.", importPath);
                        } else {
                            try {
                                commands.putAll(new MappingSaxParser(saxParserFactory, importIn, templates).parse());
                            } catch (Exception cause) {
                                throw new SAXException(cause);
                            } finally {
                                IOUtils.closeQuietly(importIn);
                            }
                        }
                        break;
                }

            }

            @Override
            public void characters(final char[] ch,
                                   final int start,
                                   final int length) {
                if (EXAMPLE != stack.peek()
                        && PARAM != stack.peek()
                        && TEMPLATE != stack.peek()) {
                    return;
                }
                final String string = new String(ch, start, length).trim();
                if (StringUtils.isBlank(string)) {
                    return;
                }
                switch (stack.peek()) {
                    case TEMPLATE:
                        templates.put(_templateId, string);
                        break;
                    case PARAM:
                        _paramDesc = string;
                        break;
                    case EXAMPLE:
                        _example = string;
                        break;
                }
            }

            @Override
            public void endElement(final String uri,
                                   final String localName,
                                   final String qName) {
                final Element element = stack.pop();
                switch (element) {
                    case EXAMPLE:
                        _command.setExample(_example);
                        break;
                    case PARAM:
                        _param.setDesc(_paramDesc);
                        switch (_param.getType()) {
                            case INDEX:
                                _command.getIndexParams().add(_param);
                                break;
                            case NAMED:
                                _command.getNamedParams().put(_param.getName(), _param);
                                break;
                        }
                        break;
                    case COMMAND:
                        commands.put(_command.getName(), _command);
                        logger.info("parse mapping command[name={};index={};named={};http={};] success.",
                                _command.getName(),
                                _command.getIndexParams().size(),
                                _command.getNamedParams().size(),
                                _command.getHttpPath()
                        );
                        break;
                }
            }

            private Command newCommand(final Attributes attr) {
                final Command newCommand = new Command();
                newCommand.setName(attr.getValue("name"));
                newCommand.setHttpPath(attr.getValue("http-path"));
                newCommand.setSummary(attr.getValue("summary"));

                {
                    // 设置SUMMARY-REF
                    final String templateId = attr.getValue("summary-ref");
                    if (StringUtils.isNotBlank(templateId)
                            && templates.containsKey(templateId)) {
                        newCommand.setSummary(templates.get(templateId));
                    }
                }

                return newCommand;
            }

            private Param newParam(final Attributes attr) {
                final Param param = new Param();
                param.setHasValue(BooleanUtils.toBoolean(attr.getValue("has-value")));
                param.setRequired(BooleanUtils.toBoolean(attr.getValue("required")));
                param.setName(attr.getValue("name"));
                param.setHttpName(attr.getValue("http-name"));
                param.setHttpValue(attr.getValue("http-value"));
                param.setSummary(attr.getValue("summary"));

                // 设置TYPE
                final String typeString = attr.getValue("type");
                if (StringUtils.equals(typeString, "index")) {
                    param.setType(Param.Type.INDEX);
                } else if (StringUtils.equals(typeString, "named")) {
                    param.setType(Param.Type.NAMED);
                } else {
                    throw new IllegalStateException("unknow param.type: " + typeString);
                }

                // 设置DESC-REF
                {
                    final String templateId = attr.getValue("desc-ref");
                    if (StringUtils.isNotBlank(templateId)
                            && templates.containsKey(templateId)) {
                        _paramDesc = templates.get(templateId);
                    }
                }

                {
                    // 设置SUMMARY-REF
                    final String templateId = attr.getValue("summary-ref");
                    if (StringUtils.isNotBlank(templateId)
                            && templates.containsKey(templateId)) {
                        param.setSummary(templates.get(templateId));
                    }
                }

                return param;
            }

        });
        reader.parse(new InputSource(in));
    }

}
