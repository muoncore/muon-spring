package io.muoncore.newton.todo;

import io.muoncore.newton.EventHandler;
import io.muoncore.newton.StreamSubscriptionManager;
import io.muoncore.newton.query.SharedDatastoreView;
import io.muoncore.newton.support.DocumentId;
import io.muoncore.newton.support.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class TaskViewStore extends SharedDatastoreView {

  private MongoTemplate mongoTemplate;

  @Autowired
  public TaskViewStore(StreamSubscriptionManager streamSubscriptionManager, MongoTemplate mongoTemplate1) throws IOException {
    super(streamSubscriptionManager);
    this.mongoTemplate = mongoTemplate1;
  }

//  @Override
//  protected Collection<Class<? extends AggregateRoot>> aggregateRoots() {
//    return Collections.singletonList(Task.class);
//  }


  @Override
  protected String[] getStreams() {
    return new String[]{"newton-sample/Task"};
  }

  public TaskView findById(DocumentId id) {
    return mongoTemplate.findById(id, TaskView.class);
  }

  public List<TaskView> listAll() {
    return mongoTemplate.findAll(TaskView.class);
  }

  @EventHandler
  public void handle(TaskCreatedEvent event) {
    if (TenantContextHolder.getTenantId() == null){
      throw new IllegalStateException("Tenant context unavailable!!!!");
    }
    mongoTemplate.save(new TaskView(event.getId(), event.getDescription(), TenantContextHolder.getTenantId()));
  }

  @EventHandler
  public void handle(TaskDescriptionChangedEvent event) {
    Update update = new Update();
    update.set("description", event.getDescription());
    mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(event.getId().getObjectId())), update, Task.class);
  }
}
