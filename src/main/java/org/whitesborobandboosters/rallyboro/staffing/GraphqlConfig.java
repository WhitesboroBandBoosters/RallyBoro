package org.whitesborobandboosters.rallyboro.staffing;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.DataFetchingEnvironment;
import java.time.DateTimeException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Configuration
public class GraphqlConfig {
  @Bean
  Instrumentation queryLimits() {
    return new ChainedInstrumentation(
        List.of(new MaxQueryDepthInstrumentation(12), new MaxQueryComplexityInstrumentation(1000)));
  }

  @Bean
  WebGraphQlInterceptor requestLimits() {
    return (request, chain) -> {
      if (request.getDocument().length() > 32000)
        return Mono.error(new IllegalArgumentException("Operation exceeds size limit"));
      String address =
          request.getRemoteAddress() == null
              ? "unknown"
              : request.getRemoteAddress().getAddress().getHostAddress();
      request.configureExecutionInput((input, builder) ->
          builder.graphQLContext(context -> context.put("clientAddress", address)).build());
      return chain.next(request).timeout(Duration.ofSeconds(10));
    };
  }

  @Component
  static class Errors extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(
        Throwable exception, DataFetchingEnvironment environment) {
      String code = "INTERNAL_ERROR", message = "The request could not be completed.";
      ErrorType type = ErrorType.INTERNAL_ERROR;
      if (exception instanceof DomainException error) {
        code = error.code();
        message = error.getMessage();
        type = code.equals("FORBIDDEN") ? ErrorType.FORBIDDEN : ErrorType.BAD_REQUEST;
      } else if (exception instanceof IllegalArgumentException
          || exception instanceof DateTimeException) {
        code = "INVALID_INPUT";
        message = "Please check the supplied values.";
        type = ErrorType.BAD_REQUEST;
      } else if (exception instanceof DataAccessException) {
        code = "CONFLICT";
        message = "The data changed or is unavailable. Reload and try again.";
        type = ErrorType.BAD_REQUEST;
      }
      return GraphqlErrorBuilder.newError(environment)
          .errorType(type)
          .message(message)
          .extensions(Map.of("code", code))
          .build();
    }
  }
}
