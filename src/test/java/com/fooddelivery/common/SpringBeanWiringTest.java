package com.fooddelivery.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Can Spring actually build every bean in this application?
 *
 * <p>Asked here because nothing else asks it before a deployment does. The
 * context-load test needs Docker and is excluded from {@code mvn test}, so a
 * bean that cannot be instantiated passes every check on the way in and fails
 * once, in production, at startup.
 *
 * <p>That is not hypothetical: {@link com.fooddelivery.common.security.SecretCipher}
 * gained a second constructor for the convenience of its own tests, which is
 * enough for Spring to stop choosing automatically. It then looks for a no-arg
 * constructor, does not find one, and the application does not boot. 619 tests
 * passed on that commit.
 *
 * <p>This cannot replace loading the context — it says nothing about missing
 * dependencies, cycles or bad configuration. It covers one specific mistake
 * that is easy to make, invisible in review, and fatal at startup.
 */
@DisplayName("Spring bean wiring")
class SpringBeanWiringTest {

    private static final String BASE_PACKAGE = "com.fooddelivery";

    @Test
    @DisplayName("every scanned component has a constructor Spring can choose")
    void everyComponentHasAnUnambiguousConstructor() {
        List<String> ambiguous = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        // @Component is meta-annotated on @Service, @Repository, @Controller,
        // @RestController and @Configuration, so this one filter covers them.
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        for (var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            Class<?> type;
            try {
                type = Class.forName(candidate.getBeanClassName());
            } catch (ClassNotFoundException e) {
                continue;
            }
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                continue;
            }

            Constructor<?>[] constructors = type.getDeclaredConstructors();
            if (constructors.length <= 1) {
                continue;
            }

            boolean resolvable = false;
            for (Constructor<?> constructor : constructors) {
                // Either one is marked, or there is a no-arg fallback.
                if (constructor.isAnnotationPresent(Autowired.class)
                        || constructor.getParameterCount() == 0) {
                    resolvable = true;
                    break;
                }
            }
            if (!resolvable) {
                ambiguous.add(type.getName() + " has " + constructors.length
                        + " constructors, none annotated @Autowired and no no-arg one");
            }
        }

        assertThat(ambiguous)
                .as("beans Spring cannot instantiate — this is a startup failure, not a warning")
                .isEmpty();
    }

    @Test
    @DisplayName("the scan actually found the application's beans")
    void theScanIsNotEmpty() {
        // Without this, a typo in the base package turns the test above into a
        // green light that checked nothing.
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        assertThat(scanner.findCandidateComponents(BASE_PACKAGE)).hasSizeGreaterThan(100);
    }
}
