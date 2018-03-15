package cucumber.runtime.java8;

import cucumber.api.java8.GlueBase;
import cucumber.api.java8.HookBody;
import cucumber.api.java8.HookNoArgsBody;
import cucumber.runtime.java.JavaBackend;

public interface LambdaGlueBase extends GlueBase {

    String EMPTY_TAG_EXPRESSION = "";
    long NO_TIMEOUT = 0;
    int DEFAULT_BEFORE_ORDER = 0;
    int DEFAULT_AFTER_ORDER = 1000;

    default void Before(final HookBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, DEFAULT_BEFORE_ORDER, NO_TIMEOUT, body));
    }

    default void Before(String tagExpression, final HookBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(tagExpression, DEFAULT_BEFORE_ORDER, NO_TIMEOUT, body));
    }

    default void Before(long timeoutMillis, final HookBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, DEFAULT_BEFORE_ORDER, timeoutMillis, body));
    }

    default void Before(int order, final HookBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, order, NO_TIMEOUT, body));
    }

    default void Before(String tagExpression, long timeoutMillis, int order, final HookBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(tagExpression, order, timeoutMillis, body));
    }

    default void Before(final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, DEFAULT_BEFORE_ORDER, NO_TIMEOUT, body));
    }

    default void Before(String tagExpression, final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(tagExpression, DEFAULT_BEFORE_ORDER, NO_TIMEOUT, body));
    }

    default void Before(long timeoutMillis, final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, DEFAULT_BEFORE_ORDER, timeoutMillis, body));
    }

    default void Before(int order, final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, order, NO_TIMEOUT, body));
    }

    default void Before(String tagExpression, long timeoutMillis, int order, final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addBeforeHookDefinition(new Java8HookDefinition(tagExpression, order, timeoutMillis, body));
    }

    default void After(final HookBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, DEFAULT_AFTER_ORDER, NO_TIMEOUT, body));
    }

    default void After(String tagExpression, final HookBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(tagExpression, DEFAULT_AFTER_ORDER, NO_TIMEOUT, body));
    }

    default void After(long timeoutMillis, final HookBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, DEFAULT_AFTER_ORDER, timeoutMillis, body));
    }

    default void After(int order, final HookBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, order, NO_TIMEOUT, body));
    }

    default void After(String tagExpression, long timeoutMillis, int order, final HookBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(tagExpression, order, timeoutMillis, body));
    }

    default void After(final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, DEFAULT_AFTER_ORDER, NO_TIMEOUT, body));
    }

    default void After(String tagExpression, final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(tagExpression, DEFAULT_AFTER_ORDER, NO_TIMEOUT, body));
    }

    default void After(long timeoutMillis, final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, DEFAULT_AFTER_ORDER, timeoutMillis, body));
    }

    default void After(int order, final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(EMPTY_TAG_EXPRESSION, order, NO_TIMEOUT, body));
    }

    default void After(String tagExpression, long timeoutMillis, int order, final HookNoArgsBody body) {
        JavaBackend.INSTANCE.get().addAfterHookDefinition(new Java8HookDefinition(tagExpression, order, timeoutMillis, body));
    }
}
