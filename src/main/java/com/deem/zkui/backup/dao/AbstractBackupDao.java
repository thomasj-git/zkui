package com.deem.zkui.backup.dao;

import com.deem.zkui.backup.object.BackData;
import java.sql.Types;
import java.util.List;
import java.util.Properties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
@Slf4j
public abstract class AbstractBackupDao implements BackupDao {

  private Properties globalProps;
  @Getter
  private JdbcTemplate jt;

  public AbstractBackupDao(Properties globalProps) {
    this.globalProps = globalProps;
    jdbcInit(globalProps);
  }

  @Override
  public void createBackTable() {
    try {
      String sql = getCreateBackTableSQL();
      jt.execute(sql);
      log.info("创建ZKUI备份表: {}", sql);
    } catch (DataAccessException e) {
      log.error("ZKUI备份数据已经存在，无需重复创建");
    }
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
  public void saveBackData(BackData data) {
    String sql = "insert into TMS_ZKUI_BACKUP(back_date,back_data) values(?,?)";
    try {
      jt.update(sql, ps -> {
        ps.setLong(1, data.getBackDate());
        data.file2Blob(ps, 2);
        data.cleanFile();
      });
      log.info("保存ZKUI备份数据成功: {}, {}", sql, data.toWriteString());
    } catch (DataAccessException e) {
      log.info(String.format("保存ZKUI备份数据异常: %s, %s", sql, data.toWriteString()), e);
    }
  }

  @Override
  public BackData queryBackData() {
    String sql =
        "select back_date as \"backDate\", back_data as \"backData\" from TMS_ZKUI_BACKUP where back_date=(select max(back_date) from TMS_ZKUI_BACKUP)";
    try {
      List<BackData> datas = jt.query(sql, (rs, i) -> {
        BackData data = new BackData(rs.getLong(1), rs.getBlob(2));
        data.blob2File();
        data.cleanFile();
        data.format();
        return data;
      });
      BackData data = datas.isEmpty() ? null : datas.get(0);
      log.info("读取ZKUI备份数据成功: {}, {}", sql, data);
      return data;
    } catch (DataAccessException e) {
      log.info(String.format("读取ZKUI备份数据异常: %s", sql), e);
      return null;
    }
  }

  @Override
  public BackData queryBackData(long backDate) {
    String formatBackDate = DateFormatUtils.format(backDate * 1000L, "yyyy-MM-dd HH:mm:ss");
    String sql = "select back_date, back_data from TMS_ZKUI_BACKUP where back_date=?";
    try {
      List<BackData> datas = jt.query(sql, ps -> {
        ps.setLong(1, backDate);
      }, (rs, i) -> {
        BackData data = new BackData(rs.getLong(1), rs.getBlob(2));
        data.blob2File();
        return data;
      });
      BackData data = datas.isEmpty() ? null : datas.get(0);
      log.info("读取[{}]时间点备份ZKUI备份数据成功: {}, 读取到配置项: {} 个", formatBackDate, sql, data.getLines());
      return data;
    } catch (DataAccessException e) {
      log.info(String.format("读取[{}]时间点ZKUI备份数据异常: %s", formatBackDate, sql), e);
      return null;
    }
  }

  @Override
  public Integer queryExpiryBackDataSize(long expiry) {
    String sql = "select count(1) from TMS_ZKUI_BACKUP where back_date < ?";
    String formatExpiry = DateFormatUtils.format(expiry * 1000L, "yyyy-MM-dd HH:mm:ss");
    Integer len = null;
    try {
      len = jt.queryForObject(sql, new Object[]{expiry}, new int[]{Types.NUMERIC}, Integer.class);
      log.info("查询到ZKUI[{}]时间点前的备份数据记录数: {}", formatExpiry, len);
    } catch (DataAccessException e) {
      log.info(String.format("查询到ZKUI[%s]时间点前的备份数据记录数异常", formatExpiry), e);
    }
    return len;
  }

  @Override
  public void clearBackData(long expiry) {
    String sql = "delete from TMS_ZKUI_BACKUP where back_date < ?";
    String formatExpiry = DateFormatUtils.format(expiry * 1000L, "yyyy-MM-dd HH:mm:ss");
    try {
      jt.update(sql, ps -> ps.setLong(1, expiry));
      log.info("清理ZKUI[{}]时间点前的备份数据成功", formatExpiry);
    } catch (DataAccessException e) {
      log.info(String.format("清理ZKUI[%s]时间点前的备份数据异常", formatExpiry), e);
    }
  }

  @Override
  public String queryMaxBackDate() {
    String sql = "select max(back_date) from TMS_ZKUI_BACKUP";
    try {
      Long backDate = jt.queryForObject(sql, Long.class);
      String date = (backDate == null ? null
          : DateFormatUtils.format(backDate, "yyyy-MM-dd HH:mm:ss"));
      log.info("查询最大备份时间: {}", date);
      return date;
    } catch (DataAccessException e) {
      log.error("", e);
      return null;
    }
  }

  @Override
  public void deleteBackData(long date) {
    String sql = "delete from TMS_ZKUI_BACKUP where back_date=?";
    try {
      jt.update(sql, ps -> {
        ps.setLong(1, date);
      });
      log.info("删除时间点[{}]的ZKUI备份数据: {}", date, sql);
    } catch (DataAccessException e) {
      log.info(String.format("删除时间点[%s]的ZKUI备份数据异常: %s", date, sql), e);
    }
  }

  protected abstract String getCreateBackTableSQL();

  private void jdbcInit(Properties globalProps) {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource
        .setDriverClassName(globalProps.getProperty("spring.datasource.dbcp2.driver-class-name"));
    dataSource.setUrl(globalProps.getProperty("spring.datasource.dbcp2.url"));
    dataSource.setUsername(globalProps.getProperty("spring.datasource.dbcp2.username"));
    dataSource.setPassword(globalProps.getProperty("spring.datasource.dbcp2.password"));
    dataSource.setMaxTotal(
        NumberUtils.toInt(globalProps.getProperty("spring.datasource.dbcp2.max-total"), 5));
    dataSource.setMaxWaitMillis(NumberUtils
        .toLong(globalProps.getProperty("spring.datasource.dbcp2.max-wait-millis"), 300000));
    dataSource
        .setValidationQuery(globalProps.getProperty("spring.datasource.dbcp2.validation-query"));
    this.jt = new JdbcTemplate(dataSource);
  }

}
