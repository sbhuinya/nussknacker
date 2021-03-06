package pl.touk.nussknacker.ui.process.displayedgraph

import pl.touk.nussknacker.engine.api.{MetaData, TypeSpecificData, UserDefinedProcessAdditionalFields}
import pl.touk.nussknacker.engine.graph.exceptionhandler.ExceptionHandlerRef
import pl.touk.nussknacker.engine.graph.node.NodeData
import pl.touk.nussknacker.ui.validation.ProcessValidation
import pl.touk.nussknacker.ui.db.entity.ProcessEntity.ProcessingType.ProcessingType
import pl.touk.nussknacker.ui.process.displayedgraph.displayablenode._
import pl.touk.nussknacker.ui.validation.ValidationResults.ValidationResult

//it would be better to have two classes but it would either to derivce from each other, which is not easy for case classes
//or we'd have to do composition which would break many things in client
case class DisplayableProcess(id: String,
                              properties: ProcessProperties,
                              nodes: List[NodeData],
                              edges: List[Edge],
                              processingType: ProcessingType,
                              validationResult: Option[ValidationResult] = None) {
  def validated(validation: ProcessValidation) =
    copy(validationResult = Some(validation.validate(this)))
  def withSuccessValidation() = {
    copy(validationResult = Some(ValidationResult.success))
  }

  val metaData = MetaData(id, properties.typeSpecificProperties, properties.isSubprocess, properties.additionalFields)

}

case class ProcessProperties(typeSpecificProperties: TypeSpecificData,
                             exceptionHandler: ExceptionHandlerRef,
                             isSubprocess: Boolean = false,
                             additionalFields: Option[UserDefinedProcessAdditionalFields] = None)
