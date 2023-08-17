package org.homio.bundle.rabbitmq;

import com.pivovarit.function.ThrowingConsumer;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.SystemUtils;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.EntityContextHardware;
import org.homio.bundle.api.entity.types.StorageEntity;
import org.homio.bundle.api.model.ActionResponseModel;
import org.homio.bundle.api.service.EntityService;
import org.homio.bundle.api.ui.UISidebarChildren;
import org.homio.bundle.api.ui.field.ProgressBar;
import org.homio.bundle.api.ui.field.UIField;
import org.homio.bundle.api.ui.field.UIFieldPort;
import org.homio.bundle.api.ui.field.UIFieldType;
import org.homio.bundle.api.ui.field.action.UIContextMenuAction;
import org.homio.bundle.api.util.Lang;
import org.homio.bundle.api.util.SecureString;
import org.homio.bundle.rabbitmq.workspace.Scratch3RabbitMQBlocks;

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

  @UIContextMenuAction(value = "install_rabbitmq", icon = "fas fa-play")
  public ActionResponseModel install(EntityContext entityContext) {
    if (SystemUtils.IS_OS_LINUX) {
      EntityContextHardware hardware = entityContext.hardware();
      if (!hardware.isSoftwareInstalled("rabbitmq-server")) {
        entityContext.bgp().runWithProgress("install-rabbitmq-server", false, progressBar -> {
          hardware.installSoftware("rabbitmq-server", 300, progressBar);
          hardware.enableAndStartSystemCtl("rabbitmq-server");
        }, exception -> {
          if (exception != null) {
            entityContext.ui().sendErrorMessage("Error during install RabbitMQ", exception);
          } else {
            entityContext.ui().sendSuccessMessage("RabbitMQ installed successfully");
          }
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
