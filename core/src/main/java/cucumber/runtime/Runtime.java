package cucumber.runtime;

import cucumber.api.TypeRegistryConfigurer;
import cucumber.api.StepDefinitionReporter;
import cucumber.api.SummaryPrinter;
import cucumber.api.event.Event;
import cucumber.api.event.TestRunFinished;
import cucumber.runner.EventBus;
import cucumber.runner.EventSink;
import cucumber.runner.Runner;
import cucumber.runner.TimeService;
import cucumber.runtime.io.ClasspathResourceLoader;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.model.CucumberFeature;
import gherkin.events.PickleEvent;
import gherkin.pickles.Compiler;
import gherkin.pickles.Pickle;
import io.cucumber.stepexpression.TypeRegistry;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;

/**
 * This is the main entry point for running Cucumber features.
 */
public class Runtime {

    final Stats stats; // package private to be available for tests.
    private final UndefinedStepsTracker undefinedStepsTracker = new UndefinedStepsTracker();

    private final RuntimeOptions runtimeOptions;

    private final ResourceLoader resourceLoader;
    private final ClassLoader classLoader;
    private final ThreadLocal<Runner> runner = new ThreadLocal<Runner>();
    private final List<PicklePredicate> filters;
    private final EventBus bus;
    private final Compiler compiler = new Compiler();
    private final Provider<Collection<? extends Backend>> backendProvider;
    private final Provider<Glue> optionalGlue;

    public Runtime(ResourceLoader resourceLoader, ClassFinder classFinder, ClassLoader classLoader, RuntimeOptions runtimeOptions) {
        this(resourceLoader, classLoader, loadBackends(resourceLoader, classFinder, runtimeOptions), runtimeOptions);
    }

    public Runtime(ResourceLoader resourceLoader, ClassLoader classLoader, Provider<Collection<? extends Backend>> backendProvider, RuntimeOptions runtimeOptions) {
        this(resourceLoader, classLoader, backendProvider, runtimeOptions, TimeService.SYSTEM, new Provider<Glue>() {
            @Override
            public Glue get() {
                return new RuntimeGlue();
            }
        });
    }

    public Runtime(ResourceLoader resourceLoader, ClassLoader classLoader, Provider<Collection<? extends Backend>> backendProvider,
                   RuntimeOptions runtimeOptions, Provider<Glue> optionalGlue) {
        this(resourceLoader, classLoader, backendProvider, runtimeOptions, TimeService.SYSTEM, optionalGlue);
    }

    public Runtime(ResourceLoader resourceLoader, ClassLoader classLoader, Provider<Collection<? extends Backend>> backendProvider,
                   RuntimeOptions runtimeOptions, TimeService stopWatch, Provider<Glue> optionalGlue) {
        this.backendProvider = backendProvider;
        this.optionalGlue = optionalGlue;
        this.resourceLoader = resourceLoader;
        this.classLoader = classLoader;
        this.runtimeOptions = runtimeOptions;
        this.stats = new Stats(runtimeOptions.isMonochrome());
        this.bus = new EventBus(stopWatch);
        this.filters = new ArrayList<PicklePredicate>();
        List<String> tagFilters = runtimeOptions.getTagFilters();
        if (!tagFilters.isEmpty()) {
            this.filters.add(new TagPredicate(tagFilters));
        }
        List<Pattern> nameFilters = runtimeOptions.getNameFilters();
        if (!nameFilters.isEmpty()) {
            this.filters.add(new NamePredicate(nameFilters));
        }
        Map<String, List<Long>> lineFilters = runtimeOptions.getLineFilters(resourceLoader);
        if (!lineFilters.isEmpty()) {
            this.filters.add(new LinePredicate(lineFilters));
        }

        stats.setEventPublisher(bus);
        undefinedStepsTracker.setEventPublisher(bus);
        runtimeOptions.setEventBus(bus);
    }

    public Runtime(ClasspathResourceLoader classpathResourceLoader, ClassLoader classLoader, final List<Backend> backends, RuntimeOptions runtimeOptions) {
        this(classpathResourceLoader, classLoader, new Provider<Collection<? extends Backend>>() {
            @Override
            public Collection<? extends Backend> get() {
                return backends;
            }
        }, runtimeOptions);
    }

    public Runtime(ClasspathResourceLoader resourceLoader, ClassLoader classLoader, final List<Backend> backends, RuntimeOptions runtimeOptions, TimeService timeService, final RuntimeGlue glue) {
        this(resourceLoader, classLoader, new Provider<Collection<? extends Backend>>() {
            @Override
            public Collection<? extends Backend> get() {
                return backends;
            }
        }, runtimeOptions, timeService, new Provider<Glue>() {
            @Override
            public Glue get() {
                return glue;
            }
        });
    }

    public Runtime(ResourceLoader resourceLoader, ClassLoader classLoader, final Collection<Backend> backends, RuntimeOptions runtimeOptions) {
        this(resourceLoader, classLoader, new Provider<Collection<? extends Backend>>() {
            @Override
            public Collection<? extends Backend> get() {
                return backends;
            }
        }, runtimeOptions);
    }

    public Runtime(ResourceLoader resourceLoader, ClassLoader classLoader, final Collection<Backend> backends, RuntimeOptions runtimeOptions, final RuntimeGlue glue) {
        this(resourceLoader, classLoader, new Provider<Collection<? extends Backend>>() {
            @Override
            public Collection<? extends Backend> get() {
                return backends;
            }
        }, runtimeOptions, new Provider<Glue>() {
            @Override
            public Glue get() {
                return glue;
            }
        });
    }

    private static Provider<Collection<? extends Backend>> loadBackends(final ResourceLoader resourceLoader, final ClassFinder classFinder, final RuntimeOptions runtimeOptions) {
        return new Provider<Collection<? extends Backend>>() {
            @Override
            public Collection<? extends Backend> get() {
                Reflections reflections = new Reflections(classFinder);
                TypeRegistryConfigurer typeRegistryConfigurer = reflections.instantiateExactlyOneSubclass(TypeRegistryConfigurer.class, MultiLoader.packageName(runtimeOptions.getGlue()), new Class[0], new Object[0], new DefaultTypeRegistryConfiguration());
                TypeRegistry typeRegistry = new TypeRegistry(typeRegistryConfigurer.locale());
                typeRegistryConfigurer.configureTypeRegistry(typeRegistry);
                return reflections.instantiateSubclasses(Backend.class, singletonList("cucumber.runtime"), new Class[]{ResourceLoader.class, TypeRegistry.class}, new Object[]{resourceLoader, typeRegistry});
            }
        };
    }

    //TODO: Remove
    public Glue getGlue() {
        return getRunner().getGlue();
    }

    /**
     * This is the main entry point. Used from CLI, but not from JUnit.
     */
    public void run() {
        // Make sure all features parse before initialising any reporters/formatters
        List<CucumberFeature> features = runtimeOptions.cucumberFeatures(resourceLoader, bus);

        // TODO: This is duplicated in cucumber.api.android.CucumberInstrumentationCore - refactor or keep uptodate

        StepDefinitionReporter stepDefinitionReporter = runtimeOptions.stepDefinitionReporter(classLoader);

        reportStepDefinitions(stepDefinitionReporter);

        for (CucumberFeature cucumberFeature : features) {
            runFeature(cucumberFeature);
        }

        bus.send(new TestRunFinished(bus.getTime()));
        printSummary();
    }

    public void reportStepDefinitions(StepDefinitionReporter stepDefinitionReporter) {
        getRunner().reportStepDefinitions(stepDefinitionReporter);
    }

    public void runFeature(CucumberFeature feature) {
        List<PickleEvent> pickleEvents = compileFeature(feature);
        for (PickleEvent pickleEvent : pickleEvents) {
            if (matchesFilters(pickleEvent)) {
                getRunner().runPickle(pickleEvent);
            }
        }
    }

    public List<PickleEvent> compileFeature(CucumberFeature feature) {
        List<PickleEvent> pickleEvents = new ArrayList<PickleEvent>();
        for (Pickle pickle : compiler.compile(feature.getGherkinFeature())) {
            pickleEvents.add(new PickleEvent(feature.getUri(), pickle));
        }
        return pickleEvents;
    }

    public boolean matchesFilters(PickleEvent pickleEvent) {
        for (PicklePredicate filter : filters) {
            if (!filter.apply(pickleEvent)) {
                return false;
            }
        }
        return true;
    }

    public void printSummary() {
        SummaryPrinter summaryPrinter = runtimeOptions.summaryPrinter(classLoader);
        summaryPrinter.print(this);
    }

    void printStats(PrintStream out) {
        stats.printStats(out, runtimeOptions.isStrict());
    }

    public List<Throwable> getErrors() {
        return stats.getErrors();
    }

    public byte exitStatus() {
        return stats.exitStatus(runtimeOptions.isStrict());
    }

    public List<String> getSnippets() {
        return undefinedStepsTracker.getSnippets();
    }

    public EventBus getEventBus() {
        return bus;
    }

    public Runner getRunner() {
        Runner runner = this.runner.get();
        if (runner != null) {
            return runner;
        }

        Collection<? extends Backend> backends = backendProvider.get();
        if (backends.isEmpty()) {
            throw new CucumberException("No backendProvider were found. Please make sure you have a backend module on your CLASSPATH.");
        }

        EventBus sink = new FlushBus(bus);
        return new Runner(optionalGlue.get(), sink, backends, runtimeOptions);
    }

    public class FlushBus extends EventBus {
        private final List<Event> events = new ArrayList<Event>();

        private EventBus parent;

        FlushBus(EventBus bus) {
            super(bus);
            this.parent = bus;
        }

        @Override
        public void send(Event event) {
            super.send(event);
            events.add(event);
        }

        public void flush(){
            parent.sendAll(events);
            events.clear();
        }

    }

}
