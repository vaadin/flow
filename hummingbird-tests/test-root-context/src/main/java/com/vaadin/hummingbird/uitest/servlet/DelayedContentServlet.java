package com.vaadin.hummingbird.uitest.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = DelayedContentServlet.DELAYED_RESOURCE_URL, name = "DynamicContentServlet")
public class DelayedContentServlet extends HttpServlet {

    public static final String DELAYED_RESOURCE_URL = "/delayed";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        resp.setContentType("text/javascript;charset=UTF-8;");
        String content = "window.document.body.appendChild(window.document.createElement('meter'));";
        resp.getWriter().write(content);
    }
}