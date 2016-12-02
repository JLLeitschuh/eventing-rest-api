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

package org.n52.eventing.wv.dao.hibernate;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.n52.eventing.rest.Pagination;
import org.n52.eventing.wv.dao.EventDao;
import org.n52.eventing.wv.model.WvEvent;
import org.n52.eventing.wv.model.WvSubscription;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class HibernateEventDao extends BaseHibernateDao<WvEvent> implements EventDao {

    public HibernateEventDao(Session session) {
        super(session);
    }

    @Override
    public List<WvEvent> retrieveForSubscription(int idInt) {
        return retrieveForSubscription(idInt, null);
    }

    @Override
    public List<WvEvent> retrieveForSubscription(int idInt, Pagination pagination) {
        Session s = getSession();
        String param = "subId";
        String eventEntity = WvEvent.class.getSimpleName();
        String subEntity = WvSubscription.class.getSimpleName();
        String hql = String.format("SELECT e FROM %s e join e.rule r, %s s WHERE s.id=:%s AND s.rule = r order by e.id asc", eventEntity, subEntity, param);
        Query query = getSession().createQuery(hql);

        if (pagination != null) {
            query.setFirstResult(pagination.getOffset());
            query.setMaxResults(pagination.getLimit());
        }

        query.setParameter(param, idInt);
        return query.list();
    }



}
