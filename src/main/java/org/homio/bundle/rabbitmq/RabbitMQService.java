package org.homio.bundle.rabbitmq;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import lombok.Getter;
import lombok.SneakyThrows;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.service.EntityService;
import org.homio.bundle.api.ui.UI.Color;
import org.homio.bundle.rabbitmq.header.RabbitMqPublishMessageConsolePlugin;
import org.homio.bundle.rabbitmq.workspace.Scratch3RabbitMQBlocks;
import org.jetbrains.annotations.Nullable;

public class RabbitMQService implements EntityService.ServiceInstance<RabbitMQClientEntity> {

  private @Nullable Channel channel;
  private final EntityContext entityContext;
  @Getter
  private RabbitMQClientEntity entity;
  private Client apiClient;
  private Connection connection;
  private long hashCode;

  public RabbitMQService(EntityContext entityContext, RabbitMQClientEntity entity) {
    this.entityContext = entityContext;
    this.entity = entity;
    RabbitMQConsolePlugin rmqPlugin = new RabbitMQConsolePlugin(entityContext, this);
    entityContext.ui().registerConsolePlugin(entity.getEntityID(), rmqPlugin);

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

  @Override
  @SneakyThrows
  public boolean entityUpdated(RabbitMQClientEntity entity) {
    long hashCode = entity.getJsonDataHashCode("host", "apiPort", "port", "user", "pwd");
    boolean reconfigure = this.hashCode != hashCode;
    this.hashCode = hashCode;
    this.entity = entity;
    if (reconfigure) {
      this.destroy();
    }
    updateNotificationBlock();
    return reconfigure;
  }

  private void updateNotificationBlock() {
    entityContext.ui().addNotificationBlock("rabbitmq", "RabbitMQ", "fas fa-envelope-square",
        Scratch3RabbitMQBlocks.COLOR, builder -> {
          builder.setStatus(getEntity().getStatus());
          if (!getEntity().getStatus().isOnline()) {
            builder.addInfo(defaultIfEmpty(getEntity().getStatusMessage(), "Unknown error"),
                Color.RED, "fas fa-exclamation", null);
          } else {
            String version = entityContext.hardware().execute("rabbitmqctl version");
            builder.setVersion(version);
          }
        });
  }

  @Override
  public void destroy() throws Exception {
    if (channel != null && channel.isOpen()) {
      channel.close();
      connection.close();
    }
  }

  @Override
  public boolean testService() {
    getChannel();
    return true;
  }
}
