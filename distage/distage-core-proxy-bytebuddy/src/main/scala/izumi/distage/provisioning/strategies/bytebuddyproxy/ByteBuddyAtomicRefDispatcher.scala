package izumi.distage.provisioning.strategies.bytebuddyproxy

import izumi.distage.model.provisioning.proxies.DistageProxy
import izumi.distage.model.provisioning.proxies.ProxyDispatcher.AtomicProxyDispatcher

import java.lang.reflect.{InvocationHandler, Method}

// dynamic dispatching is not optimal, uhu
private[distage] class ByteBuddyAtomicRefDispatcher(
  nullProxy: AnyRef
) extends AtomicProxyDispatcher
  with InvocationHandler {
  override def invoke(proxy: scala.Any, method: Method, objects: Array[AnyRef]): AnyRef = {
    val methodName = method.getName
    if (methodName == "equals" && (method.getParameterTypes sameElements Array(classOf[AnyRef]))) {
      objects.headOption match {
        case Some(r: DistageProxy) =>
          Boolean.box(getRef == r._distageProxyReference)

        case _ =>
          method.invoke(getRef, objects: _*)
      }
    } else if (methodName == "_distageProxyReference" && method.getParameterCount == 0) {
      getRef
    } else {
      method.invoke(getRef, objects: _*)
    }
  }

  @inline private[this] final def getRef: AnyRef = {
    val value = reference.get()
    if (value ne null) value else nullProxy
  }
}
