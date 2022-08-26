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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Conditions {

    private final String location;
    private final LocalDateTime localObservationDateTime = LocalDateTime.now();
    private String weatherText;
    private boolean hasPrecipitation;
    private PrecipType precipitationType;
    private boolean dayTime;
    private double temperatureC;
    private double temperatureF;

    public Conditions(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getLocalObservationDateTime() {
        return localObservationDateTime;
    }

    public long getEpochTime() {
        return localObservationDateTime.toEpochSecond(ZoneOffset.UTC);
    }

    public String getWeatherText() {
        return weatherText;
    }

    public void setWeatherText(String weatherText) {
        this.weatherText = weatherText;
    }

    public boolean isHasPrecipitation() {
        return hasPrecipitation;
    }

    public void setHasPrecipitation(boolean hasPrecipitation) {
        this.hasPrecipitation = hasPrecipitation;
    }

    public PrecipType getPrecipitationType() {
        return precipitationType;
    }

    public void setPrecipitationType(PrecipType precipitationType) {
        this.precipitationType = precipitationType;
    }

    public boolean isDayTime() {
        return dayTime;
    }

    public void setDayTime(boolean dayTime) {
        this.dayTime = dayTime;
    }

    public double getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(double temperatureC) {
        this.temperatureC = temperatureC;
    }

    public double getTemperatureF() {
        return temperatureF;
    }

    public void setTemperatureF(double temperatureF) {
        this.temperatureF = temperatureF;
    }

}
