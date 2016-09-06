package pl.touk.esp.engine.definition

import com.typesafe.config.Config
import pl.touk.esp.engine.api.exception.ExceptionHandlerFactory
import pl.touk.esp.engine.api.{FoldingFunction, Service}
import pl.touk.esp.engine.api.process.{ProcessConfigCreator, SinkFactory, SourceFactory}
import pl.touk.esp.engine.definition.DefinitionExtractor.{ObjectDefinition, Parameter}

object ProcessDefinitionExtractor {

  def extract(objects: ProcessObjects) =
    ProcessDefinition(
      services = objects.services.mapValues(ServiceDefinitionExtractor.extract),
      sourceFactories = objects.sourceFactories.mapValues(ProcessObjectDefinitionExtractor.source.extract),
      sinkFactories = objects.sinkFactories.mapValues(ProcessObjectDefinitionExtractor.sink.extract),
      foldingFunctions = objects.foldingFunctions.keySet,
      exceptionHandlerFactory = ProcessObjectDefinitionExtractor.exceptionHandler.extract(objects.exceptionHandlerFactory)
    )

  case class ProcessObjects(services: Map[String, Service],
                            sourceFactories: Map[String, SourceFactory[_]],
                            sinkFactories: Map[String, SinkFactory],
                            foldingFunctions: Map[String, FoldingFunction[_]],
                            exceptionHandlerFactory: ExceptionHandlerFactory)

  object ProcessObjects {
    def apply(creator: ProcessConfigCreator, config: Config): ProcessObjects = {
      ProcessObjects(
        services = creator.services(config),
        sourceFactories = creator.sourceFactories(config),
        sinkFactories = creator.sinkFactories(config),
        foldingFunctions = creator.foldingFunctions(config),
        exceptionHandlerFactory = creator.exceptionHandlerFactory(config)
      )
    }
  }

  case class ProcessDefinition(services: Map[String, ObjectDefinition],
                               sourceFactories: Map[String, ObjectDefinition],
                               sinkFactories: Map[String, ObjectDefinition],
                               foldingFunctions: Set[String],
                               exceptionHandlerFactory: ObjectDefinition) {

    def withService(id: String, params: Parameter*) =
      copy(services = services + (id -> ObjectDefinition(params.toList)))

    def withSourceFactory(typ: String, params: Parameter*) =
      copy(sourceFactories = sourceFactories + (typ -> ObjectDefinition(params.toList)))

    def withSinkFactory(typ: String, params: Parameter*) =
      copy(sinkFactories = sinkFactories + (typ -> ObjectDefinition(params.toList)))

    def withFoldingFunction(name: String) =
      copy(foldingFunctions = foldingFunctions + name)

    def withExceptionHandlerFactory(params: Parameter*) =
      copy(exceptionHandlerFactory = ObjectDefinition(params.toList))

  }

  object ProcessDefinition {
    def empty: ProcessDefinition = ProcessDefinition(Map.empty, Map.empty, Map.empty, Set.empty, ObjectDefinition.noParam)
  }

}
