/*
 * Copyright (c) 2011 Yahoo! Inc. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License. See accompanying LICENSE file. 
 */
package io.s4.example.model;

import io.s4.model.Model;

import java.io.InputStream;

import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.name.Names;

/**
 * Configures the controller.
 * 
 * Reads a properties file, provides a {@link Communicator} singleton, and
 * configures Guice bindings.
 * 
 * @author Leo Neumeyer
 */
public class Module extends AbstractModule {

    protected PropertiesConfiguration config = null;

    private void loadProperties(Binder binder) {

        try {
            InputStream is = this.getClass().getResourceAsStream(
                    "/model.properties");
            config = new PropertiesConfiguration();
            config.load(is);

            System.out.println(ConfigurationUtils.toString(config));
            // TODO - validate properties.

            /* Make all properties injectable. Do we need this? */
            Names.bindProperties(binder,
                    ConfigurationConverter.getProperties(config));
        } catch (ConfigurationException e) {
            binder.addError(e);
            e.printStackTrace();
        }
    }

    @Override
    protected void configure() {
        if (config == null)
            loadProperties(binder());

        int vectorSize = config.getInt("model.vector_size");
        int numGaussians = config.getInt("model.num_gaussians");

        /*
         * Here we bind a specific Model implementation. This one is the
         * GaussianMixtureModel
         */
        bind(Model.class).toInstance(
                new io.s4.model.GaussianMixtureModel(vectorSize, numGaussians,
                        io.s4.model.GaussianMixtureModel.TrainMethod.STEP));

        /* Alternatively, we could bind to the GaussianModel. */
        // bind(Model.class).toInstance(
        // new io.s4.model.GaussianModel(vectorSize, true));

    }
}