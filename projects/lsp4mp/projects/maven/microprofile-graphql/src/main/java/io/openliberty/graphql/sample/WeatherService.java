/******************************************************************************
* Copyright (c) 2019 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html

* Contributors:
* IBM Corporation - initial API and implementation
******************************************************************************/

package io.openliberty.graphql.sample;

import io.smallrye.graphql.api.Subscription;

import java.util.concurrent.Flow;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.GraphQLException;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

@GraphQLApi
@ApplicationScoped
public class WeatherService {

    Map<String, Conditions> currentConditionsMap = new HashMap<>();


    @Query
    @Optimistic
    public Conditions currentConditions(@Name("location") String location) throws UnknownLocationException {
        if ("nowhere".equalsIgnoreCase(location)) {
            throw new UnknownLocationException(location);
        }
        return currentConditionsMap.computeIfAbsent(location, this::randomWeatherConditions);
    }

    @DenyAll
    @Query
    public List<Conditions> currentConditionsList(@Optimistic @Name("locations") List<String> locations)
        throws UnknownLocationException, GraphQLException {

        List<Conditions> allConditions = new LinkedList<>();
        for (String location : locations) {
            try {
                allConditions.add(currentConditions(location));
            } catch (UnknownLocationException ule) {
                throw new GraphQLException(ule, allConditions);
            }
        }
        return allConditions;
    }

    @RolesAllowed("Role2")
    @Mutation
    @Description("Reset the cached conditions so that new queries will return newly randomized weather data." +
                 "Returns number of entries cleared.")
    public int reset() {
        int cleared = currentConditionsMap.size();
        currentConditionsMap.clear();
        return cleared;
    }

    public double wetBulbTempF(@Source @Name("conditions") @Optimistic Conditions conditions) {
        // TODO: pretend like this is a really expensive operation
        System.out.println("wetBulbTempF for location " + conditions.getLocation());
        return conditions.getTemperatureF() - 3.0;
    }

    private Conditions randomWeatherConditions(String location) {
        Conditions c = new Conditions(location);
        c.setDayTime(Math.random() > 0.5);
        c.setTemperatureF(Math.random() * 100);
        c.setTemperatureC( (c.getTemperatureF() - 30) / 2 );
        c.setHasPrecipitation(Math.random() > 0.7);
        c.setPrecipitationType(c.isHasPrecipitation() ? PrecipType.fromTempF(c.getTemperatureF()) : null);
        c.setWeatherText(c.isHasPrecipitation() ? "Overcast" : "Sunny");
        return c;
    }

    @Query
    public void myMethod() {
    }

    @Mutation
    public void myOtherMethod() {
    }

    @Subscription
    public String subscriptionReturningNonMulti() {
        return null;
    }

    @Query
    public Flow.Publisher<String> queryReturningMulti() {
        return null;
    }

}
