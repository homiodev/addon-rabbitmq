package org.touchhome.bundle.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import java.util.Objects;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.service.EntityService;
import org.touchhome.bundle.rabbitmq.header.RabbitMqPublishMessageConsolePlugin;

public class RabbitMQService implements EntityService.ServiceInstance<RabbitMQClientEntity> {

  private @Nullable Channel channel;
  @Getter
  private RabbitMQClientEntity entity;
  private Client apiClient;
  private Connection connection;

  public RabbitMQService(EntityContext entityContext, RabbitMQClientEntity entity) {
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
    boolean updated = false;
    if (!Objects.equals(this.entity.getHostname(), entity.getHostname()) ||
        !Objects.equals(this.entity.getApiPort(), entity.getApiPort()) ||
        !Objects.equals(this.entity.getPort(), entity.getPort()) ||
        !Objects.equals(this.entity.getUser(), entity.getUser()) ||
        !Objects.equals(this.entity.getPassword().asString(), entity.getPassword().asString())) {
      this.destroy();
      updated = true;
    }
    this.entity = entity;
    return updated;
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
