package test;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DispatchTarget extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {

        Enumeration<String> attrNames = req.getAttributeNames();
        if (attrNames == null) {
            throw new ServletException("Missing ASYNC dispatch related " +
                                       "request attributes");
        }

        if (!"MYVALUE".equals(req.getAttribute("MYNAME"))) {
            throw new ServletException("Missing custom request attribute");
        }

        int asyncRequestAttributeFound = 0;
        while (attrNames.hasMoreElements()){
            String attrName = attrNames.nextElement();
            if (AsyncContext.ASYNC_REQUEST_URI.equals(attrName)) {
                if (!"/web-async-context-dispatch/TestServlet".equals(
                        req.getAttribute(attrName))) {
                    throw new ServletException("Wrong value for " +
                        AsyncContext.ASYNC_REQUEST_URI +
                        " request attribute");
                }
                asyncRequestAttributeFound++;
            } else if (AsyncContext.ASYNC_CONTEXT_PATH.equals(attrName)) {
                if (!getServletContext().getContextPath().equals(
                        req.getAttribute(attrName))) {
                    throw new ServletException("Wrong value for " +
                        AsyncContext.ASYNC_CONTEXT_PATH +
                        " request attribute");
                }
                asyncRequestAttributeFound++;
            } else if (AsyncContext.ASYNC_PATH_INFO.equals(attrName)) {
                if (req.getAttribute(attrName) != null) {
                    throw new ServletException("Wrong value for " +
                        AsyncContext.ASYNC_PATH_INFO +
                        " request attribute");
                }
                asyncRequestAttributeFound++;
            } else if (AsyncContext.ASYNC_SERVLET_PATH.equals(attrName)) {
                if (!"/TestServlet".equals(
                        req.getAttribute(attrName))) {
                    throw new ServletException("Wrong value for " +
                        AsyncContext.ASYNC_SERVLET_PATH +
                        " request attribute");
                }
                asyncRequestAttributeFound++;
            } else if (AsyncContext.ASYNC_QUERY_STRING.equals(attrName)) {
                if (!"target=DispatchTargetWithPath".equals(
                        req.getAttribute(attrName))) {
                    throw new ServletException("Wrong value for " +
                        AsyncContext.ASYNC_QUERY_STRING +
                        " request attribute");
                }
                asyncRequestAttributeFound++;
            }
        }

        if (asyncRequestAttributeFound != 5) {
            throw new ServletException("Wrong number of ASYNC dispatch " +
                                       "related request attributes");
        }

        res.getWriter().println("Hello world");
    }
}
