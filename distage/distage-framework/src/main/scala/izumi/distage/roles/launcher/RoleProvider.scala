package izumi.distage.roles.launcher

import distage.{Id, *}
import izumi.distage.model.definition.Binding
import izumi.distage.model.definition.Binding.ImplBinding
import izumi.distage.model.reflection.SafeType
import izumi.distage.roles.DebugProperties
import izumi.distage.roles.model.definition.RoleTag
import izumi.distage.roles.model.exceptions.DIAppBootstrapException
import izumi.distage.roles.model.meta.{RoleBinding, RolesInfo}
import izumi.distage.roles.model.{AbstractRole, RoleDescriptor}
import izumi.fundamentals.platform.{IzPlatform, ScalaPlatform}
import izumi.fundamentals.platform.cli.model.raw.RawAppArgs
import izumi.fundamentals.platform.strings.IzString.toRichIterable
import izumi.fundamentals.reflection.TypeUtil
import izumi.logstage.api.IzLogger

import scala.annotation.unused

trait RoleProvider {
  def loadRoles[F[_]: TagK](appModule: ModuleBase): RolesInfo
}

object RoleProvider {
  private[this] final val sysPropIgnoreMismatchedEffect = DebugProperties.`izumi.distage.roles.ignore-mismatched-effect`.boolValue(false)
  private[this] final val syspropRolesReflection = DebugProperties.`izumi.distage.roles.reflection`.boolValue(true)

  open class NonReflectiveImpl(
    logger: IzLogger @Id("early"),
    ignoreMismatchedEffect: Boolean @Id("distage.roles.ignore-mismatched-effect"),
    parameters: RawAppArgs,
  ) extends RoleProvider {

    protected[this] val isIgnoredMismatchedEffect: Boolean = sysPropIgnoreMismatchedEffect || ignoreMismatchedEffect

    def loadRoles[F[_]: TagK](appModule: ModuleBase): RolesInfo = {
      val rolesInfo = getInfo(
        bindings = appModule.bindings,
        requiredRoles = parameters.roles.iterator.map(_.role).toSet,
        roleType = SafeType.get[AbstractRole[F]],
      )
      logger.info(s"Available ${rolesInfo.render() -> "roles"}")
      rolesInfo
    }

    protected def getInfo(bindings: Set[Binding], requiredRoles: Set[String], roleType: SafeType): RolesInfo = {
      val availableRoleBindings = findRoleBindings(bindings, roleType)
      val requiredRoleBindings = availableRoleBindings.filter(isRoleEnabled(requiredRoles))

      val roleNames = availableRoleBindings.map(_.descriptor.id)
      val requiredRoleNames = requiredRoleBindings.iterator.map(_.descriptor.id).toSet
      val unrequiredRoleNames = roleNames.diff(requiredRoleNames)

      val rolesInfo = RolesInfo(
        requiredComponents = requiredRoleBindings.iterator.map(_.binding.key).toSet,
        requiredRoleBindings = requiredRoleBindings,
        requiredRoleNames = requiredRoleNames,
        availableRoleNames = roleNames,
        availableRoleBindings = availableRoleBindings,
        unrequiredRoleNames = unrequiredRoleNames,
      )

      val missing = requiredRoles.diff(availableRoleBindings.map(_.descriptor.id))
      if (missing.nonEmpty) {
        logger.crit(s"Missing ${missing.niceList() -> "roles"}")
        throw new DIAppBootstrapException(s"Unknown roles:${missing.niceList("    ")}")
      }
      if (requiredRoleBindings.isEmpty) {
        throw new DIAppBootstrapException(s"""No roles selected to launch, please select one of the following roles using syntax `:${'$'}roleName` on the command-line.
                                             |
                                             |Available roles:${rolesInfo.render()}""".stripMargin)
      }

      rolesInfo
    }

    protected def findRoleBindings(bindings: Set[Binding], roleType: SafeType): Set[RoleBinding] = {
      bindings.collect {
        case s: ImplBinding if s.tags.exists(_.isInstanceOf[RoleTag]) && checkRoleType(s.implementation.implType, roleType, log = !isIgnoredMismatchedEffect) =>
          mkRoleBinding(s, s.tags.collectFirst { case RoleTag(roleDescriptor) => roleDescriptor }.get)

        case s: ImplBinding if s.implementation.implType <:< roleType =>
          handleMissingStaticMetadata(roleType, s)
      }
    }

    protected def handleMissingStaticMetadata(@unused roleType: SafeType, s: ImplBinding): RoleBinding = {
      logger.crit(s"${s.key -> "role"} defined ${s.origin -> "at"} has no RoleDescriptor, companion reflection is disabled")
      throw new DIAppBootstrapException(s"role=${s.key} defined at=${s.origin} has no RoleDescriptor, companion reflection is disabled")
    }

    protected def isRoleEnabled(requiredRoles: Set[String])(b: RoleBinding): Boolean = {
      requiredRoles.contains(b.descriptor.id) || requiredRoles.contains(b.tpe.tag.shortName.toLowerCase)
    }

    protected def checkRoleType(implType: SafeType, roleType: SafeType, log: Boolean): Boolean = {
      val res = implType <:< roleType
      if (!res && log) logger.warn(s"Found role binding with incompatible effect type $implType (expected to be a subtype of $roleType)")
      res
    }

    protected def mkRoleBinding(roleBinding: ImplBinding, roleDescriptor: RoleDescriptor): RoleBinding = {
      val runtimeClass = roleBinding.key.tpe.closestClass
      val implType = roleBinding.implementation.implType
      RoleBinding(roleBinding, runtimeClass, implType, roleDescriptor)
    }
  }

  open class ReflectiveImpl(
    logger: IzLogger @Id("early"),
    ignoreMismatchedEffect: Boolean @Id("distage.roles.ignore-mismatched-effect"),
    reflectionEnabled: Boolean @Id("distage.roles.reflection"),
    parameters: RawAppArgs,
  ) extends NonReflectiveImpl(logger, ignoreMismatchedEffect, parameters) {

    protected[this] val isReflectionEnabled: Boolean = reflectionEnabled && syspropRolesReflection && IzPlatform.platform != ScalaPlatform.GraalVMNativeImage

    override protected def handleMissingStaticMetadata(roleType: SafeType, s: ImplBinding): RoleBinding = {
      if (isReflectionEnabled) {
        reflectCompanionBinding(s)
      } else {
        super.handleMissingStaticMetadata(roleType, s)
      }
    }

    protected def reflectCompanionBinding(roleBinding: ImplBinding): RoleBinding = {
      if (isReflectionEnabled) {
        reflectCompanionDescriptor(roleBinding.key.tpe) match {
          case Some(descriptor) =>
            logger.warn(
              s"""${roleBinding.key -> "role"} defined ${roleBinding.origin -> "at"}: using deprecated reflective look-up of `RoleDescriptor` companion object.
                 |Please use `RoleModuleDef` & `makeRole` to create a role binding explicitly, instead.
                 |
                 |Reflective lookup of `RoleDescriptor` will be removed in a future version.""".stripMargin
            )
            mkRoleBinding(roleBinding, descriptor)

          case None =>
            logger.crit(s"${roleBinding.key -> "role"} defined ${roleBinding.origin -> "at"} has no companion object inherited from RoleDescriptor")
            throw new DIAppBootstrapException(s"role=${roleBinding.key} defined at=${roleBinding.origin} has no companion object inherited from RoleDescriptor")
        }
      } else {
        logger.crit(s"${roleBinding.key -> "role"} defined ${roleBinding.origin -> "at"} has no RoleDescriptor, companion reflection is disabled")
        throw new DIAppBootstrapException(s"role=${roleBinding.key} defined at=${roleBinding.origin} has no RoleDescriptor, companion reflection is disabled")
      }
    }

    protected def reflectCompanionDescriptor(role: SafeType): Option[RoleDescriptor] = {
      val roleClassName = role.closestClass.getName
      try {
        Some(TypeUtil.instantiateObject[RoleDescriptor](Class.forName(s"$roleClassName$$")))
      } catch {
        case t: Throwable =>
          logger.crit(s"""Failed to reflect RoleDescriptor companion object for $role: $t
                         |Please create a companion object extending `izumi.distage.roles.model.RoleDescriptor` for `$roleClassName`
                         |""".stripMargin)
          None
      }
    }
  }

}
