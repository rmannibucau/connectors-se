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
package org.talend.components.jdbc.graalvm.feature;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import com.oracle.svm.core.annotate.AutomaticFeature;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.BuiltInSuggestable;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.action.Suggestable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.action.Validable;
import org.talend.sdk.component.api.configuration.action.meta.ActionRef;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.condition.ActiveIfs;
import org.talend.sdk.component.api.configuration.condition.meta.Condition;
import org.talend.sdk.component.api.configuration.constraint.Max;
import org.talend.sdk.component.api.configuration.constraint.Min;
import org.talend.sdk.component.api.configuration.constraint.meta.Validation;
import org.talend.sdk.component.api.configuration.constraint.meta.Validations;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.type.meta.ConfigurationType;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.BeforeGroup;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.configuration.Configuration;

@AutomaticFeature
public class ComponentReflectionFeature implements Feature {

    @Override
    public void beforeAnalysis(final BeforeAnalysisAccess access) {
        // todo: create an AnnotationFinder before the build to scan then configure this feature with graal @Option
        // config (future @Option)
        final boolean enableFinalWrite = false; // java supports final fields to be set, not graal with that toggle on
        final Class<?>[] services = doLoad(access, Stream.of(
                "org.talend.components.jdbc.service.JdbcService",
                "org.talend.components.jdbc.service.I18nMessage"));
        final Class<?>[] components = doLoad(access, Stream.of(
                "org.talend.components.jdbc.input.TableNameInputEmitter"));
        final Class<?>[] configurations = doLoad(access, Stream.of(
                "org.talend.components.jdbc.configuration.InputTableNameConfig",
                "org.talend.components.jdbc.dataset.TableNameDataset",
                "org.talend.components.jdbc.datastore.JdbcConnection",
                "org.talend.components.jdbc.dataset.AdvancedCommon",
                "org.talend.components.jdbc.configuration.JdbcConfiguration",
                "org.talend.components.jdbc.configuration.JdbcConfiguration$Driver"));
        final Class<?>[] customRegisteredClasses = doLoad(access, Stream.of(
                "org.h2.Driver"));

        // todo: move to reflection.json?
        try {
            registerInstantiable(customRegisteredClasses);
            registerApiAnnotations();
            registerConfigurations(configurations, enableFinalWrite);
            registerServices(services);
            registerComponents(components);
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Class<?>[] doLoad(final BeforeAnalysisAccess access, final Stream<String> names) {
        return names
                .map(name -> requireNonNull(access.findClassByName(name), name))
                .toArray(Class[]::new);
    }

    private void registerApiAnnotations() {
        // todo: use xbean-finder to get them by meta annotation and not directly like that
        final Class<?>[] api = Stream.of(Emitter.class, Producer.class, BeforeGroup.class, AfterGroup.class, /* etc... */
                Proposable.class, DynamicValues.class, Suggestable.class, Updatable.class, Validable.class,
                BuiltInSuggestable.class, ActionRef.class, Condition.class, ActiveIf.class, ActiveIfs.class, Configuration.class,
                ConfigurationType.class, DataSet.class, DataStore.class, Option.class, Validations.class, Validation.class,
                Min.class, Max.class /* ...some are missing */).toArray(Class<?>[]::new);
        RuntimeReflection.register(api);
        RuntimeReflection.register(findMethods(api));
    }

    private void registerInstantiable(final Class<?>[] customRegisteredClasses) {
        RuntimeReflection.register(customRegisteredClasses);
        RuntimeReflection.register(findConstructors(customRegisteredClasses));
    }

    private void registerComponents(final Class<?>[] components) {
        RuntimeReflection.register(components);
        RuntimeReflection.register(findConstructors(components));
        RuntimeReflection.register(findMethods(components));
    }

    private void registerServices(final Class<?>[] services) {
        RuntimeReflection.register(services);
        RuntimeReflection.register(findConstructors(services));
        RuntimeReflection.register(findFields(services));
        RuntimeReflection.register(findMethods(services));
    }

    private void registerConfigurations(final Class<?>[] configurations, final boolean enableFinalWrite) {
        RuntimeReflection.register(configurations);
        RuntimeReflection.register(findConstructors(configurations));
        RuntimeReflection.register(enableFinalWrite, findFields(configurations));
    }

    private Field[] findFields(final Class<?>[] services) {
        return Stream
                .of(services).filter(
                        it -> !it.isInterface())
                .flatMap(
                        it -> Stream
                                .concat(Stream.of(it.getDeclaredFields()),
                                        it.getSuperclass() == Object.class ? Stream.empty()
                                                : Stream.of(findFields(new Class<?>[] { it.getSuperclass() }))))
                .toArray(Field[]::new);
    }

    private Method[] findMethods(final Class<?>[] services) {
        return Stream.of(services).flatMap(it -> Stream.of(it.getMethods()).filter(m -> Object.class != m.getDeclaringClass()))
                .toArray(Method[]::new);
    }

    private Constructor<?>[] findConstructors(final Class<?>[] services) {
        return Stream.of(services).filter(it -> !it.isInterface()).flatMap(it -> Stream.of(it.getDeclaredConstructors()))
                .toArray(Constructor[]::new);
    }
}
