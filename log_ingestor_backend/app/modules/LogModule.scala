package modules

import com.google.inject.AbstractModule
import controllers.LogController
import dao.LogDao
import services.{KafkaMessageQueuePublisher, LogIngestionService, LogService, MessageQueuePublisher}

class LogModule extends AbstractModule {
  override def configure() = {
    bind(classOf[MessageQueuePublisher]).to(classOf[KafkaMessageQueuePublisher]).asEagerSingleton()
    bind(classOf[LogIngestionService]).asEagerSingleton()
    bind(classOf[LogService]).asEagerSingleton()
    bind(classOf[LogDao]).asEagerSingleton()
    bind(classOf[LogController]).asEagerSingleton()
  }
}

