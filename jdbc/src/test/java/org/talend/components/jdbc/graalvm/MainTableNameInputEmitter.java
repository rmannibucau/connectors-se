/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.components.jdbc.graalvm;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;

import org.apache.johnzon.core.JsonProviderImpl;
import org.apache.johnzon.jsonb.JohnzonProvider;
import org.talend.components.jdbc.input.TableNameInputEmitter;
import org.talend.components.jdbc.service.I18nMessage;
import org.talend.components.jdbc.service.JdbcService;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.api.service.dependency.Resolver;
import org.talend.sdk.component.api.service.injector.Injector;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.LocalPartitionMapper;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.input.PartitionMapperImpl;
import org.talend.sdk.component.runtime.internationalization.InternationalizationServiceFactory;
import org.talend.sdk.component.runtime.manager.asm.ProxyGenerator;
import org.talend.sdk.component.runtime.manager.json.TalendAccessMode;
import org.talend.sdk.component.runtime.manager.proxy.JavaProxyEnricherFactory;
import org.talend.sdk.component.runtime.manager.reflect.Constructors;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.DefaultServiceProvider;
import org.talend.sdk.component.runtime.manager.service.configuration.PropertiesConfiguration;
import org.talend.sdk.component.runtime.manager.util.LazyMap;
import org.talend.sdk.component.runtime.manager.xbean.registry.EnrichedPropertyEditorRegistry;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.TCCLContainerFinder;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class MainTableNameInputEmitter {

    public static void main(final String[] args) throws Exception {
        // inputs: to do with a graal AutomaticFeature
        final Class<?> clazz = TableNameInputEmitter.class;
        final Class<?>[] servicesTypes = new Class<?>[] { I18nMessage.class, JdbcService.class };
        final Map<String, String> config = new HashMap<>();
        config.put("configuration.dataSet.connection.jdbcUrl", "jdbc:h2:mem:graal");
        config.put("configuration.dataSet.connection.userId", "sa");
        config.put("configuration.dataSet.connection.password", "");
        config.put("configuration.dataSet.tableName", "STUDENT");
        config.put("configuration.dataSet.connection.dbType", "H2");
        config.put("configuration.dataSet.advancedCommon.fetchSize", "0");

        // build config
        final Properties properties = System.getProperties();
        try (final InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("TALEND-INF/local-configuration.properties")) {
            if (stream != null) {
                final Properties temp = new Properties();
                temp.load(stream);
                // only set if missing, don't override system properties
                temp.stringPropertyNames().forEach(k -> properties.putIfAbsent(k, temp.getProperty(k)));
            }
            properties.setProperty("jdbc.drivers[1].id", "H2");
            properties.setProperty("jdbc.drivers[1].displayName", "H2");
            properties.setProperty("jdbc.drivers[1].className", "org.h2.Driver");
            properties.setProperty("jdbc.drivers[1].paths[length]", "0");
        }

        // disable classloading
        final ContainerFinder finder = new TCCLContainerFinder();
        ContainerFinder.Instance.set(() -> finder);
        // fake a pluginId since we'll not serialize we don't care
        final String pluginId = "run"; // we dont serialize so not important

        // load service layer
        final EnrichedPropertyEditorRegistry registry = new EnrichedPropertyEditorRegistry();
        final ReflectionService reflection = new ReflectionService(new ParameterModelService(registry), registry);
        final ParameterModelService parameterModelService = new ParameterModelService(registry);
        final LocalConfiguration configuration = new PropertiesConfiguration(properties);
        final Map<Class<?>, Object> services = createServices(pluginId, reflection, registry, configuration);
        Stream.of(servicesTypes)
                .forEach(st -> services.put(st, createService(st, Injector.class.cast(services.get(Injector.class)))));
        final Constructor<?> constructor = Constructors.findConstructor(clazz);
        final Function<Map<String, String>, Object[]> parameterFactory = reflection.parameterFactory(constructor, services,
                parameterModelService.buildParameterMetas(constructor,
                        ofNullable(clazz.getPackage()).map(Package::getName).orElse(""),
                        new BaseParameterEnricher.Context(configuration)));
        final Serializable instance = Serializable.class.cast(constructor.newInstance(parameterFactory.apply(config)));
        final Mapper mapper = clazz.isAnnotationPresent(Emitter.class)
                ? new LocalPartitionMapper(pluginId, "run", "run", instance)
                : new PartitionMapperImpl(pluginId, "run", "run", "run", false, instance);

        // runtime
        log.info(String.valueOf(mapper));
        log.info("Estimate size: " + mapper.assess());

        // todo: impl correctly, this is not a valid lifecycle
        final Input input = mapper.create();
        input.start();
        Object next;
        while ((next = input.next()) != null) {
            log.info("> {}", next);
        }
        input.stop();
    }

    // todo: postconstruct etc
    private static Object createService(final Class<?> st, final Injector injector) {
        if (st.isAnnotationPresent(Internationalized.class)) {
            return new InternationalizationServiceFactory(Locale::getDefault).create(st,
                    Thread.currentThread().getContextClassLoader());
        }
        // todo: http client
        try {
            final Object instance = st.getConstructor().newInstance();
            injector.inject(instance);
            return instance;
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    private static Map<Class<?>, Object> createServices(final String pluginId, final ReflectionService reflection,
            final EnrichedPropertyEditorRegistry registry, final LocalConfiguration configuration) {
        final JsonProviderImpl jsonpProvider = new JsonProviderImpl();
        final JsonbConfig jsonbConfig = new JsonbConfig().withBinaryDataStrategy(BinaryDataStrategy.BASE_64)
                .setProperty("johnzon.cdi.activated", false).setProperty("johnzon.accessModeDelegate", new TalendAccessMode());
        final JohnzonProvider jsonbProvider = new JohnzonProvider();
        final DefaultServiceProvider defaultServices = new DefaultServiceProvider(reflection, jsonpProvider,
                jsonpProvider.createGeneratorFactory(emptyMap()), jsonpProvider.createReaderFactory(emptyMap()),
                jsonpProvider.createBuilderFactory(emptyMap()), jsonpProvider.createParserFactory(emptyMap()),
                jsonpProvider.createWriterFactory(emptyMap()), jsonbConfig, jsonbProvider, new ProxyGenerator(),
                new JavaProxyEnricherFactory(), singletonList(configuration), a -> new RecordBuilderFactoryImpl("run"), registry);

        final AtomicReference<Map<Class<?>, Object>> servicesRef = new AtomicReference<>();
        final LazyMap<Class<?>, Object> services = new LazyMap<>(24, k -> {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (Resolver.class == k) {
                return new Resolver() { // contextual

                    @Override
                    public ClassLoaderDescriptor mapDescriptorToClassLoader(final InputStream descriptor) {
                        return new ClassLoaderDescriptor() {

                            @Override
                            public ClassLoader asClassLoader() {
                                return loader;
                            }

                            @Override
                            public Collection<String> resolvedDependencies() {
                                return emptyList();
                            }

                            @Override
                            public void close() {
                                // no-op
                            }
                        };
                    }

                    @Override
                    public Collection<File> resolveFromDescriptor(final InputStream descriptor) {
                        return emptyList();
                    }
                };
            }
            return defaultServices.lookup(pluginId, loader, Collections::emptyList, s -> null, // unsupported
                    k, servicesRef);
        });
        servicesRef.set(services);
        return services;
    }
}
