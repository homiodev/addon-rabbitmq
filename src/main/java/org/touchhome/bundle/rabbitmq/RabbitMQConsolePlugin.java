package org.touchhome.bundle.rabbitmq;

import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.BindingInfo;
import com.rabbitmq.http.client.domain.ConsumerDetails;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.QueueInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.console.ConsolePluginTable;
import org.touchhome.bundle.api.model.HasEntityIdentifier;
import org.touchhome.bundle.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.touchhome.bundle.api.ui.field.UIField;
import org.touchhome.bundle.api.ui.field.UIFieldType;
import org.touchhome.bundle.rabbitmq.RabbitMQConsolePlugin.RabbitMQPluginModel;
import org.touchhome.bundle.rabbitmq.header.RabbitMqPublishMessageConsolePlugin;

@RequiredArgsConstructor
public class RabbitMQConsolePlugin implements ConsolePluginTable<RabbitMQPluginModel> {

    @Getter
    private final EntityContext entityContext;
    private final RabbitMQService rabbitMQService;

    @Override
    public String getParentTab() {
        return "RABBIT_MQ";
    }

    @Override
    public int order() {
        return 2000;
    }

    @Override
    @SneakyThrows
    public Collection<RabbitMQPluginModel> getValue() {
        Client client = rabbitMQService.getApiClient();
        List<RabbitMQPluginModel> list = new ArrayList<>();
        for (QueueInfo queue : client.getQueues()) {
            list.add(new RabbitMQPluginModel(queue));
        }
        Map<String, List<BindingInfo>> bindingInfoMap = client.getBindings().stream().collect(Collectors.groupingBy(BindingInfo::getSource));
        for (ExchangeInfo exchange : client.getExchanges()) {
            if (StringUtils.isNotEmpty(exchange.getName())) {
                list.add(new RabbitMQPluginModel(exchange, bindingInfoMap.get(exchange.getName())));
            }
        }
        Collections.sort(list);
        return list;
    }

    @Override
    public Class<RabbitMQPluginModel> getEntityClass() {
        return RabbitMQPluginModel.class;
    }

    @Override
    public Map<String, Class<? extends ConsoleHeaderSettingPlugin<?>>> getHeaderActions() {
        Map<String, Class<? extends ConsoleHeaderSettingPlugin<?>>> headerActions = new LinkedHashMap<>();
        headerActions.put("publish", RabbitMqPublishMessageConsolePlugin.class);
        return headerActions;
    }

    @Getter
    @NoArgsConstructor
    public static class RabbitMQPluginModel implements HasEntityIdentifier, Comparable<RabbitMQPluginModel> {

        @UIField(order = 8, type = UIFieldType.Chips)
        private final List<String> content = new ArrayList<>();
        @UIField(order = 0)
        private String node;
        @UIField(order = 1)
        private String type;
        @UIField(order = 2, style = "max-width:150px;overflow:hidden;")
        private String name;
        @UIField(order = 3)
        private String vHost;
        @UIField(order = 4)
        private long consumerCount;
        @UIField(order = 5)
        private String idleSince;
        @UIField(order = 6)
        private boolean durable;

        public RabbitMQPluginModel(QueueInfo queue) {
            this.node = "Queue";
            this.type = queue.getType();
            this.name = queue.getName();
            this.vHost = queue.getVhost();
            this.consumerCount = queue.getConsumerCount();
            this.idleSince = queue.getIdleSince();
            this.durable = queue.isDurable();
            this.content.add(String.format("Ready: %d, Unacked: %d, Total: %d", queue.getMessagesReady(),
                queue.getMessagesUnacknowledged(), queue.getTotalMessages()));
            if (queue.getConsumerDetails() != null) {
                for (ConsumerDetails consumer : queue.getConsumerDetails()) {
                    this.content.add(String.format("Tag: %s, PrefetchCount: %s, Host: %s", consumer.getConsumerTag(), consumer.getPrefetchCount(),
                        consumer.getChannelDetails().getPeerHost()));
                }
            }
        }

        public RabbitMQPluginModel(ExchangeInfo exchange, List<BindingInfo> bindingInfo) {
            this.node = "Exchange";
            this.type = exchange.getType();
            this.name = exchange.getName();
            this.vHost = exchange.getVhost();
            if (bindingInfo != null) {
                for (BindingInfo info : bindingInfo) {
                    this.content.add(String.format("Dest: %s. RoutingKey: %s.", info.getDestination(),
                        info.getDestination().equals(info.getRoutingKey()) ? "same" : info.getRoutingKey()));
                }
            }
        }

        @Override
        public String getEntityID() {
            return name;
        }

        @Override
        public int compareTo(@NotNull RabbitMQPluginModel o) {
            return (node + name).compareTo((o.node + o.name));
        }
    }
}
