package org.kimwanyi.sacco.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {"*.xhtml", "/"})
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Set strict HTTP Cache-Control headers to prevent browser back-button caching
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private"); // HTTP 1.1
        httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
        httpResponse.setDateHeader("Expires", 0); // Proxies

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = requestURI.substring(contextPath.length());

        // Allow static resources and JSF library resources
        boolean isResourceRequest = path.startsWith("/jakarta.faces.resource") ||
                                    path.startsWith("/resources/") ||
                                    path.endsWith(".css") ||
                                    path.endsWith(".js") ||
                                    path.endsWith(".png") ||
                                    path.endsWith(".jpg") ||
                                    path.endsWith(".jpeg") ||
                                    path.endsWith(".gif") ||
                                    path.endsWith(".ico") ||
                                    path.endsWith(".svg");

        boolean isLoginPage = path.endsWith("login.xhtml");
        boolean isRegisterPage = path.endsWith("register.xhtml");
        boolean isVerifyEmailPage = path.endsWith("verify-email.xhtml");
        boolean isWelcomePage = path.endsWith("index.xhtml") || path.equals("/") || path.isEmpty();

        HttpSession session = httpRequest.getSession(false);
        boolean isLoggedIn = (session != null && Boolean.TRUE.equals(session.getAttribute("userLoggedIn")));

        if (isResourceRequest) {
            chain.doFilter(request, response);
            return;
        }

        if (isLoggedIn) {
            if (isLoginPage || isRegisterPage) {
                // If user is already authenticated, redirect away from login/register to member dashboard
                httpResponse.sendRedirect(contextPath + "/dashboard.xhtml");
                return;
            }
            chain.doFilter(request, response);
        } else {
            if (isWelcomePage || isLoginPage || isRegisterPage || isVerifyEmailPage) {
                // Allow unauthenticated access to public welcome page, login, register, and verify-email pages
                chain.doFilter(request, response);
            } else {
                // Redirect unauthenticated request to login page
                httpResponse.sendRedirect(contextPath + "/login.xhtml");
            }
        }
    }

    @Override
    public void destroy() {
    }
}
