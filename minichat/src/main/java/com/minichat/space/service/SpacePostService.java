package com.minichat.space.service;

import com.minichat.space.dto.PublishSpacePostDTO;
import com.minichat.space.vo.SpacePostVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SpacePostService {

    void publish(PublishSpacePostDTO publishSpacePostDTO);

    String uploadImage(MultipartFile file);

    List<SpacePostVO> list(Long userId, Long friendId);

    void delete(Long postId);

    void recover(Long postId);

    List<SpacePostVO> deletedList(Long userId);

    void changeLikeStatus(Long postId);
}
