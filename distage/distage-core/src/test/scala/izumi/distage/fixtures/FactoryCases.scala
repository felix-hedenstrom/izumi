package izumi.distage.fixtures

import izumi.distage.model.definition.{Id, With}
import izumi.fundamentals.platform.build.ExposedTestScope

@ExposedTestScope
object FactoryCases {

  object FactoryCase1 {

    trait Dependency {
      def isSpecial: Boolean = false
      def isVerySpecial: Boolean = false

      override def toString: String = s"Dependency($isSpecial, $isVerySpecial)"
    }

    final case class ConcreteDep() extends Dependency

    final case class SpecialDep() extends Dependency {
      override def isSpecial: Boolean = true
    }

    final case class VerySpecialDep() extends Dependency {
      override def isVerySpecial: Boolean = true
    }

    final case class TestClass(b: Dependency)
    final case class AssistedTestClass(b: Dependency, a: Int)
    final case class NamedAssistedTestClass(@Id("special") b: Dependency, a: Int)
    final case class GenericAssistedTestClass[T, S](a: List[T], b: List[S], c: Dependency)

    trait Factory {
      def wiringTargetForDependency: Dependency
      def factoryMethodForDependency(): Dependency
      def x(): TestClass
    }

    trait MixedAssistendNonAssisted {
      def assisted(): TestClass
      def nonAssisted(dependency: Dependency): TestClass
    }

    trait OverridingFactory {
      def x(b: Dependency): TestClass
    }

    trait AssistedFactory {
      def x(a: Int): AssistedTestClass
    }

    trait NamedAssistedFactory {
      def dep: Dependency @Id("veryspecial")
      def x(a: Int): NamedAssistedTestClass
    }

    trait GenericAssistedFactory {
      def x[T, S](t: List[T], s: List[S]): GenericAssistedTestClass[T, S]
    }

    trait AbstractDependency

    case class AbstractDependencyImpl(a: Dependency) extends AbstractDependency

    trait FullyAbstractDependency {
      def a: Dependency
    }

    trait AbstractFactory {
      @With[AbstractDependencyImpl]
      def x(): AbstractDependency
      def y(): FullyAbstractDependency
    }

    trait FactoryProducingFactory {
      def x(): Factory
    }

    abstract class AbstractClassFactory(private val t: TestClass) {
      def x(a: Int): AssistedTestClass
    }

  }

  object FactoryCase2 {
    trait AbstractAbstractFactory {
      def x(z: Int, y: Int, x: Int): Product
    }

    trait AssistedAbstractFactory extends AbstractAbstractFactory {
      override def x(z: Int, y: Int, x: Int): Product @With[ProductImpl]
    }

    class Dependency()

    final case class ProductImpl(x: Int, y: Int, z: Int, dependency: Dependency)

    trait AssistedAbstractFactoryF[F[_]] extends AbstractAbstractFactory {
      override def x(z: Int, y: Int, x: Int): ProductF[F] @With[ProductFImpl[F]]
    }

    trait ProductF[F[_]] extends Product
    final case class ProductFImpl[F[_]](x: Int, y: Int, z: Int, dependency: F[Dependency]) extends ProductF[F]
  }

  object FactoryCase3 {
    trait TC[-T]

    case object TC1 extends TC[Any]
    implicit case object TC2 extends TC[Any]

    class Dep1
    class Dep2
    implicit object Dep3
    class UnrelatedTC[T]

    case class TestClass[T](dep1: Dep1, dep2: Dep2)(implicit val TC: TC[T], val dep3: Dep3.type)

    trait ImplicitFactory {
      def x[T](dep1: Dep1)(implicit tc: TC[T], dep3: Dep3.type): TestClass[T]
    }

    trait InvalidImplicitFactory {
      def x[T: TC: UnrelatedTC](dep1: Dep1): TestClass[T]
    }
  }

  object FactoryCase4 {
    case class Dep()
    trait IFactory {
      def dep(): Dep
    }

    trait IFactory1 {
      def dep1(): Dep
    }

    trait IFactoryImpl extends IFactory with IFactory1
  }

  object FactoryCase5 {
    case class Dep()

    trait IFactory {
      def dep(): Dep
    }

    trait IFactory1 extends IFactory {
      def dep1(): Dep
    }

    trait IFactoryImpl extends IFactory1 {}
  }

  object FactoryCase6 {
    trait IDep
    case class Dep() extends IDep

    trait IFactory {
      def dep(): IDep
    }

    trait IFactoryImpl extends IFactory {
      def dep(): Dep
    }
  }

  object FactoryCase7 {
    trait IDep
    case class Dep() extends IDep

    trait IFactory1 {
      def dep(): IDep
    }

    trait IFactory2 {
      def dep(): Dep
    }
  }

  object FactoryCase8 {
    class XProduct[F[+_, +_]]

    trait XFactory[F[+_, +_]] {
      def create(xparam: XParam[F]): XProduct[F] @With[XImpl[F]]
    }

    class XParam[F[+_, +_]]

    final case class XImpl[F[+_, +_]](xContext: XContext[F], xparam: XParam[F]) extends XProduct[F]

    class XContext[F[+_, +_]]
  }

}
