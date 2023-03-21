package org.touchhome.bundle.rabbitmq;

import java.nio.file.Path;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.entity.dependency.DependencyExecutableInstaller;
import org.touchhome.bundle.api.setting.SettingPluginButton;
import org.touchhome.bundle.api.setting.SettingPluginText;
import org.touchhome.bundle.hquery.hardware.other.MachineHardwareRepository;
import org.touchhome.bundle.rabbitmq.setting.RabbitMQInstallSetting;
import org.touchhome.bundle.rabbitmq.setting.RabbitMQPathSetting;
import org.touchhome.bundle.api.ui.field.ProgressBar;

@Log4j2
@Component
public class RabbitMQDependencyExecutableInstaller extends DependencyExecutableInstaller {

  @Override
  public String getName() {
    return "rabbitmq-server";
  }

  @Override
  public Path installDependencyInternal(@NotNull EntityContext entityContext, @NotNull ProgressBar progressBar) {
    if (SystemUtils.IS_OS_LINUX) {
      MachineHardwareRepository machineHardwareRepository = entityContext.getBean(MachineHardwareRepository.class);
      machineHardwareRepository.installSoftware(getName(), 600);
      machineHardwareRepository.enableAndStartSystemctl(getName());
    }
    return null;
  }

  @Override
  protected void afterDependencyInstalled(@NotNull EntityContext entityContext, Path path) {
  }

  @Override
  public boolean checkWinDependencyInstalled(MachineHardwareRepository repository, @NotNull Path targetPath) {
    return !repository.isSoftwareInstalled(getName());
  }

  @Override
  public @NotNull Class<? extends SettingPluginText> getDependencyPluginSettingClass() {
    return RabbitMQPathSetting.class;
  }

  @Override
  public Class<? extends SettingPluginButton> getInstallButton() {
    return RabbitMQInstallSetting.class;
  }
}
