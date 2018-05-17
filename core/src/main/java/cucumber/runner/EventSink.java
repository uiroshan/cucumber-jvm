package cucumber.runner;

import cucumber.api.event.Event;

public interface EventSink {

    void send(Event event);

    Long getTime();
}
