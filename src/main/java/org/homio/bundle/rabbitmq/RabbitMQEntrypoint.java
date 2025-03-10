package org.homio.bundle.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.homio.api.AddonConfiguration;
import org.homio.api.AddonEntrypoint;
import org.homio.api.Context;
import org.homio.api.util.HardwareUtils;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Component
@AddonConfiguration
@RequiredArgsConstructor
public class RabbitMQEntrypoint implements AddonEntrypoint {

  private final Context context;

  @SneakyThrows
  public void init() {
    context.ui().console().registerPluginName("RABBIT_MQ");
    context.bgp().builder("check-rabbitmq").execute(() -> {
      Set<String> existIps = context.db().findAll(RabbitMQClientEntity.class).stream()
        .map(RabbitMQClientEntity::getHostname).collect(Collectors.toSet());
      HardwareUtils.scanForDevice(context, 5672, "RABBIT_MQ", ip -> {
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
        context.db().save(entity);
      });
    });
  }

  @Override
  public void destroy() {
    context.ui().console().unRegisterPlugin("RABBIT_MQ");
  }
}
