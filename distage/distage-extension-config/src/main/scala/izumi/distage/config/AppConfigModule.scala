package izumi.distage.config

import izumi.distage.config.model.AppConfig
import izumi.distage.model.definition.ModuleDef

class AppConfigModule(appConfig: AppConfig) extends ModuleDef {
  make[AppConfig].fromValue(appConfig)

  def this(config: DistageConfigImpl) = this(AppConfig(config))
}

object AppConfigModule {
  def apply(appConfig: AppConfig): AppConfigModule = new AppConfigModule(appConfig)
  def apply(config: DistageConfigImpl): AppConfigModule = new AppConfigModule(config)
}
