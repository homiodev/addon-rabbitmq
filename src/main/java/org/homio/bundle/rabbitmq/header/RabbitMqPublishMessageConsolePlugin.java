package org.homio.bundle.rabbitmq.header;

import org.homio.api.Context;
import org.homio.api.model.Icon;
import org.homio.api.setting.SettingPluginButton;
import org.homio.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.homio.api.ui.field.action.ActionInputParameter;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class RabbitMqPublishMessageConsolePlugin implements ConsoleHeaderSettingPlugin<JSONObject>, SettingPluginButton {

  @Override
  public Icon getIcon() {
    return new Icon("fas fa-upload");
  }

  @Override
  public int order() {
    return 100;
  }

  @Override
  public @Nullable String getConfirmMsg() {
    return "";
  }

  @Override
  public String getConfirmTitle() {
    return "rabbitmq.PUBLISH_TITLE";
  }

  @Override
  public List<ActionInputParameter> getInputParameters(Context context, String value) {
    return Arrays.asList(
      ActionInputParameter.text("exchange", ""),
      ActionInputParameter.text("routing_key", ""),
      ActionInputParameter.text("payload", "")
    );
  }
}
