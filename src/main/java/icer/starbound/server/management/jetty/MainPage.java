/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icer.starbound.server.management.jetty;

import icer.starbound.server.management.StarboundServer;
import icer.starbound.server.management.pojos.ServerState;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Icer
 */
public class MainPage extends HttpServlet {

    private String greeting = "Hello World";
    StarboundServer server;

    public MainPage(StarboundServer server) {
        this.server = server;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        StringBuilder sb = new StringBuilder();
        ServerState si = server.getStartingInformation();
        sb.append("<h1> Starbound Server Console</h1>");
        sb.append(createH3("IP", si.getIp()));
        sb.append(createH3("Status", si.getStatus().toString()));
        sb.append(createH3("location", si.getLocation()));
        sb.append(createH3("version", si.getVersion()));
        String ajax = "<script>\n" +
"function loadXMLDoc()\n" +
"{\n" +
"var xmlhttp;\n" +
"if (window.XMLHttpRequest)\n" +
"  {// code for IE7+, Firefox, Chrome, Opera, Safari\n" +
"  xmlhttp=new XMLHttpRequest();\n" +
"  }\n" +
"else\n" +
"  {// code for IE6, IE5\n" +
"  xmlhttp=new ActiveXObject(\"Microsoft.XMLHTTP\");\n" +
"  }\n" +
"xmlhttp.onreadystatechange=function()\n" +
"  {\n" +
"  if (xmlhttp.readyState==4 && xmlhttp.status==200)\n" +
"    {\n" +
"    document.getElementById(\"consoleDiv\").innerHTML=xmlhttp.responseText;\n "
                + "var objDiv = document.getElementById(\"consoleDiv\");\n" +
"objDiv.scrollTop = objDiv.scrollHeight;" +
"    }\n" +
"  }\n" +
"xmlhttp.open(\"GET\",\"./console/\",true);\n" +
"xmlhttp.send();\n" +
"}\n" +
"setInterval(function(){loadXMLDoc();},500);</script>\n" +
"<textarea id=\"consoleDiv\" style=\"width:700px;height:400px;line-height:3em;overflow:scroll;padding:5px;\"></textarea>";
sb.append(ajax);

        response.getWriter().println(sb.toString());
    }

    private String createH3(String a, String b) {
        return "<h3>" + a + " = " + b + "</h3>\n";
    }

    private String createDiv(String a, String b) {
        return "<div>" + a + " = " + b + "</div>\n";
    }
}
