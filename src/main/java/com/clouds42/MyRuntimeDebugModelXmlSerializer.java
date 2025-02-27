/*
 * This file is a part of Coverage41C.
 *
 * Copyright (c) 2020-2021
 * Kosolapov Stanislav aka proDOOMman <prodoomman@gmail.com> and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * Coverage41C is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * Coverage41C is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Coverage41C.
 */
package com.clouds42;

import com._1c.g5.v8.dt.internal.debug.core.runtime.client.RuntimeDebugModelXmlSerializer;
import com._1c.g5.v8.dt.internal.debug.core.runtime.client.RuntimeExtendedMetaData;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class MyRuntimeDebugModelXmlSerializer extends RuntimeDebugModelXmlSerializer {
    private static final String TRACE_OPTION = "/trace/dbgs/responses";
    private static final Charset ENCODING;
    private static final String TAG_OPENING = "<";
    private static final String TAG_CLOSING = "/>";
    private static final String RUNTIME_REQUEST_XML_TAG = "request";
    private static final String RUNTIME_RESPONSE_XML_TAG = "response";
    private static final String RUNTIME_RESPONSE_XML_SCHEME_NAME = "debugRDBGRequestResponse";
    private static final String CLASS_NAME_IMPL_PART = "Impl$";
    private static final String DELIMITER = ":";

    static {
        ENCODING = StandardCharsets.UTF_8;
    }

    public MyRuntimeDebugModelXmlSerializer() {
    }

    @Override
    public String serialize(EObject serializeFrom) throws IOException {
        Preconditions.checkArgument(serializeFrom != null);
        StringBuilder replaceFrom = new StringBuilder();
        replaceFrom.append("response");
        replaceFrom.append(":");
        replaceFrom.append(serializeFrom.getClass().getSimpleName().replaceAll("Impl$", ""));
        String replace = replaceFrom.toString();
        String xml = this.convertToXml(serializeFrom).trim();
        String result = '\ufeff' + this.replaceRootElement(xml, replace, "request");

        return result;
    }

    @Override
    public <T extends EObject> T deserialize(String xmlString, Class<T> deserializeTo) throws IOException {
        Preconditions.checkArgument(xmlString != null);
        Preconditions.checkArgument(deserializeTo != null);
        StringBuilder replaceTo = new StringBuilder();
        replaceTo.append("debugRDBGRequestResponse");
        replaceTo.append(":");
        replaceTo.append(deserializeTo.getSimpleName());
        return this.deserialize(xmlString, deserializeTo, "response", replaceTo.toString());
    }

    @Override
    public <T extends EObject> T deserialize(String xmlString, Class<T> deserializeTo, String replaceFrom, String replaceTo) throws IOException {
        Preconditions.checkArgument(xmlString != null);
        Preconditions.checkArgument(deserializeTo != null);
        Preconditions.checkArgument(replaceFrom != null);
        Preconditions.checkArgument(replaceTo != null);
        xmlString = this.removeUtf8Bom(xmlString);
        String replacedXML = this.replaceRootElement(xmlString, replaceFrom, replaceTo);
        T result = (T) this.convertToEObject(replacedXML);
        return result;
    }

    protected EObject convertToEObject(String xmlString) throws IOException {
        Map<Object, Object> loadOptions = new HashMap();
        loadOptions.put("EXTENDED_META_DATA", true);
        loadOptions.put("LAX_FEATURE_PROCESSING", true);
        XMLResourceImpl resource = new XMLResourceImpl();
        resource.setEncoding(ENCODING.name());
        resource.load(new InputSource(new StringReader(xmlString)), loadOptions);
        return (EObject)resource.getContents().get(0);
    }

    protected String convertToXml(EObject eObject) throws IOException {
        XMLResourceImpl resource = new XMLResourceImpl();
        resource.setEncoding(ENCODING.name());
        resource.getContents().add(eObject);
        Map<Object, Object> saveOptions = new HashMap();
        saveOptions.put("EXTENDED_META_DATA", new RuntimeExtendedMetaData());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        resource.save(outputStream, saveOptions);
        return new String(outputStream.toByteArray(), ENCODING);
    }

    protected String replaceRootElement(String xmlString, String replaceFromTag, String replaceToTag) {

        xmlString = xmlString.replaceFirst("<" + replaceFromTag, "<" + replaceToTag);
        if (!xmlString.endsWith("/>")) {
            StringBuilder builder = new StringBuilder(xmlString);
            int replaceingIndex = xmlString.lastIndexOf(replaceFromTag);
            if (replaceingIndex != -1) {
                builder.replace(replaceingIndex, replaceingIndex + replaceFromTag.length(), replaceToTag);
            }

            xmlString = builder.toString();
        }

        return xmlString;
    }

    protected String removeUtf8Bom(String string) {
        return string.startsWith(String.valueOf('\ufeff')) ? string.substring(1) : string;
    }
}
