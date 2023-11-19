package modules

import com.google.inject.AbstractModule
import controllers.LogController
import dao.LogDao
import play.api.ApplicationLoader
import play.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}
import services.LogService

class LogModule extends  AbstractModule{
  override def configure() = {
    bind(classOf[LogController]).asEagerSingleton()
    bind(classOf[LogService]).asEagerSingleton()
    bind(classOf[LogDao]).asEagerSingleton()
  }
}

//class MyApplicationLoader extends GuiceApplicationLoader {
//  override protected def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
//    initialBuilder
//      .in(context.environment)
//      .loadConfig(context.initialConfiguration)
//      .overrides(overrides(context): _*)
//      .bindings(new LogModule)
//  }
//}
