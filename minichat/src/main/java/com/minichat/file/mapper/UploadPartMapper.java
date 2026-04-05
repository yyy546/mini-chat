package com.minichat.file.mapper;

import com.minichat.file.entity.UploadPart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UploadPartMapper {

    @Insert("insert into upload_part (upload_id, chunk_index, chunk_size, etag, status, retry_times, create_time, update_time) " +
            "values (#{uploadId}, #{chunkIndex}, #{chunkSize}, #{etag}, #{status}, #{retryTimes}, now(), now()) " +
            "on duplicate key update chunk_size = values(chunk_size), etag = values(etag), status = values(status), retry_times = values(retry_times), update_time = now()")
    void upsert(UploadPart uploadPart);

    @Select("select * from upload_part where upload_id = #{uploadId}")
    List<UploadPart> selectByUploadId(@Param("uploadId") String uploadId);

    @Delete("delete from upload_part where upload_id = #{uploadId}")
    int deleteByUploadId(@Param("uploadId") String uploadId);
}
