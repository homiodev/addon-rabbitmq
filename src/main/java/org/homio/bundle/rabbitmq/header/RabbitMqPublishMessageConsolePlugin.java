package org.homio.bundle.rabbitmq.header;

import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.setting.SettingPluginButton;
import org.homio.bundle.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.homio.bundle.api.ui.field.action.ActionInputParameter;

public class RabbitMqPublishMessageConsolePlugin implements ConsoleHeaderSettingPlugin<JSONObject>, SettingPluginButton {

  @Override
  public String getIcon() {
    return "fas fa-upload";
  }

  @Override
  public int order() {
    return 100;
  }

  @Override
  public String getConfirmTitle() {
    return "rabbitmq.PUBLISH_TITLE";
  }

  @Override
  public List<ActionInputParameter> getInputParameters(EntityContext entityContext, String value) {
    return Arrays.asList(
        ActionInputParameter.text("exchange", ""),
        ActionInputParameter.text("routing_key", ""),
        ActionInputParameter.text("payload", "")
    );
  }
}
