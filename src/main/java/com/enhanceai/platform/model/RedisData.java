package com.enhanceai.platform.model;

import com.enhanceai.platform.util.Dates;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.LocalDateTime;

import java.io.Serializable;
import java.util.Date;

public class RedisData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userName;
    private String key;
    private String value;
    private String catagory;
    private Date createdAt = Dates.nowUTC();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("createdAt")
    public LocalDateTime calcCreatedAt() {
        return Dates.atLocalTime(createdAt);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCatagory() {
        return catagory;
    }

    public void setCatagory(String catagory) {
        this.catagory = catagory;
    }

    public static final class RedisDataBuilder {
        private String userName;
        private String key;
        private String value;
        private String catagory;
        private Date createdAt;

        private RedisDataBuilder() {
        }

        public static RedisDataBuilder aRedisData() {
            return new RedisDataBuilder();
        }

        public RedisDataBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public RedisDataBuilder key(String key) {
            this.key = key;
            return this;
        }

        public RedisDataBuilder value(String value) {
            this.value = value;
            return this;
        }

        public RedisDataBuilder catagory(String catagory) {
            this.catagory = catagory;
            return this;
        }

        public RedisDataBuilder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RedisData build() {
            RedisData redisData = new RedisData();
            redisData.setUserName(userName);
            redisData.setKey(key);
            redisData.setValue(value);
            redisData.setCatagory(catagory);
            redisData.setCreatedAt(createdAt);
            return redisData;
        }
    }
}
