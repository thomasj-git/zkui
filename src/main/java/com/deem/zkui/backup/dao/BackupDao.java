package com.deem.zkui.backup.dao;

import com.deem.zkui.backup.object.BackData;

import java.util.List;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
public interface BackupDao {

	void createBackTable();

	void saveBackData(BackData data);

	BackData queryBackData();

	List<BackData> queryAllBackData();

	BackData queryBackData(long backDate);

	void clearBackData(long expiry);

	String queryMaxBackDate();

	Integer queryExpiryBackDataSize(long date);

	void deleteBackData(long date);

}
