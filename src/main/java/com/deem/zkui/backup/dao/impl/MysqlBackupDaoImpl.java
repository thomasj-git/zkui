package com.deem.zkui.backup.dao.impl;

import com.deem.zkui.backup.dao.AbstractBackupDao;
import com.deem.zkui.backup.object.BackData;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
@Slf4j
public class MysqlBackupDaoImpl extends AbstractBackupDao {

  public MysqlBackupDaoImpl(Properties globalProps) {
    super(globalProps);
  }

  @Override
  public List<BackData> queryAllBackData() {
    String sql =
        "select back_date, back_data from TMS_ZKUI_BACKUP order by back_date desc limit 10";
    log.info("查询所有ZKUI备份的数据: {}", sql);
    List<BackData> datas = getJt().query(sql, (rs, i) -> {
      BackData data = new BackData(rs.getLong(1), rs.getBlob(2));
      data.blob2File();
      data.cleanFile();
      data.format();
      return data;
    });
    return datas;
  }

  @Override
  protected String getCreateBackTableSQL() {
    StringBuilder sb = new StringBuilder();
    sb.append("create table TMS_ZKUI_BACKUP(");
    sb.append("back_date numeric(15) primary key,");
    sb.append("back_data blob not null");
    sb.append(")");
    return sb.toString();
  }
}
