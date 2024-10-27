package com.enhanceai.platform.repository;


import com.enhanceai.platform.model.UserContent;
import com.enhanceai.platform.model.UserContentKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

public interface UserContentRepository extends CassandraRepository<UserContent, UserContentKey> {
    @Query("SELECT * FROM usercontent WHERE user_name=:userName")
    Iterable<UserContent> findByUserName(String userName);

    @Query("SELECT * FROM usercontent WHERE user_name=:userName AND parent_content_id=:parentContentId")
    Iterable<UserContent> findByUserNameAndParentContentId(String userName, String parentContentId);

}
