package org.patdouble.adventuregame.ui.rest

import groovy.transform.CompileStatic
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Provides default headers for all HTTP requests.
 */
@CompileStatic
class DefaultHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        response.addHeader('X-Frame-Options', 'DENY')
        response.addHeader('X-Content-Type-Options', 'nosniff')
        response.addHeader('X-XSS-Protection', '1; mode=block')
        filterChain.doFilter(request, response)
    }
}
