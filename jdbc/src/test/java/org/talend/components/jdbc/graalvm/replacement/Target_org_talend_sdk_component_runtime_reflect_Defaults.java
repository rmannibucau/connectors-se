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
package org.talend.components.jdbc.graalvm.replacement;

import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.slf4j.LoggerFactory;
import org.talend.sdk.component.runtime.reflect.Defaults;

@TargetClass(Defaults.class)
public final class Target_org_talend_sdk_component_runtime_reflect_Defaults {

    @Substitute
    public static Object handleDefault(final Class<?> declaringClass, final Method method, final Object proxy,
            final Object[] args) throws Throwable {
        LoggerFactory.getLogger(Defaults.class.getName())
                .warn("Default methods are not supported in this environment (" + method + "), replace it by a service");
        throw new UnsupportedOperationException("Default methods are not supported: ");
    }
}
