Hello World from 196 HttpServletDefault AuthModule Test!
<hr>
<%
    try {
        out.println("Hello, " + request.getUserPrincipal() +
            " from " + request.getAttribute("MY_NAME"));
    } catch(Exception ex) {
        out.println("Something wrong: " + ex);
        ex.printStackTrace();
    }
%>
<hr>
