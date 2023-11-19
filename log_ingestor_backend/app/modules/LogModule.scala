package modules

import com.google.inject.AbstractModule
import controllers.LogController
import dao.LogDao
import services.LogService

class LogModule extends  AbstractModule{
  override def configure() = {
    bind(classOf[LogController]).asEagerSingleton()
    bind(classOf[LogService]).asEagerSingleton()
    bind(classOf[LogDao]).asEagerSingleton()
  }
}

