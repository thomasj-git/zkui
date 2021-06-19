package com.deem.zkui.filter;

import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
public class SessionCheck {
    private final static Logger log = LoggerFactory.getLogger(SessionCheck.class);
    /** 定时调度器 */
    private ScheduledThreadPoolExecutor sch = new ScheduledThreadPoolExecutor(1);
    /** 会话最大空闲时间 */
    private final static long SESSION_MAX_IDLE_TIME = 300000L;
    /** 最近一次请求时间 */
    private volatile long latestTime = 0L;
    /** 会话是否无效 */
    private volatile boolean sessionInValid = false;

    public SessionCheck() {
        sch.scheduleAtFixedRate(() -> {
            long idle = System.currentTimeMillis() - latestTime;
            log.info("定时检测会话是否失效");
            if (latestTime > 0 && idle > SESSION_MAX_IDLE_TIME) {
                log.warn(
                    "已经超过: " + SESSION_MAX_IDLE_TIME + "ms，未进行任何操作, 上次时间: " + latestTime + ", 空闲时间: " + idle + "ms.");
                sessionInValid = true;
                latestTime = 0;
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void check(HttpServletRequest request, HttpServletResponse response) throws IOException,
			InterruptedException {
        if (latestTime == 0) {
            latestTime = System.currentTimeMillis();
            log.info("首次有效请求");
        }
        if (sessionInValid) {
            log.info("会话失效...");
            sessionInValid = false;
            HttpSession session = request.getSession();
            ZooKeeper zk = (ZooKeeper)session.getAttribute("zk");
            if (zk != null) {
				zk.close();
            }
            response.sendRedirect("/cms-ability/zkui/login");
            return;
        }
        latestTime = System.currentTimeMillis();
        log.info("处理请求: {}", request.getRequestURI());
    }
}
