package org.homio.bundle.rabbitmq;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.SystemUtils;
import org.homio.api.Context;
import org.homio.api.entity.types.StorageEntity;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.service.EntityService;
import org.homio.api.ui.UISidebarChildren;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.UIFieldPort;
import org.homio.api.ui.field.UIFieldType;
import org.homio.api.ui.field.action.UIContextMenuAction;
import org.homio.api.util.Lang;
import org.homio.api.util.SecureString;
import org.homio.bundle.rabbitmq.workspace.Scratch3RabbitMQBlocks;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"JpaAttributeMemberSignatureInspection", "JpaAttributeTypeInspection", "unused"})
@Getter
@Setter
@Entity
@Accessors(chain = true)
@UISidebarChildren(icon = "fas fa-envelope-square", color = Scratch3RabbitMQBlocks.COLOR)
public class RabbitMQClientEntity extends StorageEntity implements
  EntityService<RabbitMQService> {

  @UIField(order = 1, hideInEdit = true, hideOnEmpty = true, fullWidth = true, bg = "#334842", type = UIFieldType.HTML)
  public final String getDescription() {
    return Lang.getServerMessage("rabbitmq.description");
  }

  @Override
  protected @NotNull String getDevicePrefix() {
    return "rmq";
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
    setJsonDataSecure("pwd", value);
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
  public ActionResponseModel testConnection() {
    getService().testServiceWithSetStatus();
    return ActionResponseModel.success();
  }

  @Override
  public long getEntityServiceHashCode() {
    return getJsonDataHashCode("used", "pwd", "port", "host", "apiPort");
  }

  @Override
  public @NotNull Class<RabbitMQService> getEntityServiceItemClass() {
    return RabbitMQService.class;
  }

  @Override
  @SneakyThrows
  public RabbitMQService createService(@NotNull Context context) {
    return new RabbitMQService(context, this);
  }

  @UIContextMenuAction(value = "install_rabbitmq", icon = "fas fa-play")
  public ActionResponseModel install(Context context) {
    if (SystemUtils.IS_OS_LINUX) {
      var hardware = context.hardware();
      if (!hardware.isSoftwareInstalled("rabbitmq-server")) {
        context.bgp().runWithProgress("install-rabbitmq-server").execute(progressBar -> {
          hardware.installSoftware("rabbitmq-server", 300, progressBar);
          hardware.enableAndStartSystemCtl("rabbitmq-server");
        });
        return ActionResponseModel.showInfo("Installing RabbitMQ...");
      } else {
        return ActionResponseModel.showError("RabbitMQ already installed");
      }
    } else {
      return ActionResponseModel.showError("Unable to install RabbitMQ for non-linux env");
    }
  }
}
