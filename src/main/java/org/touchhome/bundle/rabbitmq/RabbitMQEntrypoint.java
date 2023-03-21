package org.touchhome.bundle.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.touchhome.bundle.api.BundleEntrypoint;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.util.TouchHomeUtils;

@Log4j2
@Component
@RequiredArgsConstructor
public class RabbitMQEntrypoint implements BundleEntrypoint {

  private final EntityContext entityContext;

  @SneakyThrows
  public void init() {
    entityContext.ui().registerConsolePluginName("RABBIT_MQ");
    entityContext.bgp().builder("check-rabbitmq").execute(() -> {
      Set<String> existIps = entityContext.findAll(RabbitMQClientEntity.class).stream()
          .map(RabbitMQClientEntity::getHostname).collect(Collectors.toSet());
      TouchHomeUtils.scanForDevice(entityContext, 5672, "RABBIT_MQ", ip -> {
        if (existIps.contains(ip)) {
          return false;
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(ip);
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection conn = factory.newConnection()) {
          conn.createChannel();
        }
        return true;
      }, ip -> {
        RabbitMQClientEntity entity = new RabbitMQClientEntity();
        entity.setHostname(ip);
        entityContext.save(entity);
      });
    });
  }

  @Override
  @SneakyThrows
  public void destroy() {
  }

  @Override
  public String getBundleId() {
    return "rabbitmq";
  }

  @Override
  public int order() {
    return 2000;
  }
}
