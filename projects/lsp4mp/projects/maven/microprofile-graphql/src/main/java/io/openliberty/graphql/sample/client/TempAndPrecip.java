/******************************************************************************
* Copyright (c) 2020 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html

* Contributors:
* IBM Corporation - initial API and implementation
******************************************************************************/
package io.openliberty.graphql.sample.client;


public class TempAndPrecip {

    private double temperatureF;
    private boolean hasPrecipitation;

    public double getTemperatureF() {
        return temperatureF;
    }

    public void setTemperatureF(double temperatureF) {
        this.temperatureF = temperatureF;
    }

    public boolean ishasPrecipitation() {
        return hasPrecipitation;
    }

    public void setHasPrecipitation(boolean hasPrecipitation) {
        this.hasPrecipitation = hasPrecipitation;
    }

    @Override
    public String toString() {
        return "Temperature (F): " + temperatureF + " Precipitation: " + hasPrecipitation;
    }
}