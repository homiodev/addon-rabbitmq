package org.homio.bundle.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import lombok.SneakyThrows;
import org.homio.api.Context;
import org.homio.api.model.Icon;
import org.homio.api.service.EntityService;
import org.homio.bundle.rabbitmq.header.RabbitMqPublishMessageConsolePlugin;
import org.homio.bundle.rabbitmq.workspace.Scratch3RabbitMQBlocks;
import org.jetbrains.annotations.Nullable;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.homio.api.ui.UI.Color.RED;

public class RabbitMQService extends EntityService.ServiceInstance<RabbitMQClientEntity> {

  private @Nullable Channel channel;
  private Client apiClient;
  private Connection connection;
  private long hashCode;

  public RabbitMQService(Context entityContext, RabbitMQClientEntity entity) {
    super(entityContext, entity, true, "RabbitMQ");
    RabbitMQConsolePlugin rmqPlugin = new RabbitMQConsolePlugin(entityContext, this);
    entityContext.ui().console().registerPlugin(entity.getEntityID(), rmqPlugin);

    entityContext.setting().listenValue(RabbitMqPublishMessageConsolePlugin.class, entity.getEntityID() + "-publish",
      json -> {
        if (json != null) {
          getChannel().basicPublish(
            json.getString("exchange"),
            json.getString("routing_key"),
            null,
            json.getString("payload").getBytes());
        }
      });
  }

  @SneakyThrows
  public Client getApiClient() {
    if (this.apiClient == null) {
      this.apiClient = new Client(
        new ClientParameters()
          .url(String.format("http://%s:%d/api/", entity.getHostname(), entity.getApiPort()))
          .username(entity.getUser())
          .password(entity.getPassword().asString()));
    }
    return this.apiClient;
  }

  @SneakyThrows
  public Channel getChannel() {
    if (this.channel == null || !this.channel.isOpen()) {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(entity.getHostname());
      factory.setPort(entity.getPort());
      factory.setUsername(entity.getUser());
      factory.setPassword(entity.getPassword().asString());
      this.connection = factory.newConnection();
      this.channel = connection.createChannel();
    }
    return this.channel;
  }

  public void updateNotificationBlock() {
    var icon = new Icon("fas fa-envelope-square", Scratch3RabbitMQBlocks.COLOR);
    context.ui().notification().addBlock("rabbitmq", "RabbitMQ", icon, builder -> {
      builder.setStatus(getEntity().getStatus());
      if (!getEntity().getStatus().isOnline()) {
        var info = defaultIfEmpty(getEntity().getStatusMessage(), "Unknown error");
        builder.addInfo(String.valueOf(info.hashCode()), new Icon("fas fa-exclamation", RED), info);
      } else {
        String version = context.hardware().execute("rabbitmqctl version");
        builder.setVersion(version);
      }
    });
  }

  @Override
  public void destroy(boolean forRestart, @Nullable Exception ex) throws Exception {
    if (channel != null && channel.isOpen()) {
      channel.close();
      connection.close();
    }
  }

  @Override
  public void testService() {
    getChannel();
  }

  @Override
  protected void initialize() {
    getChannel();
  }
}
