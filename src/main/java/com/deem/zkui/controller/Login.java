/**
 * Copyright (c) 2014, Deem Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.deem.zkui.controller;

import com.ailk.product.cms.security.Encrypt;
import com.ailk.product.cms.webauth.code.object.VerifyCode;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.deem.zkui.utils.ServletUtil;
import com.deem.zkui.utils.ZooKeeperUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.deem.zkui.utils.LdapAuth;

import java.util.Arrays;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/cms-ability/zkui/login"})
public class Login extends HttpServlet {

	private final static Logger logger = LoggerFactory.getLogger(Login.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		logger.debug("Login Action!");
		try {
			Properties          globalProps   = (Properties) getServletContext().getAttribute("globalProps");
			Map<String, Object> templateParam = new HashMap<>();
			templateParam.put("uptime", globalProps.getProperty("uptime"));
			templateParam.put("loginMessage", globalProps.getProperty("loginMessage"));
			ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "login.ftl.html");
		} catch (TemplateException ex) {
			logger.error(Arrays.toString(ex.getStackTrace()));
			ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
		}

	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		logger.debug("Login Post Action!");
		try {
			Properties          globalProps   = (Properties) getServletContext().getAttribute("globalProps");
			Map<String, Object> templateParam = new HashMap<>();
			HttpSession         session       = request.getSession(true);
			session.setMaxInactiveInterval(Integer.valueOf(globalProps.getProperty("sessionTimeout")));
			//TODO: Implement custom authentication logic if required.
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			logger.info("提交的加密信息, 账号: {}, 密码: {}", username, password);
			try {
				username = Encrypt.aesDecrypt(username);
				password = Encrypt.aesDecrypt(password);
				logger.info("加密信息解密后, 账号: {}, 密码: {}", username, password);
			} catch (Exception e) {
				logger.error("", e);
			}
			if (username == null || password == null) {
				session.setAttribute("flashMsg", "账号和密码不能为空");
				ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "login.ftl.html");
				return;
			}
			String submitCode = request.getParameter("code");
			String verifyCode = (String) session.getAttribute(VerifyCode.DEFAULT_SESSION_VERIFY_CODE_KEY);

			String  role          = null;
			Boolean authenticated = false;
			if (submitCode == null || !submitCode.equals(verifyCode)) {
				logger.warn("页面提交的验证码: {}, 会话中验证码: {}", submitCode, verifyCode);
				session.setAttribute("flashMsg", "验证码错误！");
				session.removeAttribute(VerifyCode.DEFAULT_SESSION_VERIFY_CODE_KEY);
				ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "login.ftl.html");
			}
			else {
				session.removeAttribute(VerifyCode.DEFAULT_SESSION_VERIFY_CODE_KEY);
//if ldap is provided then it overrides roleset.
				if (globalProps.getProperty("ldapAuth").equals("true")) {
					authenticated = new LdapAuth()
							.authenticateUser(globalProps.getProperty("ldapUrl"), username, password, globalProps
									.getProperty("ldapDomain"));
					if (authenticated) {
						JSONArray jsonRoleSet = (JSONArray) ((JSONObject) new JSONParser()
								.parse(globalProps.getProperty("ldapRoleSet"))).get("users");
						for (Iterator it = jsonRoleSet.iterator(); it.hasNext(); ) {
							JSONObject jsonUser = (JSONObject) it.next();
							if (jsonUser.get("username") != null && jsonUser.get("username").equals("*")) {
								role = (String) jsonUser.get("role");
							}
							if (jsonUser.get("username") != null && jsonUser.get("username").equals(username)) {
								role = (String) jsonUser.get("role");
							}
						}
						if (role == null) {
							role = ZooKeeperUtil.ROLE_USER;
						}

					}
				}
				else {
					JSONArray jsonRoleSet =
							(JSONArray) ((JSONObject) new JSONParser().parse(globalProps.getProperty("userSet")))
									.get("users");
					for (Iterator it = jsonRoleSet.iterator(); it.hasNext(); ) {
						JSONObject jsonUser = (JSONObject) it.next();
						if (jsonUser.get("username").equals(username) && jsonUser.get("password").equals(password)) {
							authenticated = true;
							role          = (String) jsonUser.get("role");
						}
					}
				}
				if (authenticated) {
					logger.info("Login successful: " + username);
					session.setAttribute("authName", username);
					session.setAttribute("authRole", role);
					response.sendRedirect("/cms-ability/zkui/home");
				}
				else {
					session.setAttribute("flashMsg", "账号或密码错误");
					ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "login.ftl.html");
				}
			}
		} catch (ParseException | TemplateException ex) {
			logger.error(Arrays.toString(ex.getStackTrace()));
			ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
		}
	}

	public static void main(String[] args) {
		try {
			String userName = Encrypt.aesDecrypt("aJm6W36H/M+r7rG1OFw7ig==", "ailk!QAZ");
			logger.info(userName);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

}
