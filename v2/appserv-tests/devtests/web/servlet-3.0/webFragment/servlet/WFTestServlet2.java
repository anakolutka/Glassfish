package wftest;

import java.io.*;
import java.util.Enumeration;
import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;

@WebServlet(name="wftestServlet2", urlPatterns={"/wftest2"})
public class WFTestServlet2 extends HttpServlet {
    @Resource(name="wfmin") private Integer min;
    @Resource(name="wfmid") private Integer mid;
    @Resource(name="wfmax") private Integer max;

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        String message = "min=" + min + ", mid=" + mid + ", max=" + max;;
        res.getWriter().println(message);
    }
}
