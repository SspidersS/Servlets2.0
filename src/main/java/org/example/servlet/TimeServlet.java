package org.example.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

public class TimeServlet extends HttpServlet {
    private TemplateEngine templateEngine;
    private static final String COOKIE_NAME = "lastTimezone";
    private static final String DEFAULT_TIMEZONE = "UTC";

    @Override
    public void init() throws ServletException {
        ServletContextTemplateResolver resolver = new ServletContextTemplateResolver(getServletContext());
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        String timezoneParam = request.getParameter("timezone");
        String timezone = getValidTimezone(timezoneParam, request, response);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
        String formattedTime = formatter.format(now) + " " + timezone;

        WebContext ctx = new WebContext(request, response, getServletContext());
        ctx.setVariable("timezone", timezone);
        ctx.setVariable("currentTime", formattedTime);

        response.setContentType("text/html; charset=UTF-8");
        templateEngine.process("time", ctx, response.getWriter());

        System.out.println(Arrays.toString(response.getHeaders("Set-Cookie").toArray()));
    }

    private String getValidTimezone(String timezoneParam, HttpServletRequest request, HttpServletResponse response) {
        if (timezoneParam != null && !timezoneParam.isEmpty()) {
            try {
                ZoneId.of(timezoneParam);
                saveCookie(response, timezoneParam, request);
                return timezoneParam;
            } catch (Exception e) {
                return DEFAULT_TIMEZONE;
            }
        }

        return getCookie(request).orElse(DEFAULT_TIMEZONE);
    }

    private void saveCookie(HttpServletResponse response, String timezone,HttpServletRequest request ) {
        Cookie cookie = new Cookie(COOKIE_NAME, timezone);
        cookie.setMaxAge(60 * 60 * 24 * 30);
        cookie.setPath(request.getContextPath());
        response.addCookie(cookie);
    }

    private Optional<String> getCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
        }
        return Optional.empty();
    }
}

