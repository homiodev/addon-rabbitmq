package org.touchhome.bundle.rabbitmq.setting;

import org.apache.commons.lang3.SystemUtils;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.setting.SettingPluginButton;
import org.touchhome.bundle.rabbitmq.RabbitMQDependencyExecutableInstaller;

public class RabbitMQInstallSetting implements SettingPluginButton {

  @Override
  public int order() {
    return 100;
  }

  @Override
  public String getIcon() {
    return "fas fa-play";
  }

  @Override
  public boolean isVisible(EntityContext entityContext) {
    return SystemUtils.IS_OS_LINUX &&
        entityContext.getBean(RabbitMQDependencyExecutableInstaller.class)
            .isRequireInstallDependencies(entityContext, true);
  }
}
