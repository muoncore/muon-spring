package io.muoncore.newton;

import io.muoncore.MultiTransportMuon;
import io.muoncore.Muon;
import io.muoncore.codec.Codecs;
import io.muoncore.codec.json.JsonOnlyCodecs;
import io.muoncore.config.AutoConfiguration;
import io.muoncore.config.MuonConfigBuilder;
import io.muoncore.eventstore.TestEventStore;
import io.muoncore.memory.discovery.InMemDiscovery;
import io.muoncore.memory.transport.InMemTransport;
import io.muoncore.memory.transport.bus.EventBus;
import io.muoncore.newton.cluster.LockService;
import io.muoncore.newton.eventsource.AggregateRootSnapshotRepository;
import io.muoncore.newton.eventsource.muon.EventStreamProcessor;
import io.muoncore.newton.eventsource.muon.NoOpAggregateRootSnapshotRepository;
import io.muoncore.newton.eventsource.muon.NoOpEventStreamProcessor;
import io.muoncore.newton.query.EventStreamIndexStore;
import io.muoncore.newton.query.InMemEventStreamIndexStore;
import io.muoncore.newton.saga.SagaIntegrationTests;
import io.muoncore.newton.saga.SagaLoader;
import io.muoncore.protocol.event.Event;
import io.muoncore.protocol.event.client.DefaultEventClient;
import io.muoncore.protocol.event.client.EventClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

//@Profile("test")
@Configuration
@EnableNewton
public class InMemoryTestConfiguration {

  @Autowired
  //Don't remove as it's required for tests - Spring lazy-loads beans & thus causes tests to fail as event store cannot be found!!!
  private TestEventStore testEventStore;

  @Bean
  public InMemDiscovery discovery() {
    return new InMemDiscovery();
  }

  @Bean
  public EventBus bus() {
    return new EventBus();
  }

  @Bean
  public AutoConfiguration config() {
    return MuonConfigBuilder.withServiceIdentifier("test-service").build();
  }

  @Bean
  public InMemTransport transport(AutoConfiguration config) {
    return new InMemTransport(config, bus());
  }

  @Bean
  public EventStreamIndexStore indexStore() {
    return new InMemEventStreamIndexStore();
  }

  @Bean
  public Muon muon(AutoConfiguration config, InMemTransport transport) {
    return new MultiTransportMuon(config, discovery(),
                                  Collections.singletonList(transport),
                                  new JsonOnlyCodecs()
    );
  }

  public static String EXISTING_EVENT_ID;

  @Bean
  public TestEventStore testEventStore(Codecs codecs) throws InterruptedException {
    AutoConfiguration config = MuonConfigBuilder.withServiceIdentifier("photon-mini")
                                                .withTags("eventstore")
                                                .build();
    //Another separate instance of muon is fired up to ensure....
    Muon muon = new MultiTransportMuon(config, discovery(),
                                       Collections.singletonList(new InMemTransport(config, bus())),
                                       new JsonOnlyCodecs());

    TestEventStore testEventStore = new TestEventStore(muon);

    createPreExistingEvent(codecs, testEventStore);

    return testEventStore;
  }

  private void createPreExistingEvent(Codecs codecs, TestEventStore testEventStore) {
    testEventStore.setOrderid(new AtomicLong(2));

    SagaIntegrationTests.OrderRequestedEvent orderRequestedEvent = new SagaIntegrationTests.OrderRequestedEvent();
    EXISTING_EVENT_ID = orderRequestedEvent.getId();

    Codecs.EncodingResult encode = codecs.encode(orderRequestedEvent, codecs.getAvailableCodecs());

    testEventStore.getHistory().add(new Event(
      "123456789",
      "OrderRequestedEvent",
      "TestAggregate",
      null,
      null,
      null,
      "faked-service",
      2L,
      System.currentTimeMillis(),
      codecs.decode(encode.getPayload(), encode.getContentType(), Map.class),
      null
    ));
  }

  @Bean
  public EventClient eventClient(Muon muon) {
    return new DefaultEventClient(muon);
  }

  @Bean
  public NewtonEventClient aggregateEventClient(EventClient eventClient) {
    return new NewtonEventClient(eventClient, "unknown-token");
  }

  @Bean
  public LockService lockService() throws Exception {
    return (name, exec) -> exec.execute((LockService.TaskLockControl) () -> {
    });
  }

  @Bean
  public SagaLoader sagaLoader() {
    return interest -> (Class) InMemoryTestConfiguration.class.getClassLoader().loadClass(interest.getSagaClassName());
  }

  @Bean
  @ConditionalOnMissingBean(EventStreamProcessor.class)
  public EventStreamProcessor eventStreamProcessor() {
    return new NoOpEventStreamProcessor();
  }
}

