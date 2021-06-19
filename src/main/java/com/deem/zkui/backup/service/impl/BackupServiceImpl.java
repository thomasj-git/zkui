package com.deem.zkui.backup.service.impl;

import com.deem.zkui.backup.dao.BackupDao;
import com.deem.zkui.backup.object.BackData;
import com.deem.zkui.backup.service.BackupService;
import com.deem.zkui.utils.ServletUtil;
import com.deem.zkui.utils.ZooKeeperUtil;
import com.deem.zkui.vo.LeafBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
@Slf4j
public class BackupServiceImpl implements BackupService, InitializingBean {

  private ScheduledExecutorService schedule = new ScheduledThreadPoolExecutor(5);
  private BackupDao featureDao;
  private Properties globalProps;
  private ZooKeeper zk;

  public BackupServiceImpl(Properties globalProps, BackupDao featureDao) {
    this.globalProps = globalProps;
    this.featureDao = featureDao;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    zkInit();
  }

  @Override
  public void executeBackup() {
    log.info("开始ZKUI备份操作...");
    long time = System.currentTimeMillis();
    try {
      String zkPath = "/";
      StringBuilder output = new StringBuilder();
      Set<LeafBean> leaves = ZooKeeperUtil.INSTANCE
          .exportTree(zkPath, zk, ZooKeeperUtil.ROLE_ADMIN);
      log.info("查询需要备份的ZKUI配置项数量: {}， 耗时: {} ms", leaves.size(),
          (System.currentTimeMillis() - time));
      for (LeafBean leaf : leaves) {
        output.append(leaf.getPath()).append('=').append(leaf.getName()).append('=')
            .append(ServletUtil.INSTANCE.externalizeNodeValue(leaf.getValue())).append('\n');
      }
      BackData data = new BackData(System.currentTimeMillis() / 1000L);
      data.format();
      data.writeFile(output.toString());
      featureDao.saveBackData(data);
      log.info("备份ZKUI配置到数据库成功, 备份时间: [{}], 耗时: {} ms", data.getFormatBackDate(), (System
          .currentTimeMillis() - time));
    } catch (Exception e) {
      log.error("备份ZKUI数据异常", e);
    }
  }

  @Override
  public void taskBackup() {
    boolean enableBackup = Boolean.valueOf(globalProps.getProperty("backup.enable", "false"));
    long cycleTime = Long.valueOf(globalProps.getProperty("backup.cycle.time", "1"));
    String cycleUnit = globalProps.getProperty("backup.cycle.unit", "MINUTES");
    log.info("是否自动备份: {}, 备份周期: {}, 备份周期(单位): {}", (enableBackup ? "是" : "否"), cycleTime,
        cycleUnit);
    if (enableBackup) {
      schedule.scheduleAtFixedRate(() -> {
        executeBackup();
        dataExpiry();
      }, cycleTime, cycleTime, TimeUnit.valueOf(cycleUnit));
    }
  }

  @Override
  public void resumeData() {
    resumeData(null);
  }

  @Override
  public void resumeData(String date) {
    if (date == null) {
      date = featureDao.queryMaxBackDate();
    }
    if (date == null) {
      throw new RuntimeException("无备份数据可恢复");
    }
    try {
      long time = DateUtils.parseDate(date, new String[]{"yyyy-MM-dd HH:mm:ss"}).getTime() / 1000L;
      BackData data = featureDao.queryBackData(time);
      log.info("开始使用[{}]时间的备份数据,恢复ZKUI的数据, 恢复配置数: {}", date, data.getLines());
      long mills = System.currentTimeMillis();
      ZooKeeperUtil.INSTANCE.importData(data.getLineLst(), true, zk,globalProps);
      data.cleanFile();
      log.info("使用[{}]时间的备份数据,恢复ZKUI的数据成功, 恢复配置数: {}, 耗时: {} 秒", date, data.getLines(), (System
          .currentTimeMillis() - mills) / 1000L);
    } catch (Exception e) {
      log.error("恢复配置中心数据", e);
    }
  }

  @Override
  public void dataExpiry() {
    try {
      long time = Long.valueOf(globalProps.getProperty("backup.expiry.time", "7"));
      String unit = globalProps.getProperty("backup.expiry.unit", "DAYS");
      long expiry = System.currentTimeMillis() / 1000L - TimeUnit.SECONDS
          .convert(time, TimeUnit.valueOf(unit));
      Integer len = featureDao.queryExpiryBackDataSize(expiry);
      if (len > 0) {
        featureDao.clearBackData(expiry);
      }
      log.info("清理[{}]时间点前的ZKUI备份记录[{}]条.",
          DateFormatUtils.format(expiry * 1000L, "yyyy-MM-dd HH:mm:ss"), len);
    } catch (Exception e) {
      log.error("清理过期ZKUI备份数据异常", e);
    }
  }

  @Override
  public void deleteData(String date) {
    try {
      long time = DateUtils.parseDate(date, new String[]{"yyyy-MM-dd HH:mm:ss"}).getTime() / 1000L;
      featureDao.deleteBackData(time);
      log.info("删除[{}]时间点ZKUI备份数据", date);
    } catch (Exception e) {
      log.error(String.format("删除[{}]时间点ZKUI备份数据", date), e);
    }
  }

  private void zkInit() throws IOException, InterruptedException {
    Integer zkSessionTimeout = Integer.parseInt(globalProps.getProperty("zkSessionTimeout"));
    String zkServer = globalProps.getProperty("zkServer");
    String[] zkServerLst = zkServer.split(",");
    //Converting seconds to ms.
    zkSessionTimeout = zkSessionTimeout * 1000;
    ZooKeeperUtil.INSTANCE.setDefaultAcl(globalProps.getProperty("defaultAcl"));
    zk = ZooKeeperUtil.INSTANCE.createZKConnection(zkServerLst[0], zkSessionTimeout,globalProps);
  }

}
