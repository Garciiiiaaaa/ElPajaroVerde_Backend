package es.elpajaroverde.config;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

public final class PublicEndpoints {
    private PublicEndpoints() {}

    public static final RequestMatcher SESION = RequestMatchers.anyOf(
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/sesion"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.DELETE, "/api/v1/sesion")
    );

    public static final RequestMatcher DISPONIBILIDAD = RequestMatchers.anyOf(
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/disponibilidad")
    );

    public static final RequestMatcher MENSAJES = RequestMatchers.anyOf(
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/mensajes")
    );

    public static final RequestMatcher SIN_TOKEN = RequestMatchers.anyOf(SESION, DISPONIBILIDAD, MENSAJES);
}