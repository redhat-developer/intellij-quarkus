/*
 *******************************************************************************
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eclipse.microprofile.fault.tolerance.tck.invalidParameters;

import java.sql.Connection;

import org.eclipse.microprofile.faulttolerance.Retry;

/**
 * A client to demonstrate the validation of @Retry
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
public class RetryClientForValidation {

    @Retry(delay = -1)
    public Connection invalidA() {
        return null;
    }
    
    @Retry(maxDuration = -1)
    public Connection invalidB() {
        return null;
    }
    
    @Retry(jitter = -1)
    public Connection invalidC() {
        return null;
    }
    
    @Retry(maxRetries = -2)
    public Connection invalidD() {
        return null;
    }
    
    @Retry(delay = 1000, maxDuration = 500)
    public Connection invalidE() {
        return null;
    }
    
    @Retry(delay = 1000, maxDuration = 1000)
    public Connection invalidF() {
        return null;
    }
    
    @Retry(delay = 1)
    public Connection validA() {
        return null;
    }
    
    @Retry(maxDuration = 1)
    public Connection validB() {
        return null;
    }
    
    @Retry(jitter = 1)
    public Connection validC() {
        return null;
    }
    
    @Retry(maxRetries = 1)
    public Connection validD() {
        return null;
    }
    
    @Retry(delay = 1000, maxDuration = 5000)
    public Connection validE() {
        return null;
    }
}