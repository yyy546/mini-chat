package com.minichat.es;

import com.minichat.chat.entity.EsChatMessage;
import com.minichat.chat.service.ChatSearchService;
import com.minichat.chat.service.impl.ChatSearchServiceImpl;
import com.minichat.common.core.constants.SessionConstants;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EsDataSyncTest {

    @Autowired
    private ChatSearchService chatSearchService;

    @Autowired
    private ChatSearchServiceImpl chatSearchServiceImpl;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void syncAllData() {
        System.out.println("====== 开始重置索引并全量同步 ======");

        // 0. 删除旧索引并重新初始化
        try {
            boolean exists = esClient.indices().exists(req -> req.index("chat_message")).value();
            if (exists) {
                esClient.indices().delete(d -> d.index("chat_message"));
                System.out.println("旧索引已删除");
            }
            // 调用 initIndex 重新创建（包含最新的 mapping）
            // 稍作等待确保 ES 处理完删除操作
            Thread.sleep(1000);
            
            chatSearchServiceImpl.initIndex();
            System.out.println("新索引已创建");
            
            // 再次等待确保索引创建完成
            Thread.sleep(1000);
            
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 1. 同步私聊消息 (过滤掉已撤回的消息 is_recall = 1)
        String privateSql = "SELECT pm.*, u.nickname as sender_nickname FROM private_message pm LEFT JOIN user u ON pm.sender_id = u.id WHERE pm.is_recall = 0";
        List<EsChatMessage> privateMessages = jdbcTemplate.query(privateSql, (rs, rowNum) -> {
            return EsChatMessage.builder()
                    .dbId(rs.getLong("id"))
                    .type(SessionConstants.SESSION_TYPE_PRIVATE) // 0: 私聊
                    .senderId(rs.getLong("sender_id"))
                    .senderNickName(rs.getString("sender_nickname")) // 设置发送者昵称
                    .targetId(rs.getLong("receiver_id")) // 私聊的目标是接收者
                    .content(rs.getString("content"))
                    .messageType(rs.getInt("message_type"))
                    .fileName(rs.getString("file_name"))
                    .fileUrl(rs.getString("file_url"))
                    .sendTime(rs.getObject("send_time", LocalDateTime.class))
                    .build();
        });

        System.out.println("检测到私聊消息 " + privateMessages.size() + " 条，开始同步...");
        for (EsChatMessage msg : privateMessages) {
            chatSearchService.saveChatMessage(msg);
        }

        // 2. 同步群聊消息 (过滤掉已撤回的消息 is_recall = 1)
        String groupSql = "SELECT gm.*, u.nickname as sender_nickname FROM group_message gm LEFT JOIN user u ON gm.sender_id = u.id WHERE gm.is_recall = 0";
        List<EsChatMessage> groupMessages = jdbcTemplate.query(groupSql, (rs, rowNum) -> {
            return EsChatMessage.builder()
                    .dbId(rs.getLong("id"))
                    .type(SessionConstants.SESSION_TYPE_GROUP) // 1: 群聊
                    .senderId(rs.getLong("sender_id"))
                    .senderNickName(rs.getString("sender_nickname")) // 设置发送者昵称
                    .targetId(rs.getLong("group_id")) // 群聊的目标是群ID
                    .content(rs.getString("content"))
                    .messageType(rs.getInt("message_type"))
                    .fileName(rs.getString("file_name"))
                    .fileUrl(rs.getString("file_url"))
                    .sendTime(rs.getObject("send_time", LocalDateTime.class))
                    .build();
        });

        System.out.println("检测到群聊消息 " + groupMessages.size() + " 条，开始同步...");
        for (EsChatMessage msg : groupMessages) {
            chatSearchService.saveChatMessage(msg);
        }

        System.out.println("====== 全量同步完成 ======");
    }
}
