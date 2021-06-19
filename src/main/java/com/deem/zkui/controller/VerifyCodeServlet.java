package com.deem.zkui.controller;

import com.ailk.product.cms.webauth.code.util.VerifyCodeHelper;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
@WebServlet(urlPatterns = {"/cms-ability/zkui/verifyCode"})
@Slf4j
public class VerifyCodeServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		VerifyCodeHelper.responseCode(req, resp);
	}

}
