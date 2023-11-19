package modules

import play.ApplicationLoader.Context
import play.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}
import scalikejdbc.config.DBs

class AppApplicationLoader extends GuiceApplicationLoader {
  override  def builder(context: Context): GuiceApplicationBuilder = {

    DBs.setupAll()
    super.builder(context)
  }
}
