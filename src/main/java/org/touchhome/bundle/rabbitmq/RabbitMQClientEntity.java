package org.touchhome.bundle.rabbitmq;

import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.entity.types.StorageEntity;
import org.touchhome.bundle.api.model.ActionResponseModel;
import org.touchhome.bundle.api.service.EntityService;
import org.touchhome.bundle.api.ui.UISidebarChildren;
import org.touchhome.bundle.api.ui.field.UIField;
import org.touchhome.bundle.api.ui.field.UIFieldPort;
import org.touchhome.bundle.api.ui.field.UIFieldType;
import org.touchhome.bundle.api.ui.field.action.UIContextMenuAction;
import org.touchhome.bundle.api.util.SecureString;
import org.touchhome.bundle.rabbitmq.workspace.Scratch3RabbitMQBlocks;
import org.touchhome.bundle.api.util.Lang;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@UISidebarChildren(icon = "fas fa-envelope-square", color = Scratch3RabbitMQBlocks.COLOR)
public class RabbitMQClientEntity extends StorageEntity<RabbitMQClientEntity> implements
    EntityService<RabbitMQService, RabbitMQClientEntity> {

  public static final String PREFIX = "rmq_";

  @Override
  public String getEntityPrefix() {
    return PREFIX;
  }

  @UIField(order = 1, hideInEdit = true, hideOnEmpty = true, fullWidth = true, bg = "#334842", type = UIFieldType.HTML)
  public final String getDescription() {
    return Lang.getServerMessage("rabbitmq.description");
  }

  @UIField(order = 30)
  public String getHostname() {
    return getJsonData("host", "127.0.0.1");
  }

  public void setHostname(String value) {
    setJsonData("host", value);
  }

  @Override
  public String getDefaultName() {
    return "RabbitMQ client";
  }

  @UIField(order = 40)
  @UIFieldPort
  public int getPort() {
    return getJsonData("port", 5672);
  }

  public void setPort(String value) {
    setJsonData("port", value);
  }

  @UIField(order = 50)
  public String getUser() {
    return getJsonData("user", "guest");
  }

  public void setUser(String value) {
    setJsonData("user", value);
  }

  @UIField(order = 60)
  public SecureString getPassword() {
    return getJsonSecure("pwd", "guest");
  }

  public void setPassword(String value) {
    setJsonData("pwd", value);
  }

  @UIField(order = 70)
  @UIFieldPort
  public int getApiPort() {
    return getJsonData("apiPort", 15672);
  }

  public void setApiPort(String value) {
    setJsonData("apiPort", value);
  }

  @UIContextMenuAction(value = "CHECK_DB_CONNECTION", icon = "fas fa-plug")
  public ActionResponseModel testConnection() throws Exception {
    getService().testServiceWithSetStatus();
    return ActionResponseModel.success();
  }

  @Override
  public Class<RabbitMQService> getEntityServiceItemClass() {
    return RabbitMQService.class;
  }

  @Override
  @SneakyThrows
  public RabbitMQService createService(EntityContext entityContext) {
    return new RabbitMQService(entityContext, this);
  }
}
