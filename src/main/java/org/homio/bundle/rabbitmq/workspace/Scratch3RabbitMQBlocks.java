package org.homio.bundle.rabbitmq.workspace;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.homio.api.Context;
import org.homio.api.state.DecimalType;
import org.homio.api.state.State;
import org.homio.api.workspace.WorkspaceBlock;
import org.homio.api.workspace.scratch.MenuBlock;
import org.homio.api.workspace.scratch.Scratch3Block;
import org.homio.api.workspace.scratch.Scratch3ExtensionBlocks;
import org.homio.bundle.rabbitmq.RabbitMQClientEntity;
import org.homio.bundle.rabbitmq.RabbitMQEntrypoint;
import org.homio.bundle.rabbitmq.RabbitMQService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Getter
@Component
public class Scratch3RabbitMQBlocks extends Scratch3ExtensionBlocks {

  public static final String COLOR = "#B08907";

  private final RabbitMQEntrypoint rabbitMQEntrypoint;

  private final MenuBlock.ServerMenuBlock menuRmqClient;
  private final MenuBlock.StaticMenuBlock<ExchangeType> menuExchangeType;
  private final MenuBlock.StaticMenuBlock<AddRemove> menuAddRemove;
  private final MenuBlock.StaticMenuBlock<CountTypeMenu> menuCountType;

  public Scratch3RabbitMQBlocks(Context context, RabbitMQEntrypoint rabbitMQEntrypoint) {
    super(COLOR, context, rabbitMQEntrypoint);
    setParent(ScratchParent.communication);
    this.rabbitMQEntrypoint = rabbitMQEntrypoint;

    // menu
    this.menuRmqClient = menuServerItems("rmqClientMenu", RabbitMQClientEntity.class, "Select RMQ");
    this.menuExchangeType = menuStatic("exchangeType", ExchangeType.class, ExchangeType.Direct);
    this.menuAddRemove = menuStatic("addRemoveMenu", AddRemove.class, AddRemove.bind);
    this.menuCountType = menuStatic("countTypeMenu", CountTypeMenu.class, CountTypeMenu.messages);

    // blocks
    blockReporter(10, "queueCount", "Queue [QUEUE] [TYPE] count of [RMQ]", this::messageCountReporter, block -> {
      block.addArgument("RMQ", this.menuRmqClient);
      block.addArgument("QUEUE", "queue");
      block.addArgument("TYPE", this.menuCountType);
    });

    blockHat(20, "subscribe", "Subscribe [QUEUE] of [RMQ] | Value: [VALUE], Auto ack: [AUTO_ACK]",
      this::subscribeToValue, block -> {
        block.addArgument("RMQ", this.menuRmqClient);
        block.addArgument("QUEUE", "queue");
        block.addArgument("VALUE", ".*");
        block.addArgument("TIMEOUT", 0);
        block.addArgument("AUTO_ACK", true);
        block.appendSpace();
      });

    blockCommand(30, "send", "Send [MSG] of [RMQ] | Exchange: [EXCHANGE], RoutingKey: [ROUTING_KEY]",
      this::sendMessageCommand, block -> {
        addRabbitExchangeRabbit(block);
        block.addArgument("MSG", "message");
      });

    blockCommand(40, "rpc", "RPC [MSG] to [EXCHANGE] of [RMQ] | RoutingKey: [ROUTING_KEY]",
      this::sendMessageAndReceiveCommand, block -> {
        addRabbitExchangeRabbit(block);
        block.addArgument("MSG", "message");
        block.appendSpace();
      });

    blockCommand(45, "ensureExchangeQueueAndBind", "Declare [TYPE] exchange [EXCHANGE] and bind to queue [QUEUE] of [RMQ] | RoutingKey: [ROUTING_KEY]",
      this::declareAndBindHandler, block -> {
        addRabbitExchangeRabbit(block);
        block.addArgument("TYPE", menuExchangeType);
        block.addArgument("QUEUE", "queue");
      });

    blockCommand(50, "declareQueue", "Declare queue [QUEUE] of [RMQ] | Durable: [DURABLE], Auto delete: [AUTO_DELETE]",
      this::declareQueueHandler, block -> {
        block.addArgument("RMQ", this.menuRmqClient);
        block.addArgument("QUEUE", "queue");
        block.addArgument("DURABLE", true);
        block.addArgument("AUTO_DELETE", false);
      });

    blockCommand(60, "deleteQueue", "Delete queue [QUEUE] of [RMQ] | If unused: [UNUSED], If empty: [EMPTY]",
      this::deleteQueueHandler, block -> {
        block.addArgument("RMQ", this.menuRmqClient);
        block.addArgument("QUEUE", "queue");
        block.addArgument("UNUSED", false);
        block.addArgument("EMPTY", false);
        block.overrideColor("#C9001E");
      });

    blockCommand(70, "declareExchange", "Declare [TYPE] exchange [EXCHANGE] of [RMQ] | Durable: [DURABLE], Auto delete: [AUTO_DELETE]",
      this::declareExchangeHandler, block -> {
        block.addArgument("RMQ", this.menuRmqClient);
        block.addArgument("EXCHANGE", "exchange");
        block.addArgument("TYPE", menuExchangeType);
        block.addArgument("DURABLE", true);
        block.addArgument("AUTO_DELETE", false);
      });

    blockCommand(80, "deleteExchange", "Delete exchange [EXCHANGE] of [RMQ] | If unused: [UNUSED]",
      this::deleteExchangeHandler, block -> {
        block.addArgument("RMQ", this.menuRmqClient);
        block.addArgument("EXCHANGE", "exchange");
        block.addArgument("UNUSED", false);
        block.overrideColor("#C9001E");
      });

    blockCommand(90, "exchangeBind", "[OP] exchange S: [SOURCE], D: [DESTINATION], R: [ROUTING_KEY] of [RMQ]",
      this::bindUnbindExchangeHandler, block -> {
        block.addArgument("RMQ", this.menuRmqClient);
        block.addArgument("OP", this.menuAddRemove);
        block.addArgument("DESTINATION", "dest exchange");
        block.addArgument("SOURCE", "source exchange");
        block.addArgument("ROUTING_KEY", "route key");
        block.overrideColor("#CF9A4C");
      });

    blockCommand(100, "queueBind", "[OP] queue Q: [QUEUE], E: [EXCHANGE], R: [ROUTING_KEY] of [RMQ]",
      this::bindUnbindQueueHandler, block -> {
        addRabbitExchangeRabbit(block);
        block.addArgument("OP", this.menuAddRemove);
        block.addArgument("QUEUE", "queue");
        block.overrideColor("#CF9A4C");
      });
  }

  private void declareAndBindHandler(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);
    RabbitMQService service = rabbitMQClientEntity.getService();
    String queue = workspaceBlock.getInputStringRequired("QUEUE");
    String exchange = workspaceBlock.getInputStringRequired("EXCHANGE");
    service.getChannel().queueDeclare(queue, true, false, false, null);
    service.getChannel().exchangeDeclare(exchange,
      workspaceBlock.getMenuValue("TYPE", this.menuExchangeType).type, true, false, null);
    service.getChannel().queueBind(queue, exchange, workspaceBlock.getInputString("ROUTING_KEY"));
  }

  private State messageCountReporter(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);
    String name = workspaceBlock.getInputStringRequired("QUEUE");
    long value;
    Channel channel = rabbitMQClientEntity.getService().getChannel();
    if (workspaceBlock.getMenuValue("TYPE", this.menuCountType) == CountTypeMenu.messages) {
      value = channel.messageCount(name);
    } else {
      value = channel.consumerCount(name);
    }
    return new DecimalType(value);
  }

  private void deleteQueueHandler(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);
    Channel channel = rabbitMQClientEntity.getService().getChannel();
    channel.queueDelete(
      workspaceBlock.getInputStringRequired("QUEUE"),
      workspaceBlock.getInputBoolean("UNUSED"),
      workspaceBlock.getInputBoolean("EMPTY"));
  }

  private void deleteExchangeHandler(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);
    Channel channel = rabbitMQClientEntity.getService().getChannel();
    channel.exchangeDelete(
      workspaceBlock.getInputStringRequired("EXCHANGE"),
      workspaceBlock.getInputBoolean("UNUSED"));
  }

  private void bindUnbindExchangeHandler(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);
    String destination = workspaceBlock.getInputStringRequired("DESTINATION");
    String source = workspaceBlock.getInputStringRequired("SOURCE");
    String routingKey = workspaceBlock.getInputString("ROUTING_KEY");

    Channel channel = rabbitMQClientEntity.getService().getChannel();
    if (workspaceBlock.getMenuValue("OP", this.menuAddRemove) == AddRemove.bind) {
      channel.exchangeBind(destination, source, routingKey);
    } else {
      channel.exchangeUnbind(destination, source, routingKey);
    }
  }

  private void bindUnbindQueueHandler(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);
    String queue = workspaceBlock.getInputStringRequired("QUEUE");
    String exchange = workspaceBlock.getInputStringRequired("EXCHANGE");
    String routingKey = workspaceBlock.getInputString("ROUTING_KEY");

    Channel channel = rabbitMQClientEntity.getService().getChannel();
    if (workspaceBlock.getMenuValue("OP", this.menuAddRemove) == AddRemove.bind) {
      channel.queueBind(queue, exchange, routingKey);
    } else {
      channel.queueBind(queue, exchange, routingKey);
    }
  }

  private void declareExchangeHandler(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);
    Channel channel = rabbitMQClientEntity.getService().getChannel();
    channel.exchangeDeclare(
      workspaceBlock.getInputStringRequired("EXCHANGE"),
      workspaceBlock.getMenuValue("TYPE", this.menuExchangeType).type,
      workspaceBlock.getInputBoolean("DURABLE"),
      workspaceBlock.getInputBoolean("AUTO_DELETE"), null);
  }

  private void declareQueueHandler(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);
    Channel channel = rabbitMQClientEntity.getService().getChannel();
    channel.queueDeclare(
      workspaceBlock.getInputStringRequired("QUEUE"),
      workspaceBlock.getInputBoolean("DURABLE"), false,
      workspaceBlock.getInputBoolean("AUTO_DELETE"),
      null);
  }

  private RabbitMQClientEntity sendMessageCommand(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);

    Channel channel = rabbitMQClientEntity.getService().getChannel();
    channel.basicPublish(
      workspaceBlock.getInputStringRequired("EXCHANGE"),
      workspaceBlock.getInputString("ROUTING_KEY"),
      null,
      workspaceBlock.getInputStringRequired("MSG").getBytes()
    );
    return rabbitMQClientEntity;
  }

  void sendMessageAndReceiveCommand(WorkspaceBlock workspaceBlock) throws IOException {
    RabbitMQClientEntity rabbitMQClientEntity = workspaceBlock.getMenuValueEntityRequired("RMQ", this.menuRmqClient);
    Channel channel = rabbitMQClientEntity.getService().getChannel();
    String replyQueueName = channel.queueDeclare().getQueue();

    String corrId = UUID.randomUUID().toString();
    BasicProperties props = new BasicProperties
      .Builder()
      .correlationId(corrId)
      .replyTo(replyQueueName)
      .build();
    channel.basicPublish(workspaceBlock.getInputStringRequired("EXCHANGE"), workspaceBlock.getInputString("ROUTING_KEY"),
      props, workspaceBlock.getInputStringRequired("MSG").getBytes());

    String tag = "rmq-" + workspaceBlock.getId();
    var lock = workspaceBlock.getLockManager().getLock(workspaceBlock, tag);

    channel.basicConsume(replyQueueName, true, tag, (consumerTag, delivery) -> {
      if (delivery.getProperties().getCorrelationId().equals(corrId)) {
        lock.signalAll(new String(delivery.getBody(), StandardCharsets.UTF_8));
      }
    }, consumerTag -> {
    });

    workspaceBlock.onRelease(() -> {
      channel.basicCancel(tag);
    });

    workspaceBlock.subscribeToLock(lock, workspaceBlock.getInputInteger("EXCHANGE", 0), TimeUnit.SECONDS, () -> {
    });
  }

  private void addRabbitExchangeRabbit(Scratch3Block scratch3Block) {
    scratch3Block.addArgument("RMQ", this.menuRmqClient);
    scratch3Block.addArgument("EXCHANGE", "exchange");
    scratch3Block.addArgument("ROUTING_KEY", "");
  }

  private void subscribeToValue(WorkspaceBlock workspaceBlock) {
    workspaceBlock.handleNextOptional(next -> {
      Channel channel = ((RabbitMQClientEntity) workspaceBlock
        .getMenuValueEntityRequired("RMQ", this.menuRmqClient))
        .getService().getChannel();

      String queue = workspaceBlock.getInputStringRequired("QUEUE");
      String expectedValue = workspaceBlock.getInputString("VALUE");
      if (StringUtils.isEmpty(expectedValue)) {
        expectedValue = null;
      }
      String tag = "rmq-" + queue;
      channel.basicConsume(
        queue, workspaceBlock.getInputBoolean("AUTO_ACK"),
        tag, (consumerTag, message) -> {
          String value = new String(message.getBody(), StandardCharsets.UTF_8);
          workspaceBlock.getLockManager().signalAll(tag, value);
        }, consumerTag -> {
          throw new RuntimeException("Consumer has been cancelled by some reason");
        }
      );
      workspaceBlock.onRelease(() -> {
        channel.basicCancel(tag);
      });
      var lock = workspaceBlock.getLockManager().getLock(workspaceBlock,
        tag, expectedValue == null ? null : Pattern.compile(expectedValue));
      workspaceBlock.subscribeToLock(lock, next::handle);
    });
  }

  @AllArgsConstructor
  private enum ExchangeType {
    Direct(BuiltinExchangeType.DIRECT),
    Fanout(BuiltinExchangeType.FANOUT),
    Topic(BuiltinExchangeType.TOPIC),
    Headers(BuiltinExchangeType.HEADERS);

    private final BuiltinExchangeType type;
  }

  @AllArgsConstructor
  private enum AddRemove {
    bind, unbind
  }

  private enum CountTypeMenu {
    messages, consumers
  }
}
