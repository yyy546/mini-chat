package com.minichat.file.mapper;

import com.minichat.file.entity.UploadTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UploadTaskMapper {

    @Insert("insert into upload_task (upload_id, user_id, biz_type, biz_id, file_name, file_size, file_hash, chunk_size, total_chunks, uploaded_chunks, oss_upload_id, oss_object_key, status, expire_time, create_time, update_time) " +
            "values (#{uploadId}, #{userId}, #{bizType}, #{bizId}, #{fileName}, #{fileSize}, #{fileHash}, #{chunkSize}, #{totalChunks}, #{uploadedChunks}, #{ossUploadId}, #{ossObjectKey}, #{status}, #{expireTime}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UploadTask uploadTask);

    @Select("select * from upload_task where upload_id = #{uploadId}")
    UploadTask selectByUploadId(@Param("uploadId") String uploadId);

    @Update("update upload_task set uploaded_chunks = uploaded_chunks + 1, update_time = now() where upload_id = #{uploadId}")
    int increaseUploadedChunks(@Param("uploadId") String uploadId);

    @Update("update upload_task set status = #{status}, oss_object_key = #{ossObjectKey}, file_url = #{fileUrl}, update_time = now() where upload_id = #{uploadId}")
    int updateStatusAndResult(@Param("uploadId") String uploadId, @Param("status") Integer status, @Param("ossObjectKey") String ossObjectKey, @Param("fileUrl") String fileUrl);

    @Update("update upload_task set status = #{status}, error_msg = #{errorMsg}, update_time = now() where upload_id = #{uploadId}")
    int updateStatusAndError(@Param("uploadId") String uploadId, @Param("status") Integer status, @Param("errorMsg") String errorMsg);

    @Select("select * from upload_task where status <> 1 and expire_time < #{now} limit #{limit}")
    List<UploadTask> selectExpiredUnfinished(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Delete("delete from upload_task where id = #{id}")
    int deleteById(@Param("id") Long id);
}
