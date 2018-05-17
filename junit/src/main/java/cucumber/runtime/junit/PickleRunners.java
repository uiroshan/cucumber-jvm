package cucumber.runtime.junit;

import cucumber.api.event.Event;
import cucumber.runner.EventBus;
import cucumber.runner.Runner;
import cucumber.runner.TimeService;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import gherkin.events.PickleEvent;
import gherkin.pickles.PickleLocation;
import gherkin.pickles.PickleStep;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class PickleRunners {

    interface PickleRunner {
        void run(RunNotifier notifier);

        Description getDescription();

        Description describeChild(PickleStep step);

    }

    static PickleRunner withStepDescriptions(Runtime runner, PickleEvent pickleEvent, RuntimeOptions runtimeOptions, JUnitOptions jUnitOptions) throws InitializationError {
        return new WithStepDescriptions(runner, pickleEvent, runtimeOptions, jUnitOptions);
    }

    static PickleRunner withNoStepDescriptions(String featureName, Runtime runtime, PickleEvent pickleEvent, RuntimeOptions runtimeOptions, JUnitOptions jUnitOptions) {
        return new NoStepDescriptions(featureName, runtime, pickleEvent, runtimeOptions, jUnitOptions);
    }

    static class WithStepDescriptions extends ParentRunner<PickleStep> implements PickleRunner {
        private final Runtime runtime;
        private final PickleEvent pickleEvent;
        private final RuntimeOptions runtimeOptions;
        private final JUnitOptions jUnitOptions;
        private final Map<PickleStep, Description> stepDescriptions = new HashMap<PickleStep, Description>();
        private Description description;


        public WithStepDescriptions(Runtime runtime, PickleEvent pickleEvent, RuntimeOptions runtimeOptions, JUnitOptions jUnitOptions) throws InitializationError {
            super(null);
            this.runtime = runtime;
            this.pickleEvent = pickleEvent;
            this.runtimeOptions = runtimeOptions;
            this.jUnitOptions = jUnitOptions;
        }

        @Override
        protected List<PickleStep> getChildren() {
            return pickleEvent.pickle.getSteps();
        }

        @Override
        public String getName() {
            return getPickleName(pickleEvent, jUnitOptions.filenameCompatibleNames());
        }

        @Override
        public Description getDescription() {
            if (description == null) {
                description = Description.createSuiteDescription(getName(), new PickleId(pickleEvent));
                for (PickleStep step : getChildren()) {
                    description.addChild(describeChild(step));
                }
            }
            return description;
        }

        @Override
        public Description describeChild(PickleStep step) {
            Description description = stepDescriptions.get(step);
            if (description == null) {
                String testName;
                if (jUnitOptions.filenameCompatibleNames()) {
                    testName = makeNameFilenameCompatible(step.getText());
                } else {
                    testName = step.getText();
                }
                description = Description.createTestDescription(getName(), testName, new PickleStepId(pickleEvent, step));
                stepDescriptions.put(step, description);
            }
            return description;
        }

        @Override
        public void run(final RunNotifier notifier) {
            Runner runner = runtime.getRunner();
            Runtime.FlushBus bush = (Runtime.FlushBus) runner.getBus();
            JUnitReporter jUnitReporter = new JUnitReporter(bush, runtimeOptions.isStrict(), jUnitOptions);
            jUnitReporter.startExecutionUnit(this, notifier);
            // This causes runChild to never be called, which seems OK.
            runner.runPickle(pickleEvent);
//            bush.flush();
        }

        @Override
        protected void runChild(PickleStep step, RunNotifier notifier) {
            // The way we override run(RunNotifier) causes this method to never be called.
            // Instead it happens via cucumberScenario.run(jUnitReporter, jUnitReporter, runtime);
            throw new UnsupportedOperationException();
        }

    }


    static final class NoStepDescriptions implements PickleRunner {
        private final String featureName;
        private final Runtime runtime;
        private final PickleEvent pickleEvent;
        private final RuntimeOptions runtimeOptions;
        private final JUnitOptions jUnitOptions;
        private Description description;


        public NoStepDescriptions(String featureName, Runtime runtime, PickleEvent pickleEvent, RuntimeOptions runtimeOptions, JUnitOptions jUnitOptions) {
            this.featureName = featureName;
            this.runtime = runtime;
            this.pickleEvent = pickleEvent;
            this.runtimeOptions = runtimeOptions;
            this.jUnitOptions = jUnitOptions;
        }

        @Override
        public Description getDescription() {
            if (description == null) {
                String className = createName(featureName, jUnitOptions.filenameCompatibleNames());
                String name = getPickleName(pickleEvent, jUnitOptions.filenameCompatibleNames());
                description = Description.createTestDescription(className, name, new PickleId(pickleEvent));
            }
            return description;
        }

        @Override
        public Description describeChild(PickleStep step) {
            throw new UnsupportedOperationException("This pickle runner does not wish to describe its children");
        }

        @Override
        public void run(final RunNotifier notifier) {
            Runner runner = runtime.getRunner();
            Runtime.FlushBus bus = (Runtime.FlushBus) runner.getBus();
            JUnitReporter jUnitReporter = new JUnitReporter(bus, runtimeOptions.isStrict(), jUnitOptions);
            jUnitReporter.startExecutionUnit(this, notifier);
            runner.runPickle(pickleEvent);
//            bus.flush();
        }
    }

    private static final class Store extends EventBus {

        private final List<Event> events = new ArrayList<Event>();

        Store(TimeService stopWatch) {
            super(stopWatch);
        }

        @Override
        public void send(Event event) {
            super.send(event);
            events.add(event);
        }


    }

    private static String getPickleName(PickleEvent pickleEvent, boolean useFilenameCompatibleNames) {
        final String name = pickleEvent.pickle.getName();
        return createName(name, useFilenameCompatibleNames);
    }


    private static String createName(final String name, boolean useFilenameCompatibleNames) {
        if (name.isEmpty()) {
            return "EMPTY_NAME";
        }

        if (useFilenameCompatibleNames) {
            return makeNameFilenameCompatible(name);
        }

        return name;
    }

    private static String makeNameFilenameCompatible(String name) {
        return name.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static final class PickleId implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String uri;
        private int pickleLine;

        PickleId(PickleEvent pickleEvent) {
            this.uri = pickleEvent.uri;
            this.pickleLine = pickleEvent.pickle.getLocations().get(0).getLine();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PickleId that = (PickleId) o;
            return pickleLine == that.pickleLine && uri.equals(that.uri);
        }

        @Override
        public int hashCode() {
            int result = uri.hashCode();
            result = 31 * result + pickleLine;
            return result;
        }

        @Override
        public String toString() {
            return uri + ":" + pickleLine;
        }
    }

    private static final class PickleStepId implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String uri;
        private final int pickleLine;
        private int pickleStepLine;

        PickleStepId(PickleEvent pickleEvent, PickleStep pickleStep) {
            this.uri = pickleEvent.uri;
            this.pickleLine = pickleEvent.pickle.getLocations().get(0).getLine();
            List<PickleLocation> stepLocations = pickleStep.getLocations();
            this.pickleStepLine = stepLocations.get(stepLocations.size() - 1).getLine();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PickleStepId that = (PickleStepId) o;
            return pickleLine == that.pickleLine && pickleStepLine == that.pickleStepLine && uri.equals(that.uri);
        }

        @Override
        public int hashCode() {
            int result = pickleLine;
            result = 31 * result + uri.hashCode();
            result = 31 * result + pickleStepLine;
            return result;
        }

        @Override
        public String toString() {
            return uri + ":" + pickleLine + ":" + pickleStepLine;
        }
    }

}
