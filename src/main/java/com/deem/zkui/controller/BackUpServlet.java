package com.deem.zkui.controller;

import com.deem.zkui.backup.dao.BackupDao;
import com.deem.zkui.backup.object.BackData;
import com.deem.zkui.backup.service.BackupService;
import com.deem.zkui.utils.BeanFactory;
import com.deem.zkui.utils.ServletUtil;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
@WebServlet(urlPatterns = {"/cms-ability/zkui/backup"})
@Slf4j
public class BackUpServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException,
      IOException {
    log.debug("BackUp Get Action!");
    String operation = request.getParameter("operation");
    String backDate = request.getParameter("backDate");
    try {
      if ("delete".equals(operation)) {
        BeanFactory.getBean(BackupService.class).deleteData(backDate);
        renderBackUpTemplate(request, response);
      } else if ("resume".equals(operation)) {
        BeanFactory.getBean(BackupService.class).resumeData(backDate);
        response.sendRedirect("/cms-ability/zkui/home");
      } else {
        renderBackUpTemplate(request, response);
      }
    } catch (TemplateException ex) {
      log.error(Arrays.toString(ex.getStackTrace()));
      ServletUtil.INSTANCE.renderError(request, response, ex.getMessage());
    }
  }

  private void renderBackUpTemplate(HttpServletRequest request, HttpServletResponse response)
      throws IOException,
      TemplateException {
    Map<String, Object> templateParam = new HashMap<>();
    List<BackData> backList = BeanFactory.getBean(BackupDao.class).queryAllBackData();
    templateParam.put("backList", backList);
    templateParam.put("backNode", "");
    ServletUtil.INSTANCE.renderHtml(request, response, templateParam, "backup.ftl.html");
  }

}
