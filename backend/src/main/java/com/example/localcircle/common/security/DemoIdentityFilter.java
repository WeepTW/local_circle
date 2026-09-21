package com.example.localcircle.common.security;

import com.example.localcircle.business.service.RegistryService;
import com.example.localcircle.common.error.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DemoIdentityFilter extends OncePerRequestFilter {
  private final RegistryService service;
  private final ObjectMapper mapper;
  private final Environment environment;

  public DemoIdentityFilter(RegistryService service, ObjectMapper mapper, Environment environment) {
    this.service = service;
    this.mapper = mapper;
    this.environment = environment;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Referrer-Policy", "no-referrer");
    if (!request.getRequestURI().startsWith("/api/")) {
      chain.doFilter(request, response);
      return;
    }
    try {
      String origin = request.getHeader("Origin");
      var origins =
          Set.of(environment.getRequiredProperty("local-circle.allowed-origins").split(","));
      if (origin != null) {
        if (!origins.contains(origin)) throw DomainException.forbidden();
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Vary", "Origin");
      }
      if ("OPTIONS".equals(request.getMethod())) {
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Demo-User-Id");
        response.setStatus(204);
        return;
      }
      if (!environment.acceptsProfiles(Profiles.of("local")))
        throw new DomainException(
            401, "DEMO_DISABLED", "Demo identity is only enabled in the local profile.");
      String header = request.getHeader("X-Demo-User-Id");
      if (header == null || !header.matches("[1-9][0-9]{0,17}"))
        throw new DomainException(401, "UNAUTHENTICATED", "Select a valid local demo identity.");
      request.setAttribute("actor", service.actor(Long.parseLong(header)));
    } catch (DomainException e) {
      write(response, ApiProblem.of(e.status(), e.code(), e.getMessage(), request.getRequestURI()));
      return;
    } catch (Exception e) {
      write(
          response,
          ApiProblem.of(
              503,
              "SERVICE_UNAVAILABLE",
              "Service is temporarily unavailable.",
              request.getRequestURI()));
      return;
    }
    chain.doFilter(request, response);
  }

  private void write(HttpServletResponse response, ApiProblem problem) throws IOException {
    response.setStatus(problem.status());
    response.setContentType("application/problem+json");
    mapper.writeValue(response.getOutputStream(), problem);
  }
}
