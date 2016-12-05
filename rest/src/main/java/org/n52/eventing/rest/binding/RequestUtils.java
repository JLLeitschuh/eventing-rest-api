/*
 * Copyright (C) 2016-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package org.n52.eventing.rest.binding;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class RequestUtils {

    public static String resolveFullRequestUrl() throws IOException, URISyntaxException {
        HttpServletRequest request = resolveRequestObject();

        URL url = new URL(request.getRequestURL().toString());

        String scheme = url.getProtocol();
        String userInfo = url.getUserInfo();
        String host  = url.getHost();

        int port = url.getPort();

        String path = request.getRequestURI();
        if (path != null && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
//        String query = request.getQueryString();

        URI uri = new URI(scheme, userInfo, host, port, path, null, null);
        return uri.toString();
    }

    public static HttpServletRequest resolveRequestObject() {
        return ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    public static HttpServletResponse resolveResponseObject() {
        return ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getResponse();
    }

}
