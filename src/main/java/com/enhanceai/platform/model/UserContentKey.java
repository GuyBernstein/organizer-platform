package com.enhanceai.platform.model;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.util.Date;
import java.util.Objects;

@PrimaryKeyClass
public class UserContentKey {

    @PrimaryKeyColumn(name = "user_name",ordinal = 0,type = PrimaryKeyType.PARTITIONED)
    private String userName;

    @PrimaryKeyColumn(name = "creation_time", ordinal = 1, type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING)
    private Date creationTime = new Date();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserContentKey that = (UserContentKey) o;
        return Objects.equals(userName, that.userName) && Objects.equals(creationTime, that.creationTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, creationTime);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public static final class UserContentKeyBuilder {
        private String userName;
        private Date creationTime;

        private UserContentKeyBuilder() {
        }

        public static UserContentKeyBuilder anUserContentKey() {
            return new UserContentKeyBuilder();
        }

        public UserContentKeyBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public UserContentKeyBuilder creationTime(Date creationTime) {
            this.creationTime = creationTime;
            return this;
        }

        public UserContentKey build() {
            UserContentKey userContentKey = new UserContentKey();
            userContentKey.setUserName(userName);
            userContentKey.setCreationTime(creationTime);
            return userContentKey;
        }
    }
}
