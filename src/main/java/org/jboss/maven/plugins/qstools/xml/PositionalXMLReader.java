/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.maven.plugins.qstools.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Rafael Benevides
 * 
 */
public class PositionalXMLReader {

    public final static String LINE_NUMBER_KEY_NAME = "lineNumber";

    public static Document readXML(final InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final Stack<Element> elementStack = new Stack<Element>();
        final StringBuilder textBuffer = new StringBuilder();
        final DefaultHandler handler = new DefaultHandler() {
            private Locator locator;

            @Override
            public void setDocumentLocator(final Locator locator) {
                this.locator = locator; // Save the locator, so that it can be used later for line tracking when traversing
                                        // nodes.
            }

            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
                addTextIfNeeded();
                final Element el = doc.createElement(qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    el.setAttribute(attributes.getQName(i), attributes.getValue(i));
                }
                el.setUserData(LINE_NUMBER_KEY_NAME, String.valueOf(this.locator.getLineNumber()), null);
                elementStack.push(el);
            }

            @Override
            public void endElement(final String uri, final String localName, final String qName) {
                addTextIfNeeded();
                final Element closedEl = elementStack.pop();
                if (elementStack.isEmpty()) { // Is this the root element?
                    doc.appendChild(closedEl);
                } else {
                    final Element parentEl = elementStack.peek();
                    parentEl.appendChild(closedEl);
                }
            }

            @Override
            public void characters(final char ch[], final int start, final int length) throws SAXException {
                textBuffer.append(ch, start, length);
            }

            // Outputs text accumulated under the current node
            private void addTextIfNeeded() {
                if (textBuffer.length() > 0) {
                    final Element el = elementStack.peek();
                    final Node textNode = doc.createTextNode(textBuffer.toString());
                    el.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }
            }
        };
        try {
            parser.parse(is, handler);
        } finally {
            is.close();
        }

        return doc;
    }
}
