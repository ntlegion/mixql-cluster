package org.mixql.cluster
import org.mixql.protobuf.messages.clientMsgs

object RemoteMsgsConverter {

  import org.mixql.core.context.gtype
  import org.mixql.protobuf.ProtoBufConverter
  import org.mixql.protobuf.messages.clientMsgs
  import org.mixql.protobuf.messages.clientMsgs.AnyMsg

  def toGtype(remoteMsg: scalapb.GeneratedMessage): gtype.Type = {

    remoteMsg match
      case _: clientMsgs.NULL => gtype.Null
      case msg: clientMsgs.Bool => gtype.bool(msg.value)
      case msg: clientMsgs.Int => gtype.int(msg.value)
      case msg: clientMsgs.Double => gtype.double(msg.value)
      case msg: clientMsgs.String => gtype.string(msg.value)
      case msg: clientMsgs.Array => gtype.array(msg.arr.map(
        f => ProtoBufConverter.unpackAnyMsg(f.toByteArray) match
          case _: clientMsgs.NULL => gtype.Null
          case msg: clientMsgs.Bool => gtype.bool(msg.value)
          case msg: clientMsgs.Int => gtype.int(msg.value)
          case msg: clientMsgs.Double => gtype.double(msg.value)
          case msg: clientMsgs.String => gtype.string(msg.value)
          case a: Any => throw Exception(s"RemoteMsgsConverter: toGtype error:  " +
            s"execute error while unpacking array: got ${a.toString}, when type was expected")
      ).toArray)
      case clientMsgs.Error(msg, _) => throw Exception(msg)
      case a: scala.Any => throw Exception(s"RemoteMsgsConverter: toGtype error: " +
        s"got ${a.toString}, when type was expected")
  }

  def toAnyMessage(gValue: gtype.Type): AnyMsg = {
    gValue match
      case gtype.Null => AnyMsg(
        clientMsgs.NULL.getClass.getName,
        Some(com.google.protobuf.any.Any.pack(clientMsgs.NULL()))
      )
      case gtype.bool(value) => AnyMsg(
        clientMsgs.Bool.getClass.getName,
        Some(com.google.protobuf.any.Any.pack(clientMsgs.Bool(value)))
      )
      case gtype.int(value) => AnyMsg(
        clientMsgs.Int.getClass.getName,
        Some(com.google.protobuf.any.Any.pack(clientMsgs.Int(value)))
      )
      case gtype.double(value) => AnyMsg(
        clientMsgs.Double.getClass.getName,
        Some(com.google.protobuf.any.Any.pack(clientMsgs.Double(value)))
      )
      case gtype.string(value, quote) =>
        AnyMsg(
          clientMsgs.String.getClass.getName,
          Some(com.google.protobuf.any.Any.pack(clientMsgs.String(value, quote)))
        )
      case gtype.array(arr) =>
        AnyMsg(
          clientMsgs.Array.getClass.getName,
          Some(com.google.protobuf.any.Any.pack(clientMsgs.Array(arr.map(
            gType => com.google.protobuf.any.Any.pack(toAnyMessage(gType))
          ).toSeq)))
        )
  }

}
