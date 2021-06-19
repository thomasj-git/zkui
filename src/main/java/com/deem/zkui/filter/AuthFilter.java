/**
 *
 * Copyright (c) 2014, Deem Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package com.deem.zkui.filter;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(filterName = "filteranno", urlPatterns = "/*")
public class AuthFilter implements Filter {

    private final static Logger log = LoggerFactory.getLogger(AuthFilter.class);
//    private SessionCheck worker = new SessionCheck();

    @Override
    public void init(FilterConfig fc) throws ServletException {
        // Do Nothing
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        if (!request.getRequestURI().contains("/login") && !request.getRequestURI().contains("/acd/appconfig") && !request.getRequestURI().contains("/verifyCode")) {
            RequestDispatcher dispatcher;
            HttpSession session = request.getSession();
            if (session != null) {
                if (session.getAttribute("authName") == null || session.getAttribute("authRole") == null) {
                    response.sendRedirect("/cms-ability/zkui/login");
                    return;
                }

            } else {
                request.setAttribute("fail_msg", "Session timed out!");
                dispatcher = request.getRequestDispatcher("/cms-ability/zkui/Login");
                dispatcher.forward(request, response);
                return;
            }
        }
//        try {
//            worker.check(request, response);
//        } catch (InterruptedException e) {
//            response.sendRedirect("/login");
//        }
        fc.doFilter(req, res);
    }

    @Override
    public void destroy() {
        // Do nothing
    }

}
