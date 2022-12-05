import org.mixql.core.context.Context
import org.mixql.core.context.gtype.Type
import org.mixql.core.engine.Engine
import org.mixql.core.function.{ArrayFunction, StringFunction}
import scala.collection.mutable.{Map => MutMap}

class ClusterContext(
                      engines: MutMap[String, Engine],
                      defaultEngine: String,
                      broker: BrokerModule,
                      variables: MutMap[String, Type] = MutMap[String, Type](),
                      functions: MutMap[String, Any] = MutMap[String, Any](
                        "ascii" -> StringFunction.ascii,
                        "base64" -> StringFunction.base64,
                        "concat" -> StringFunction.concat,
                        "concat_ws" -> StringFunction.concat_ws,
                        "length" -> StringFunction.length,
                        "substr" -> StringFunction.substr,
                        "format_number" -> StringFunction.formatNumber,
                        "size" -> ArrayFunction.size,
                        "sort" -> ArrayFunction.sort
                      )
                    ) extends Context(engines, defaultEngine, variables, functions) with java.lang.AutoCloseable {
  override def close() = {
    println("Cluster Context: starting close")
    println("Cluster context: close broker")
    broker.close()
    println("Cluster context: stop engines, if they were not closed before by shutdown command")
    engines.values.foreach(
      engine => {
        if engine.isInstanceOf[ClientModule] then
          val remoteEngineClient: ClientModule = engine.asInstanceOf[ClientModule]
          println(s"Cluster context: stopping remote engine " + remoteEngineClient.name)
          remoteEngineClient.close()
      }
    )
  }
}
