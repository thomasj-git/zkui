package com.deem.zkui.backup.object;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
@Slf4j
@Data
public class BackData {

  private String formatBackDate;
  private Long backDate;
  private Blob blob;
  private File backFile;
  private int lines;
  private List<String> lineLst;

  public BackData(Long backDate, Blob blob) {
    this.backDate = backDate;
    this.blob = blob;
    createBackFile();
  }

  public BackData(Long backDate) {
    this.backDate = backDate;
  }

  public void blob2File() {
    Blob _blob = this.blob;
    InputStream in = null;
    FileOutputStream out = null;
    if (_blob != null) {
      try {
        in = _blob.getBinaryStream();
        out = new FileOutputStream(this.backFile);
        IOUtils.copy(in, out);
        lineLst = FileUtils.readLines(this.backFile, "utf-8");
        lines = lineLst.size();
      } catch (Exception ex) {
        log.error("blob转文件异常", ex);
      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
      }
    }
  }

  public void writeFile(String bkConfig) {
    if (this.backFile == null) {
      createBackFile();
    }
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(this.backFile);
      IOUtils.write(bkConfig, out, "utf-8");
    } catch (IOException e) {
      log.error("从ZKUI读取配置写临时文件异常", e);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  public void file2Blob(PreparedStatement ps, int index) {
    if (backFile != null) {
      InputStream in = null;
      try {
        in = FileUtils.openInputStream(backFile);
        ps.setBinaryStream(index, in, in.available());
      } catch (Exception e) {
        log.error("file转blob对象异常", e);
      }
    }
  }

  public void format() {
    if (backDate != null) {
      this.formatBackDate = DateFormatUtils
          .format(backDate.longValue() * 1000L, "yyyy-MM-dd HH:mm:ss");
    }
  }

  public void cleanFile() {
    if (this.backFile != null) {
      try {
        FileUtils.forceDelete(this.backFile);
        log.info("Clean file {}.", this.backFile.getAbsolutePath());
      } catch (IOException e) {
        log.error(String.format("删除文件[%s]", this.backFile), e);
      }
    }
  }

  private void createBackFile() {
    File backDir = new File(System.getProperty("user.dir") + "/backup");
    if (!backDir.exists()) {
      backDir.mkdir();
    }
    String filePath = System.getProperty("user.dir") + "/backup/config.txt." + DateFormatUtils
        .format(backDate * 1000L, "yyyyMMddHHmmss");
    this.backFile = new File(filePath);
    log.info("创建ZKUI备份文件: {}", filePath);
  }

  public String toWriteString() {
    return String.format("备份时间: %s, 备份文件: %s", backDate, backFile);
  }

  public String toReadString() {
    return String.format("备份时间: %s, 备件数据blob: %s, 创建临时文件: %s", backDate, blob, backFile);
  }

}
