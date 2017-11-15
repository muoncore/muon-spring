package io.muoncore.newton.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;

@ActiveProfiles({"test"})
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CommandFactoryTest.class, CommandConfiguration.class})
@Configuration
public class CommandFactoryTest {

	@Autowired
	private CommandFactory commandFactory;

	@Test
	public void create() throws Exception {
		Command cmd = commandFactory.create(TestCommand.class);
		Assert.assertNotNull(cmd);
		//THEN - expect no exception
		cmd.execute();
	}

	@Test
	public void createFromPayload() throws Exception {
		Command cmd = commandFactory.create(TestPayloadCommand.class, new TestRequest("AAA"), null);
		Assert.assertNotNull(cmd);
		//THEN - expect no exception
		cmd.execute();
	}

	@Test
	public void createFromCommandDefinition() throws Exception {
		final TestRequest payload = new TestRequest("AAA");
		final String id = UUID.randomUUID().toString();
    final Class<TestPayloadCommand> type = TestPayloadCommand.class;
		Command cmd = commandFactory.create(type, payload, id, "tenantId");
		Assert.assertNotNull(cmd);
		//THEN - expect no exception
		cmd.execute();
	}

	@Test
	public void createFromCommandDefinition_withAdditionalProperties() throws Exception {
		Command cmd = commandFactory.create(TestPayloadCommand.class, null, null, Collections.singletonMap("prop1", "Value"), null);
		Assert.assertNotNull(cmd);
		//THEN - expect no exception
		cmd.execute();
	}

  @Test
  public void createFromStringIdUsingReflection() throws Exception {
    final String id = "1234";
    Command cmd = commandFactory
      .create(TestCommand.class,null, id,null);
    Assert.assertNotNull(cmd);
    assertEquals(id, ((TestCommand)cmd).getId());
  }

  @Test
  public void createFromCustomIdUsingReflection() throws Exception {
    final CustomId id = new CustomId("1234");
    Command cmd = commandFactory
      .create(TestCommandWithCustomId.class,null, id,null);
    Assert.assertNotNull(cmd);
    assertEquals(id, ((TestCommandWithCustomId)cmd).getId());
  }


  @Test
  public void createFromAdditonalProperties_GenericList() throws Exception {
    ArrayList<String> list = new ArrayList<>();
    list.add("abc");
    Map<String,Object> additionalProperties = new HashMap<>();
    additionalProperties.put("list", list);

    Command cmd = commandFactory
      .create(TestCommand.class,null,null, additionalProperties, null);
    Assert.assertNotNull(cmd);
    //THEN
    assertEquals(list, ((TestCommand)cmd).getList());
  }


  //CONFIGURATION
	@Bean
	public TestPayloadCommand testPayloadCommand() {
		return new TestPayloadCommand();
	}

	@Bean
	public TestCommand testCommand() {
		return new TestCommand();
	}

	@Bean
	public TestIdCommand testIdCommand() {
		return new TestIdCommand();
	}

  @Bean
  public TestCommandWithCustomId testCommandWithCustomId() {
    return new TestCommandWithCustomId();
  }


  @Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TestRequest {

		private String prop1;
	}

  @Data
	public static class TestCommand implements Command {
		private String id;

		private List<String> list;


		public void setAd(String id) {
      throw new IllegalArgumentException("THIS SHOULD NOT BE CALLD");
    }

		@Override
		public void execute() {

		}
	}

  public static class TestCommandWithCustomId implements Command {

	  @Setter private CustomId id;

    @Override
    public void execute() {
    }

    public CustomId getId(){
      return this.id;
    }
  }


	public static class TestIdCommand implements Command {

		@Setter private String id;

		@Override
		public void execute() {
			if (id == null) {
				throw new IllegalStateException("Id must be specified");
			}
		}

	}


	public static class TestPayloadCommand implements Command {

    @Setter
		protected String id;
		@Setter
		private String prop1;
		@Setter
		private int prop2 = -1;

		@Override
		public void execute() {
			if (prop1 == null) {
				throw new IllegalStateException("prop1 not specified!");
			}
			if (prop2 != -1) {
				throw new IllegalStateException("prop2 not expected to be specified!");
			}
			System.out.println("RUNNING");
		}
	}

	public static class CustomId{

	  private String val;

	  public CustomId(String val){
	    this.val = val;
    }

    public String getVal() {
      return val;
    }
  }
}
