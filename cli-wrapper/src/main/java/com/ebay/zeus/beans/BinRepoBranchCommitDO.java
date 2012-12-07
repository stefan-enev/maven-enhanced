package com.ebay.zeus.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: rgiroti
 * Date: 10/9/12
 * Time: 1:30 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class BinRepoBranchCommitDO {

    private long id;
    private String repoUrl;
    private String branch;
    private String commitId;
    private String binRepoUrl;
    private String binRepoBranch;
    private String binRepoCommitId;

    /*@XmlElement(name = "timestamp", required = true)
    @XmlJavaTypeAdapter(TimestampAdapter.class)
    private Timestamp creationDate;*/

    private long creationDate;

    public BinRepoBranchCommitDO() {}

    public BinRepoBranchCommitDO(String repoUrl, String branch, String commitId, String binRepoUrl,
                                 String binRepoBranch, String binRepoCommitId, long creationDate) {
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.commitId = commitId;
        this.binRepoUrl = binRepoUrl;
        this.binRepoBranch = binRepoBranch;
        this.binRepoCommitId = binRepoCommitId;
        this.creationDate = creationDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getBinRepoUrl() {
        return binRepoUrl;
    }

    public void setBinRepoUrl(String binRepoUrl) {
        this.binRepoUrl = binRepoUrl;
    }

    public String getBinRepoBranch() {
        return binRepoBranch;
    }

    public void setBinRepoBranch(String binRepoBranch) {
        this.binRepoBranch = binRepoBranch;
    }

    public String getBinRepoCommitId() {
        return binRepoCommitId;
    }

    public void setBinRepoCommitId(String binRepoCommitId) {
        this.binRepoCommitId = binRepoCommitId;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public Timestamp getCreationTimestamp() {
        return new Timestamp(creationDate);
    }

    @Override
    public String toString() {
        return "BinRepoBranchCommitDO{" +
                "id=" + id +
                ", repoUrl='" + repoUrl + '\'' +
                ", branch='" + branch + '\'' +
                ", commitId='" + commitId + '\'' +
                ", binRepoUrl='" + binRepoUrl + '\'' +
                ", binRepoBranch='" + binRepoBranch + '\'' +
                ", binRepoCommitId='" + binRepoCommitId + '\'' +
                ", creationDate=" + creationDate +
                '}';
    }
}