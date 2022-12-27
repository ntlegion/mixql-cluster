package org.mixql.cluster

import org.mixql.core.context.gtype.Type
import org.mixql.core.engine.Engine
import org.mixql.protobuf.ProtoBufConverter
import org.mixql.protobuf.messages.clientMsgs
import org.zeromq.{SocketType, ZMQ}

import java.io.File
import java.net.{InetSocketAddress, SocketAddress}
import java.nio.channels.{ServerSocketChannel, SocketChannel}
import scala.concurrent.Future

//if start script name is not none then client must start remote engine by executing script
//which is {basePath}/{startScriptName}. P.S executor will be ignored
//if executor is not none and startScriptName is none then execute it in scala future
//if executor is none and startScript is none then just connect
class ClientModule(
  clientName: String,
  moduleName: String,
  startScriptName: Option[String],
  executor: Option[IExecutor],
  host: String,
  portFrontend: Int,
  portBackend: Int,
  basePath: File
) extends Engine
    with java.lang.AutoCloseable {
  var client: ZMQ.Socket = null
  var ctx: ZMQ.Context = null

  var clientRemoteProcess: sys.process.Process = null
  var clientFuture: Future[Unit] = null
  var started: Boolean = false

  override def name: String = clientName

  override def execute(stmt: String): Type = {
    import org.mixql.protobuf.messages.clientMsgs
    import org.mixql.protobuf.RemoteMsgsConverter
    sendMsg(clientMsgs.Execute(stmt))
    RemoteMsgsConverter.toGtype(recvMsg())
  }

  override def setParam(name: String, value: Type): Unit = {
    import org.mixql.protobuf.messages.clientMsgs
    import org.mixql.protobuf.RemoteMsgsConverter

    sendMsg(
      clientMsgs.SetParam(
        name,
        Some(
          com.google.protobuf.any.Any
            .pack(RemoteMsgsConverter.toAnyMessage(value))
        )
      )
    )
    recvMsg() match
      case clientMsgs.ParamWasSet(_) =>
      case clientMsgs.Error(msg, _)  => throw Exception(msg)
      case a: scala.Any =>
        throw Exception(
          s"engine-client-module: setParam error:  " +
            s"error while receiving confirmation that param was set: got ${a.toString}," +
            " when ParamWasSet or Error messages were expected"
        )
  }

  override def getParam(name: String): Type = {
    import org.mixql.protobuf.messages.clientMsgs
    import org.mixql.protobuf.RemoteMsgsConverter

    sendMsg(clientMsgs.GetParam(name))
    RemoteMsgsConverter.toGtype(recvMsg())
  }

  override def isParam(name: String): Boolean = {
    import org.mixql.core.context.gtype
    import org.mixql.protobuf.messages.clientMsgs
    import org.mixql.protobuf.RemoteMsgsConverter

    sendMsg(clientMsgs.IsParam(name))
    RemoteMsgsConverter.toGtype(recvMsg()).asInstanceOf[gtype.bool].value
  }

  def sendMsg(msg: scalapb.GeneratedMessage): Unit = {
    if !started then
      startModuleClient()
      ctx = ZMQ.context(1)
      client = ctx.socket(SocketType.REQ)
      // set id for client
      client.setIdentity(clientName.getBytes)
      println(
        "server: Clientmodule " + clientName + " connected to " +
          s"tcp://$host:$portFrontend " + client
            .connect(s"tcp://$host:$portFrontend")
      )
      started = true
    end if
    println(
      "server: Clientmodule " + clientName + " sending identity of remote module " + moduleName + " " +
        client.send(moduleName.getBytes, ZMQ.SNDMORE)
    )
    println(
      "server: Clientmodule " + clientName + " sending empty frame to remote module " + moduleName + " " +
        client.send("".getBytes, ZMQ.SNDMORE)
    )
    println(
      "server: Clientmodule " + clientName + " sending protobuf message to remote module " + moduleName + " " +
        client.send(ProtoBufConverter.toArray(msg).get, 0)
    )
  }

  def recvMsg(): scalapb.GeneratedMessage = {
    ProtoBufConverter.unpackAnyMsg(client.recv(0))
  }

  def startModuleClient() = {
    startScriptName match
      case Some(scriptName) =>
        println(
          s"server: ClientModule: $clientName trying to  start remote module $moduleName at " + host +
            " and port at " + portBackend + " in " + basePath.getAbsolutePath + " by executing script " +
            scriptName + " in directory " + basePath.getAbsolutePath
        )
        clientRemoteProcess = CmdOperations.runCmdNoWait(
          Some(
            s"$scriptName.bat --port $portBackend --host $host --identity $moduleName"
          ),
          Some(
            s"$scriptName --port $portBackend --host $host --identity $moduleName"
          ),
          basePath
        )
      case None =>
        executor match
          case Some(engine) =>
            println(
              s"server: ClientModule: $clientName trying to  start module $moduleName at " + host +
                " and port at " + portBackend + " in " + basePath.getAbsolutePath + " by executing in scala future"
            )
            clientFuture = engine.start(moduleName, host, portBackend.toString)
          case None =>
  }

  override def close() = {
    println(s"Server: ClientModule: $clientName: Executing close")
    if (client != null) {
      println(s"Server: ClientModule: $clientName: close client socket")
      client.close()
    }
    if (ctx != null) {
      println(s"Server: ClientModule: $clientName: close context")
      ctx.close()
    }

    //    if (clientRemoteProcess.isAlive()) clientRemoteProcess.exitValue()
    //    println(s"server: ClientModule: $clientName: Remote client was shutdown")

  }
}
