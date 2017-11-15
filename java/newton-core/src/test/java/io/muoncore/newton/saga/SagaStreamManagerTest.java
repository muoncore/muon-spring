package io.muoncore.newton.saga;

import io.muoncore.newton.NewtonEvent;
import io.muoncore.newton.StreamSubscriptionManager;
import io.muoncore.newton.command.CommandBus;
import io.muoncore.newton.command.CommandIntent;
import lombok.Getter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.function.Consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SagaStreamManagerTest {

    private StreamSubscriptionManager subscriptionManager = mock(StreamSubscriptionManager.class);
    private SagaRepository sagaRepository = mock(SagaRepository.class);
    private CommandBus commandBus;
    private SagaInterestMatcher sagaInterestMatcher = mock(SagaInterestMatcher.class);
    private SagaFactory sagaFactory = mock(SagaFactory.class);
    private SagaLoader sagaLoader = mock(SagaLoader.class);

    @Test(expected = IllegalStateException.class)
    public void badlyConfiguredSagaBreaks() throws Exception {
        SagaStreamManager manager = streamManager();

        manager.processSaga(NoAnnotationSaga.class);
    }

    @Before
    public void setUp() {
        when(sagaInterestMatcher.matches(any(), any())).thenReturn(true);
        commandBus = mock(CommandBus.class);
    }

    @Test
    public void sagaConfigOpensStreamSubscriptions() throws Exception {
        SagaStreamManager manager = streamManager();

        manager.processSaga(SagaWithConfig.class);

        verify(subscriptionManager,
                times(1)).globallyUniqueSubscription(eq("saga-manager-stream"), eq("stream"), any());
        verify(subscriptionManager,
                times(1)).globallyUniqueSubscription(eq("saga-manager-stream2"), eq("stream2"), any());
    }

    @Test
    public void duplicateStreamConfigDoesNotOpenAgain() throws Exception {
        SagaStreamManager manager = streamManager();

        manager.processSaga(SagaWithConfig.class);
        manager.processSaga(SagaWithConfig.class);

        verify(subscriptionManager,
                times(1)).globallyUniqueSubscription(eq("saga-manager-stream"), eq("stream"), any());
    }

    @Test
    public void dataOnStreamIsSentToSagas() throws Exception {

        SagaWithEventHandler saga = mock(SagaWithEventHandler.class);

        String sagaId = "12345";

        when(sagaRepository.getSagasInterestedIn(eq(SagaEvent.class))).thenReturn(Arrays.asList(new SagaInterest(
                TestSaga.class.getName(),
                SagaWithEventHandler.class.getName(), "££££123", sagaId, "hello", "world")));

        Class<? extends Saga> type = SagaWithEventHandler.class;

        when(sagaLoader.loadSagaClass(any())).thenReturn((Class)type);
        when(sagaRepository.load(eq(sagaId), eq(SagaWithEventHandler.class))).thenReturn(Optional.of(saga));

        SagaStreamManager manager = streamManager();
        ArgumentCaptor<Consumer<NewtonEvent>> eventStreamCaptor = (ArgumentCaptor)ArgumentCaptor.forClass(Consumer.class);
        manager.processSaga(SagaWithConfig.class);

        verify(subscriptionManager,
                times(1)).globallyUniqueSubscription(eq("saga-manager-stream"), eq("stream"), eventStreamCaptor.capture());

        SagaEvent ev = new SagaEvent();
        eventStreamCaptor.getValue().accept(ev);

        Thread.sleep(500);

        verify(saga).handle(eq(ev));
    }

    @Test
    public void associationsAreUsedToFilterStream() throws Exception {

        when(sagaInterestMatcher.matches(any(), any())).thenReturn(false);

        SagaWithCommands saga = new SagaWithCommands();

        SagaStreamManager manager = streamManager();
        ArgumentCaptor<Consumer<NewtonEvent>> eventStreamCaptor =  (ArgumentCaptor)ArgumentCaptor.forClass(Consumer.class);
        manager.processSaga(SagaWithCommands.class);

        String sagaId = "1234";

        when(sagaRepository.getSagasInterestedIn(eq(SagaEvent.class))).thenReturn(Arrays.asList(new SagaInterest(
                TestSaga.class.getName(), SagaWithEventHandler.class.getCanonicalName(), sagaId, "4321", "hello", "orld")));

        Class<? extends Saga> type = SagaWithCommands.class;

        when(sagaLoader.loadSagaClass(any())).thenReturn((Class)type);
        when(sagaRepository.load(eq(sagaId), eq(SagaWithCommands.class))).thenReturn(Optional.of(saga));


        verify(subscriptionManager).globallyUniqueSubscription(eq("saga-manager-mystream"), eq("mystream"), eventStreamCaptor.capture());

        eventStreamCaptor.getValue().accept(new SagaEvent());

        verify(commandBus, times(0)).dispatch(any(CommandIntent.class));
    }

    private SagaStreamManager streamManager() {
        return new SagaStreamManager(subscriptionManager, sagaRepository, commandBus, sagaInterestMatcher, sagaFactory, sagaLoader);
    }


    @SagaStreamConfig(streams = {"stream", "stream2"}, aggregateRoots = {})
    static class SagaWithConfig extends StatefulSaga {

    }

    static class NoAnnotationSaga extends StatefulSaga {

    }
    @SagaStreamConfig(streams = {"mystream"}, aggregateRoots = {})
    static class SagaWithCommands implements Saga {
        @Override
        public List<CommandIntent> getNewOperations() {
            return new ArrayList<>(Arrays.asList(
              CommandIntent.builder("mytype").build(),
              CommandIntent.builder("mytype").build(),
              CommandIntent.builder("mytype").build(),
              CommandIntent.builder("mytype").build()
            ));
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public void handle(NewtonEvent event) {

        }

        @StartSagaWith
        public void startWith(NewtonEvent event) {

        }

      @Override
        public List<SagaInterest> getNewSagaInterests() {
            return Collections.emptyList();
        }
    }

    @SagaStreamConfig(streams = {}, aggregateRoots = {})
    static class SagaWithEventHandler implements Saga {

        @Override
        public List<CommandIntent> getNewOperations() {
            return null;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public void startWith(NewtonEvent event) {}
        @Override
        public void handle(NewtonEvent event) {}
        @Override
        public List<SagaInterest> getNewSagaInterests() {
            return Collections.emptyList();
        }
    }

    static class SagaEvent implements NewtonEvent {
      @Getter
      private final String id = "hello-world";
    }
}
