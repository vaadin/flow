/*
 * Copyright 2000-2019 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.starter.skeleton;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import akka.actor.Actor;
import akka.actor.ActorSelection;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.Multimap;
import com.google.inject.Injector;
import org.hibernate.Hibernate;
import org.hibernate.cfg.JPAIndexHolder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQL5Dialect;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.html.Div;

@Route("")
public class StartupPerformance extends Div {
    // Reference classes in various libraries. If bytecode scanning is in use,
    // visiting these classes is expected to be quite slow.
    Hibernate hibernate;
    EntityManager entityManager;
    TransactionManager trsManager;
    JPAIndexHolder jpa;
    H2Dialect h2Dialect;
    MySQL5Dialect mysqlDialect;
    Multimap<String,String> mmap;
    Injector injector;
    MetricRegistry registry;
    MetricSet metricSet;
    ConsoleReporter reporter;
    Gauge gauge;
    Actor actor;
    ActorSelection actorSelection;
}
