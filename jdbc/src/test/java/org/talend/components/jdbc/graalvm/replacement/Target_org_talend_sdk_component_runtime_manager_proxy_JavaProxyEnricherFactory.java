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

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.talend.sdk.component.runtime.manager.proxy.JavaProxyEnricherFactory;

@TargetClass(JavaProxyEnricherFactory.class)
public final class Target_org_talend_sdk_component_runtime_manager_proxy_JavaProxyEnricherFactory {

    @Substitute
    public Object asSerializable(final ClassLoader loader, final String plugin, final String key, final Object instanceToWrap) {
        return instanceToWrap;
    }
}
