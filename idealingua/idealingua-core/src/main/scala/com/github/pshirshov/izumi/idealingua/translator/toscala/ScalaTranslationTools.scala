package com.github.pshirshov.izumi.idealingua.translator.toscala

import com.github.pshirshov.izumi.idealingua.model.common.TypeId.DTOId
import com.github.pshirshov.izumi.idealingua.model.common.{SigParam, StructureId, TypeId}
import com.github.pshirshov.izumi.idealingua.model.typespace.structures.ConverterDef
import com.github.pshirshov.izumi.idealingua.translator.toscala.types.CompositeStructure

import scala.meta._


class ScalaTranslationTools(ctx: STContext) {

  import ctx.conv._
  import com.github.pshirshov.izumi.idealingua.translator.toscala.types.ScalaField._

  def mkStructure(id: StructureId): CompositeStructure = {
    val fields = ctx.typespace.structure.structure(id).toScala
    new CompositeStructure(ctx, fields)
  }


  def idToParaName(id: TypeId): Term.Name = Term.Name(ctx.typespace.tools.idToParaName(id))

  def makeParams(t: ConverterDef): List[Term.Param] = {
    t.outerParams
      .map {
        f =>
          /*
          ANYVAL:ERASURE
           this is a workaround for anyval/scala erasure issue.
           We prohibit to use DTOs directly in parameters and using mirrors instead
            */
          val source = f.sourceType match {
            case s: DTOId =>
              ctx.typespace.defnId(s)

            case o =>
              o
          }

          (Term.Name(f.sourceName), ctx.conv.toScala(source).typeFull)
      }
      .toParams
  }

  def makeConstructor(t: ConverterDef): List[Term.Assign] = {
    t.allFields.map(toAssignment)
  }

  private def toAssignment(f: SigParam): Term.Assign = {
    f.sourceFieldName match {
      case Some(sourceFieldName) =>
        q""" ${Term.Name(f.targetFieldName)} = ${Term.Name(f.source.sourceName)}.${Term.Name(sourceFieldName)}  """


      case None =>
        val sourceType = f.source.sourceType
        val defnid = sourceType match {
          case d: DTOId =>
            ctx.typespace.tools.defnId(d)
          case o =>
            o
        }
        if (defnid == sourceType) {

          q""" ${Term.Name(f.targetFieldName)} = ${Term.Name(f.source.sourceName)}  """
        } else {
          q""" ${Term.Name(f.targetFieldName)} = ${ctx.conv.toScala(sourceType).termFull}(${Term.Name(f.source.sourceName)})"""
        }
    }
  }


}
