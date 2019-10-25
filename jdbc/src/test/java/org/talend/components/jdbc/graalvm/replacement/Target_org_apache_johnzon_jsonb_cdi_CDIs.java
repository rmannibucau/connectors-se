/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.components.jdbc.graalvm.replacement;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.johnzon.jsonb.cdi.CDIs;

@TargetClass(CDIs.class)
public final class Target_org_apache_johnzon_jsonb_cdi_CDIs {
    @Substitute
    public Target_org_apache_johnzon_jsonb_cdi_CDIs(final Object beanManager) {
        // no-op, no cdi there
    }

    @Substitute
    public boolean isCanWrite() {
        return false;
    }
}
