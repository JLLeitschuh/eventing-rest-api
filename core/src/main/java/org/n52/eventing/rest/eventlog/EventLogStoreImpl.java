/*
 * Copyright (C) 2016-2019 52°North Initiative for Geospatial Open Source
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
package org.n52.eventing.rest.eventlog;

import org.n52.eventing.rest.model.EventHolder;
import com.google.common.collect.EvictingQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.n52.eventing.rest.QueryResult;
import org.n52.eventing.rest.RequestContext;
import org.n52.eventing.rest.model.Subscription;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class EventLogStoreImpl implements EventLogStore {

    private final Map<Subscription, Collection<EventHolder>> internalStore = new HashMap<>();
    private final Map<Subscription, Object> mutexes = new HashMap<>();

    @Override
    public void addEvent(Subscription subscription, EventHolder eh, int maxCapacity) {
        Collection<EventHolder> targetList;
        synchronized (this) {
            if (!this.internalStore.containsKey(subscription)) {
                targetList = EvictingQueue.create(maxCapacity);
                this.internalStore.put(subscription, targetList);
                this.mutexes.put(subscription, new Object());
            }
            else {
                targetList = this.internalStore.get(subscription);
            }
        }

        synchronized (this.mutexes.get(subscription)) {
            targetList.add(eh);
        }
    }

    @Override
    public QueryResult<EventHolder> getAllEvents() {
        List<EventHolder> result = new ArrayList<>();

        synchronized (this) {
            this.internalStore.values().forEach(coll -> {
                result.addAll(coll);
            });
        }

        return new QueryResult<>(result, result.size());
    }

    @Override
    public QueryResult<EventHolder> getEventsForSubscription(Subscription sub) {
        synchronized (this) {
            if (!this.internalStore.containsKey(sub)) {
                return new QueryResult<>(Collections.emptyList(), 0);
            }
            List<EventHolder> data = new ArrayList(this.internalStore.get(sub));
            return new QueryResult<>(data, data.size());
        }
    }

    @Override
    public Optional<EventHolder> getSingleEvent(String eventId, RequestContext context) {
        return getAllEvents().getResult().stream()
                .filter(eh -> eh.getId().equals(eventId))
                .findFirst();
    }

}
