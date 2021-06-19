package com.deem.zkui.backup.service;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
public interface BackupService {

  /**
   * 执行备份操作
   */
  void executeBackup();

  /**
   * 启动备份任务,进行定时备份
   */
  void taskBackup();

  /**
   * 恢复最新的备份数据
   */
  void resumeData();

  /**
   * 恢复指定日期的备份数据
   */
  void resumeData(String date);

  /**
   * 清理过期备份数据
   */
  void dataExpiry();

  /**
   * 删除指定日期的备份数据
   */
  void deleteData(String date);

}
