package izumi.distage.constructors

import izumi.fundamentals.platform.reflection.ReflectionUtil
import scala.quoted.{Expr, Quotes, Type}
import scala.collection.mutable

class ConstructorUtil[Q <: Quotes](using val qctx: Q) {
  import qctx.reflect.*

  def requireConcreteTypeConstructor[R: Type](macroName: String): Unit = {
    val tpe = TypeRepr.of[R]
    if (!ReflectionUtil.allPartsStrong(tpe.typeSymbol.typeRef)) {
      val hint = tpe.dealias.show
      report.errorAndAbort(
        s"""$macroName: Can't generate constructor for ${tpe.show}:
           |Type constructor is an unresolved type parameter `$hint`.
           |Did you forget to put a $macroName context bound on the $hint, such as [$hint: $macroName]?
           |""".stripMargin
      )
    }
  }

  def wrapApplicationIntoLambda[R: Type](paramss: List[List[(String, qctx.reflect.TypeTree)]], constructorTerm: qctx.reflect.Term): Expr[Any] = {
    wrapIntoLambda[R](paramss) {
      (_, args0) =>
        import scala.collection.immutable.Queue
        val (_, argsLists) = paramss.foldLeft((args0, Queue.empty[List[Term]])) {
          case ((args, res), params) =>
            val (argList, rest) = args.splitAt(params.size)
            (rest, res :+ (argList: List[Tree]).asInstanceOf[List[Term]])
        }

        val appl = argsLists.foldLeft(constructorTerm)(_.appliedToArgs(_))
        val trm = Typed(appl, TypeTree.of[R])
        trm
    }
  }

  def wrapIntoLambda[R: Type](
    paramss: List[List[(String, qctx.reflect.TypeTree)]]
  )(body: (qctx.reflect.Symbol, List[qctx.reflect.Term]) => qctx.reflect.Tree
  ): Expr[Any] = {
    val params = paramss.flatten
    val mtpe = MethodType(params.map(_._1))(_ => params.map(_._2.tpe), _ => TypeRepr.of[R])
    val lam = Lambda(
      Symbol.spliceOwner,
      mtpe,
      {
        case (lamSym, args0) =>
          body(lamSym, args0.asInstanceOf[List[Term]])
      },
    )
    lam.asExpr
  }

  def findRequiredImplParents(resultTpeSym: Symbol): List[Symbol] = {
    if (!resultTpeSym.flags.is(Flags.Trait)) {
      List(resultTpeSym)
    } else {
      val banned = mutable.HashSet[Symbol](defn.ObjectClass, defn.MatchableClass, defn.AnyRefClass, defn.AnyValClass, defn.AnyClass)
      val seen = mutable.HashSet.empty[Symbol]
      seen.addAll(banned)

      def go(sym: Symbol): List[Symbol] = {
        val onlyBases = sym.typeRef.baseClasses
          .drop(1) // without own type
          .filterNot(seen)

        if (!sym.flags.is(Flags.Trait)) {
          // (abstract) class calls the constructors of its bases, so don't call constructors for any of its bases
          def banAll(s: Symbol): Unit = {
            val onlyBasesNotBanned = s.typeRef.baseClasses.drop(1).filterNot(banned)
            seen ++= onlyBasesNotBanned
            banned ++= onlyBasesNotBanned
            onlyBasesNotBanned.foreach(banAll)
          }

          banAll(sym)
          List(sym)
        } else {
          seen ++= onlyBases
          val needConstructorCall = onlyBases.filter(
            s =>
              !s.flags.is(Flags.Trait) || (
                s.primaryConstructor.paramSymss.nonEmpty
                && s.primaryConstructor.paramSymss.exists(_.headOption.exists(!_.isTypeParam))
              )
          )
          needConstructorCall ++ onlyBases.flatMap(go)
        }
      }

      val (classCtors0, traitCtors0) = go(resultTpeSym).filterNot(banned).distinct.partition(!_.flags.is(Flags.Trait))
      val classCtors = if (classCtors0.isEmpty) List(defn.ObjectClass) else classCtors0
      val traitCtors =
        // try to instantiate traits in order from deeper to shallower
        // (allow traits defined later in the hierarchy to override their base traits)
        (resultTpeSym :: traitCtors0).reverse
      classCtors ++ traitCtors
    }
  }

  type ParamLists = List[List[(String, qctx.reflect.TypeRepr)]]

  def buildConstructorParameters(resultTpe: TypeRepr)(sym: Symbol): (qctx.reflect.Symbol, ParamLists) = {
    val argTypes = resultTpe.baseType(sym) match {
      case AppliedType(_, args) =>
        args
      case _ =>
        Nil
    }

    val methodTypeApplied = sym.typeRef.memberType(sym.primaryConstructor).appliedTo(argTypes)

    val paramLists: List[List[(String, TypeRepr)]] = {
      def go(t: TypeRepr): List[List[(String, TypeRepr)]] = {
        t match {
          case MethodType(paramNames, paramTpes, res) =>
            paramNames.zip(paramTpes) :: go(res)
          case _ =>
            Nil
        }
      }

      go(methodTypeApplied)
    }

    sym -> paramLists
  }

  def buildParentConstructorCallTerms(
    resultTpe: TypeRepr,
    constructorParamLists: List[(qctx.reflect.Symbol, ParamLists)],
    contextParameters: List[Term],
  ): Seq[Term] = {
    import scala.collection.immutable.Queue
    val (_, parents) = constructorParamLists.foldLeft((contextParameters, Queue.empty[Term])) {
      case ((remainingLamArgs, doneCtors), (sym, ctorParamLists)) =>
        // TODO decopypaste

        val consSym = sym.primaryConstructor
        val ctorTree = Select(New(TypeIdent(sym)), consSym)

        val argTypes = resultTpe.baseType(sym) match {
          case AppliedType(_, args) =>
            args.map(repr => TypeTree.of(using repr.asType))
          case _ =>
            Nil
        }
        val ctorTreeParameterized = ctorTree.appliedToTypeTrees(argTypes)

        val (rem, argsLists) = ctorParamLists.foldLeft((remainingLamArgs, Queue.empty[List[Term]])) {
          case ((lamArgs, res), params) =>
            val (argList, rest) = lamArgs.splitAt(params.size)
            (rest, res :+ argList)
        }

        val appl = argsLists.foldLeft(ctorTreeParameterized)(_.appliedToArgs(_))
        (rem, doneCtors :+ appl)
    }

    parents
  }
}
