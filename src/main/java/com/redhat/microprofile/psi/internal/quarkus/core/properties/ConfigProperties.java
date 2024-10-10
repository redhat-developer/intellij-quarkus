package com.redhat.microprofile.psi.internal.quarkus.core.properties;

import io.quarkus.runtime.util.StringUtil;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigProperties {
    String UNSET_PREFIX = "<< unset >>";

    String prefix() default "<< unset >>";

    NamingStrategy namingStrategy() default ConfigProperties.NamingStrategy.FROM_CONFIG;

    public static enum NamingStrategy {
        FROM_CONFIG {
            public String getName(String name) {
                throw new IllegalStateException("The naming strategy needs to substituted with the configured naming strategy");
            }
        },
        VERBATIM {
            public String getName(String name) {
                return name;
            }
        },
        KEBAB_CASE {
            public String getName(String name) {
                return StringUtil.hyphenate(name);
            }
        };

        private NamingStrategy() {
        }

        public abstract String getName(String var1);
    }
}
